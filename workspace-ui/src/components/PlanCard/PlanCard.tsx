import { ArrowDownIcon, ArrowUpIcon, CheckCheckIcon, CheckIcon, CircleAlertIcon, type LucideIcon, MinusIcon, PencilIcon, PlusIcon, Undo2Icon, XIcon } from 'lucide-react';
import { type KeyboardEvent, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { RefToken } from '@/components/MessageParts/RefToken';
import { Button } from '@/components/ui/button';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { planMessage, type PlanStepInput } from '@/helper/planMessage/planMessage';
import { isStalePlanCode, reloadPlans } from '@/helper/planReload/planReload';
import { useSendMessage } from '@/hooks/useGenaixQueries';
import { GenaixApiError } from '@/services/apiService/apiService';
import type { Plan, PlanItem, Problem } from '@/services/apiService/genaix/types';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';

import styles from './PlanCard.module.css';

type Status = PlanItem['status'];
type Change = NonNullable<PlanItem['change']>;

// The marker icon per status; pending and running show the item's number instead.
const ICONS: Partial<Record<Status, LucideIcon>> = { done: CheckIcon, skipped: MinusIcon, failed: XIcon, removed: MinusIcon };
const STATUSES = new Set<string>(['pending', 'running', 'done', 'skipped', 'failed', 'removed']);

// The changes a revision marks on an item (rc.5); `unchanged` and `removed` get no marker (a removed
// item is struck through).
const MARKED_CHANGES: ReadonlySet<Change> = new Set<Change>(['added', 'changed']);

// A status outside the contract's enum shows as pending (client rule: tolerate unknown enum values).
function statusOf(item: PlanItem): Status {
    return STATUSES.has(item.status) ? item.status : 'pending';
}

/** A step while the user adjusts the plan; `key` keeps a row's field while rows move. */
interface Row extends PlanStepInput {
    key: string;
    /** Carried out in an earlier version (rc.5): sent as it is, never edited, removed or moved. */
    isDone?: boolean;
    /** Marked `removed` by the revision: struck through and not sent, unless the user restores it. */
    isRemoved?: boolean;
    /** The ids of the items this one needs, which stay before it (`PlanItem.depends_on`). */
    dependsOn?: string[];
    /** What the revision did to the item (`PlanItem.change`). */
    change?: Change;
}

function rowsOf(items: PlanItem[]): Row[] {
    return items.map(({ id, label, target, status, depends_on: dependsOn, change }) => ({
        key: id,
        id,
        label,
        target,
        ...(status === 'done' && { isDone: true }),
        ...(status === 'removed' && { isRemoved: true }),
        ...(dependsOn?.length && { dependsOn }),
        ...(change && { change }),
    }));
}

// The rows as they are kept: trimmed, empty ones dropped; a done one exactly as it is.
function normalized(rows: Row[]): Row[] {
    return rows.map((row) => (row.isDone ? row : { ...row, label: row.label.trim() })).filter(({ label }) => label !== '');
}

// The steps as they would be sent: removed ones left out (sending a removed item's id would bring it back).
function stepsOf(rows: Row[]): Row[] {
    return normalized(rows).filter(({ isRemoved }) => !isRemoved);
}

// Whether the steps that would be sent differ from those the plan sends as proposed (every item but the
// removed ones): an item, its wording, order or target.
function isChanged(steps: PlanStepInput[], items: PlanItem[]): boolean {
    const signature = (step: PlanStepInput) => `${step.id ?? ''}\u0000${step.label}\u0000${step.target ? JSON.stringify(step.target) : ''}`;

    return steps.map(signature).join('\u0001') !== items.filter(({ status }) => status !== 'removed').map(signature).join('\u0001');
}

// Whether every kept row comes after the kept rows it depends on (rc.5: `422 dependency_order` otherwise).
function isInDependencyOrder(rows: Row[]): boolean {
    const kept = new Set(rows.flatMap(({ id, isRemoved }) => (id && !isRemoved ? [id] : [])));
    const before = new Set<string>();

    for (const row of rows) {
        if (row.isRemoved) {
            continue;
        }

        if ((row.dependsOn ?? []).some((id) => kept.has(id) && !before.has(id))) {
            return false;
        }

        if (row.id) {
            before.add(row.id);
        }
    }

    return true;
}

// Whether another kept row depends on `row`, so it cannot be dropped (rc.5: `422 dependency_order`).
function isNeeded(rows: Row[], row: Row): boolean {
    return row.id !== undefined && rows.some((other) => other !== row && !other.isRemoved && other.dependsOn?.includes(row.id!));
}

// The rows with the one at `index` moved by `by`.
function moved(rows: Row[], index: number, by: -1 | 1): Row[] {
    const next = [...rows];
    const [row] = next.splice(index, 1);

    next.splice(index + by, 0, row!);

    return next;
}

// An item of a sent `plan` part the server refused (`422 validation_failed`), by its pointer
// `/parts/<i>/plan/items/<j>` (rc.5: a done item changed, `dependency_order`).
const ITEM_POINTER = /^\/parts\/\d+\/plan\/items\/(\d+)(?:\/|$)/;

// The rows the problem points at, by row key, each with the i18n key of what is wrong with it.
function invalidRowsOf(problem: Problem | undefined, sent: Row[]): Record<string, string> {
    const invalid: Record<string, string> = {};

    for (const { pointer, code } of problem?.errors ?? []) {
        const index = ITEM_POINTER.exec(pointer)?.[1];
        const row = index === undefined ? undefined : sent[Number(index)];

        if (row) {
            invalid[row.key] = code === 'dependency_order' ? 'parts.plan.invalid.dependency_order' : 'parts.plan.invalid.other';
        }
    }

    return invalid;
}

interface PlanEditorProps {
    rows: Row[];
    onChange: (update: (rows: Row[]) => Row[]) => void;
    onDone: () => void;
    onCancel: () => void;
    /** Rows the server refused, by key: the i18n key of what is wrong. */
    invalid?: Record<string, string>;
}

/**
 * Adjusting the plan right in the card (draft `editPlan`): each step a field with move and remove
 * buttons, "Add a step" below. Enter adds a step after the current one, Backspace in an empty field
 * removes it, Esc cancels, Ctrl/Cmd+Enter is Done.
 *
 * A step finished in an earlier version is read-only: no field, no remove, and it does not move, nor
 * does a step past it (rc.5: changing it is `422`); nor does a step past a removed one, which is not sent. A step the revision removed stays struck through
 * with "Restore", which keeps it again. A move that puts a step before one it depends on, or removing
 * one another kept step depends on, is not offered (`422 dependency_order`).
 */
function PlanEditor({ rows, onChange: setRows, onDone, onCancel, invalid }: PlanEditorProps) {
    const { t } = useTranslation();
    // The row whose field gets the focus after the next render: the first editable at the start, then
    // the one added, moved or before the one removed.
    const focusKey = useRef<string | undefined>(rows.find((row) => !row.isDone && !row.isRemoved)?.key);
    const fields = useRef(new Map<string, HTMLInputElement>());
    const keptCount = rows.filter(({ isRemoved }) => !isRemoved).length;

    useEffect(() => {
        if (focusKey.current) {
            fields.current.get(focusKey.current)?.focus();
            focusKey.current = undefined;
        }
    });

    function update(index: number, label: string) {
        setRows((current) => current.map((row, i) => (i === index ? { ...row, label } : row)));
    }

    function add(after: number) {
        const row = { key: crypto.randomUUID(), label: '' };

        setRows((current) => [...current.slice(0, after + 1), row, ...current.slice(after + 1)]);
        focusKey.current = row.key;
    }

    function remove(index: number) {
        setRows((current) => current.filter((_, i) => i !== index));
        focusKey.current = rows[index - 1]?.key ?? rows[index + 1]?.key;
    }

    function restore(index: number) {
        setRows((current) => current.map((row, i) => (i === index ? { ...row, isRemoved: false } : row)));
    }

    function canMove(index: number, by: -1 | 1): boolean {
        const row = rows[index];
        const other = rows[index + by];

        return Boolean(row && other) && !row!.isDone && !row!.isRemoved && !other!.isDone && !other!.isRemoved && isInDependencyOrder(moved(rows, index, by));
    }

    function move(index: number, by: -1 | 1) {
        setRows((current) => moved(current, index, by));
        focusKey.current = rows[index]?.key;
    }

    function canRemove(row: Row): boolean {
        return keptCount > 1 && !isNeeded(rows, row);
    }

    function handleKeyDown(event: KeyboardEvent<HTMLInputElement>, index: number) {
        const row = rows[index];

        if (event.key === 'Escape') {
            event.preventDefault();
            onCancel();
        } else if (event.key === 'Enter' && (event.metaKey || event.ctrlKey)) {
            event.preventDefault();
            onDone();
        } else if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            add(index);
        } else if (event.key === 'Backspace' && row?.label === '' && canRemove(row)) {
            event.preventDefault();
            remove(index);
        }
    }

    return (
        <>
            <ol className={styles.items}>
                {rows.map((row, index) => {
                    const invalidKey = invalid?.[row.key];

                    if (row.isDone || row.isRemoved) {
                        const Icon = row.isDone ? CheckIcon : MinusIcon;

                        return (
                            <li key={row.key} className={[styles.item, styles.editing, styles[row.isDone ? 'done' : 'removed']].join(' ')}>
                                <span className={styles.marker} aria-hidden><Icon size={11} strokeWidth={2.4} /></span>
                                <span className={styles.text}>
                                    <span className={styles.label}>{row.label}</span>
                                    {row.target && <span><RefToken value={row.target} /></span>}
                                </span>
                                {row.isDone
                                    ? <span className={styles.status}>{t('chat.steps.status.done')}</span>
                                    : (
                                        <Button variant="ghost" size="sm" onClick={() => restore(index)}>
                                            <Undo2Icon size={14} />
                                            {t('parts.plan.restore')}
                                        </Button>
                                    )}
                            </li>
                        );
                    }

                    return (
                        <li key={row.key} className={[styles.item, styles.editing, invalidKey && styles.invalid].filter(Boolean).join(' ')}>
                            <span className={styles.marker} aria-hidden>{index + 1}</span>
                            <span className={styles.text}>
                                <input
                                    ref={(element) => {
                                        if (element) {
                                            fields.current.set(row.key, element);
                                        } else {
                                            fields.current.delete(row.key);
                                        }
                                    }}
                                    className={styles.input}
                                    aria-label={t('parts.plan.step', { number: index + 1 })}
                                    aria-invalid={invalidKey ? true : undefined}
                                    placeholder={t('parts.plan.placeholder')}
                                    value={row.label}
                                    maxLength={500}
                                    onChange={(event) => update(index, event.target.value)}
                                    onKeyDown={(event) => handleKeyDown(event, index)}
                                />
                                {row.target && <span><RefToken value={row.target} /></span>}
                                {invalidKey && <span className={styles.problem}>{t(invalidKey)}</span>}
                            </span>
                            <span className={styles.rowActions}>
                                <Button variant="ghost" size="icon" aria-label={t('parts.plan.moveUp')} disabled={!canMove(index, -1)} onClick={() => move(index, -1)}>
                                    <ArrowUpIcon size={14} />
                                </Button>
                                <Button variant="ghost" size="icon" aria-label={t('parts.plan.moveDown')} disabled={!canMove(index, 1)} onClick={() => move(index, 1)}>
                                    <ArrowDownIcon size={14} />
                                </Button>
                                <Button variant="ghost" size="icon" aria-label={t('parts.plan.remove')} disabled={!canRemove(row)} onClick={() => remove(index)}>
                                    <XIcon size={14} />
                                </Button>
                            </span>
                        </li>
                    );
                })}
            </ol>
            <div className={styles.add}>
                <Button variant="ghost" size="sm" onClick={() => add(rows.length - 1)}>
                    <PlusIcon size={14} />
                    {t('parts.plan.add')}
                </Button>
            </div>
        </>
    );
}

interface PlanCardProps {
    plan: Plan;
    sessionId: string;
    /** No run works on the session: the plan can be adjusted and accepted. */
    canAnswer: boolean;
}

/**
 * The agent's work plan (`plan.updated`), shown in the plan bar above the composer while it is open
 * (`PlanBar`; design.md §12 "Plan-Schritte"; draft `partHTML` plan, `editPlan`): the summary, then each
 * item with its number or state, its target and its status word. While the plan is `proposed` and no
 * run works, Adjust edits the steps in place and Accept plan sends the plan to the agent (`planMessage`):
 * as proposed, or as adjusted. A later version says why it changed (`reason`). Refused because the plan
 * moved on meanwhile (`409 plan_outdated`, `plan_closed`, `plan_not_proposed`), the plans are read again
 * (`reloadPlans`); refused for an item (`422` pointing at `/parts/<i>/plan/items/<j>`), that row is
 * highlighted with what is wrong instead of an error notification.
 *
 * A plan is living (rc.5): a revision keeps its items while it is drafted (shown muted), then proposes a
 * new version in which each item says what it changed (`PlanItem.change`). A proposed revision marks the
 * items it added or changed; a plan never accepted has no marks, every item being `added` there. A
 * removed item is struck through until the version is accepted.
 */
export function PlanCard({ plan, sessionId, canAnswer }: PlanCardProps) {
    const { t } = useTranslation();
    const sendMessage = useSendMessage(sessionId);
    const items = plan.items ?? [];
    const [adjustedRows, setAdjusted] = useState<Row[]>();
    // The steps while the user adjusts them; none while the plan is only shown.
    const [draft, setDraft] = useState<Row[]>();
    const [isAccepted, setIsAccepted] = useState(false);
    // The rows the server refused, by key, until the user adjusts the plan again.
    const [invalid, setInvalid] = useState<Record<string, string>>();
    // Only a proposed plan can be accepted (contract: `409 plan_not_proposed` otherwise).
    const isProposed = plan.status === 'proposed';
    // The user's adjustment counts only while the plan waits for them: once it is accepted, the card
    // shows the plan as GenAIx executes it.
    const adjusted = isProposed ? adjustedRows : undefined;
    const canAct = isProposed && canAnswer && !isAccepted;
    // A proposed revision against the version accepted last (`change` is stated against it, rc.5).
    const showsChanges = plan.accepted_version != null && plan.version > plan.accepted_version;
    const shownInvalid = adjusted ? invalid : undefined;

    const isEditing = Boolean(draft) && canAct;
    const steps = stepsOf(draft ?? []);

    function handleDone() {
        if (steps.length > 0 && draft) {
            setAdjusted(isChanged(steps, items) ? normalized(draft) : undefined);
            setDraft(undefined);
            setInvalid(undefined);
        }
    }

    function accept() {
        const sent = adjusted && stepsOf(adjusted);

        setIsAccepted(true);
        sendMessage.mutateAsync(planMessage(plan, sent)).catch((error: unknown) => {
            setIsAccepted(false);

            if (error instanceof GenaixApiError && isStalePlanCode(error.genaixCode)) {
                reloadPlans(sessionId).catch((reloadError: unknown) => console.warn('Reading the plans again failed', reloadError));
            }

            const refused = error instanceof GenaixApiError && error.status === 422 && sent ? invalidRowsOf(error.problem, sent) : {};

            if (Object.keys(refused).length > 0) {
                setInvalid(refused);

                return;
            }

            useErrorNotificationStore.getState().addError({ messageKey: 'chat.sendFailed', detailKey: errorMessageKey(error) });
        });
    }

    let footer;

    if (isEditing) {
        footer = (
            <>
                <span className={styles.caption}>{t('parts.plan.keys')}</span>
                <Button variant="secondary" size="sm" onClick={() => setDraft(undefined)}>{t('parts.plan.cancel')}</Button>
                <Button variant="primary" size="sm" disabled={steps.length === 0} onClick={handleDone}>
                    <CheckIcon size={14} />
                    {t('parts.plan.done')}
                </Button>
            </>
        );
    } else if (canAct) {
        let caption;

        if (shownInvalid) {
            caption = t('parts.plan.invalid.caption');
        } else if (adjusted) {
            caption = t('parts.plan.adjustedByYou');
        }

        footer = (
            <>
                <span className={styles.caption}>{caption}</span>
                <Button variant="secondary" size="sm" onClick={() => setDraft(adjusted ?? rowsOf(items))}>
                    <PencilIcon size={14} />
                    {t('parts.plan.adjust')}
                </Button>
                <Button variant="primary" size="sm" onClick={accept}>
                    <CheckCheckIcon size={14} />
                    {t('parts.plan.accept')}
                </Button>
            </>
        );
    } else if (plan.reason && !adjusted) {
        // A version with a `reason` is the agent's answer to the user: why the plan changed.
        footer = <span className={styles.caption}>{plan.reason}</span>;
    }

    const shown = adjusted ?? rowsOf(items);

    // One element for showing and adjusting, so switching keeps the plan bar's height steady. While a
    // revision is drafted, the items of the current version are shown muted.
    return (
        <div className={[styles.plan, plan.status === 'drafting' && styles.drafting].filter(Boolean).join(' ')}>
            {plan.summary && <p className={styles.summary}>{plan.summary}</p>}
            {isEditing && draft
                ? <PlanEditor rows={draft} invalid={invalid} onChange={(update) => setDraft((current) => current && update(current))} onDone={handleDone} onCancel={() => setDraft(undefined)} />
                : (
                    <ol className={styles.items}>
                        {shown.map((row, index) => {
                            const item = adjusted ? undefined : items[index];
                            let status: Status = 'pending';

                            if (item) {
                                status = statusOf(item);
                            } else if (row.isDone) {
                                status = 'done';
                            } else if (row.isRemoved) {
                                status = 'removed';
                            }

                            const Icon = ICONS[status];
                            const invalidKey = shownInvalid?.[row.key];

                            return (
                                <li key={row.key} className={[styles.item, styles[status], invalidKey && styles.invalid].filter(Boolean).join(' ')}>
                                    <span className={styles.marker} aria-hidden>
                                        {Icon ? <Icon size={11} strokeWidth={2.4} /> : index + 1}
                                    </span>
                                    <span className={styles.text}>
                                        <span className={styles.label}>
                                            {row.label}
                                            {showsChanges && row.change && MARKED_CHANGES.has(row.change) && (
                                                <span className={styles.change}>{t(`parts.plan.change.${row.change}`)}</span>
                                            )}
                                        </span>
                                        {row.target && <span><RefToken value={row.target} /></span>}
                                        {item?.detail && <span className={styles.detail}>{item.detail}</span>}
                                        {invalidKey && (
                                            <span className={styles.problem}>
                                                <CircleAlertIcon size={12} aria-hidden />
                                                {t(invalidKey)}
                                            </span>
                                        )}
                                    </span>
                                    <span className={styles.status}>{t(`chat.steps.status.${status}`)}</span>
                                </li>
                            );
                        })}
                    </ol>
                )}
            {footer && <div className={styles.foot}>{footer}</div>}
        </div>
    );
}
