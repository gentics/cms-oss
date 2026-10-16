import { ArrowDownIcon, ArrowUpIcon, CheckCheckIcon, CheckIcon, ChevronDownIcon, ChevronRightIcon, CircleAlertIcon, CircleQuestionMarkIcon, type LucideIcon, MinusIcon, PencilIcon, PlusIcon, Undo2Icon, XIcon } from 'lucide-react';
import { type KeyboardEvent, type ReactNode, useEffect, useId, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { focusInteractionCard } from '@/components/InteractionCard/focusInteractionCard';
import { PlannedToken, RefToken } from '@/components/MessageParts/RefToken';
import { Button } from '@/components/ui/button';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { glanceText, isWorthAGlance, planGlance, showsProgress } from '@/helper/planGlance/planGlance';
import { planMessage, type PlanStepInput } from '@/helper/planMessage/planMessage';
import { isStalePlanCode, reloadPlans } from '@/helper/planReload/planReload';
import { useSendMessage } from '@/hooks/useGenaixQueries';
import { useInteractionLock } from '@/hooks/useInteractionLock';
import { GenaixApiError } from '@/services/apiService/apiService';
import type { ContextReference, Interaction, Plan, PlanItem, PlannedObject, Problem } from '@/services/apiService/genaix/types';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

import styles from './PlanCard.module.css';

type Status = PlanItem['status'];
type Change = NonNullable<PlanItem['change']>;

// The marker icon per status; pending and running show the item's number instead.
const ICONS: Partial<Record<Status, LucideIcon>> = { done: CheckIcon, skipped: MinusIcon, failed: XIcon, removed: MinusIcon };
const STATUSES = new Set<string>(['pending', 'running', 'done', 'skipped', 'failed', 'removed']);

// The changes a revision marks on an item (rc.5); `unchanged` and `removed` get no marker (a removed
// item is struck through).
const MARKED_CHANGES: ReadonlySet<Change> = new Set<Change>(['added', 'changed']);

// The most items a `plan` part may carry (`UserPlan.items`, 200 since the rc.5 addition).
const MAX_ITEMS = 200;

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
    /**
     * The item whose object this one works on (`PlanItem.parent`, rc.5 addition): a sub-step, folded under
     * it, never edited, and moved and removed with it.
     */
    parent?: string;
    /** What the item will create (`PlanItem.creates`). */
    creates?: PlannedObject;
    /** What the item created, once written (`PlanItem.created`). */
    created?: ContextReference;
}

function rowsOf(items: PlanItem[]): Row[] {
    return items.map(({ id, label, target, status, depends_on: dependsOn, change, parent, creates, created }) => ({
        key: id,
        id,
        label,
        target,
        ...(status === 'done' && { isDone: true }),
        ...(status === 'removed' && { isRemoved: true }),
        ...(dependsOn?.length && { dependsOn }),
        ...(change && { change }),
        ...(parent && { parent }),
        ...(creates && { creates }),
        ...(created && { created }),
    }));
}

// The rows as blocks: each top-level row with its sub-steps (`parent`), in the order of the top-level
// rows. A sub-step whose parent is not among the rows stands alone.
function blocksOf(rows: Row[]): Row[][] {
    const heads = new Set(rows.flatMap(({ id, parent }) => (id && !parent ? [id] : [])));
    const blocks = rows.filter(({ parent }) => !parent || !heads.has(parent)).map((row) => [row]);

    for (const row of rows) {
        if (row.parent && heads.has(row.parent)) {
            blocks.find(([head]) => head?.id === row.parent)?.push(row);
        }
    }

    return blocks;
}

// The rows as they are kept: trimmed, empty ones dropped; a done one or a sub-step exactly as it is.
function normalized(rows: Row[]): Row[] {
    return rows.map((row) => (row.isDone || row.parent ? row : { ...row, label: row.label.trim() })).filter(({ label }) => label !== '');
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

// Whether a kept row outside `block` depends on a row of it, so the block cannot be dropped (rc.5:
// `422 dependency_order`). Its own sub-steps may depend on the head.
function isNeeded(rows: Row[], block: Row[]): boolean {
    const ids = new Set(block.flatMap(({ id }) => (id ? [id] : [])));

    return rows.some((other) => !block.includes(other) && !other.isRemoved && other.dependsOn?.some((id) => ids.has(id)));
}

// The rows with the block at `index` moved by `by` blocks, each block's sub-steps right after its head.
function moved(rows: Row[], index: number, by: -1 | 1): Row[] {
    const blocks = blocksOf(rows);
    const [block] = blocks.splice(index, 1);

    blocks.splice(index + by, 0, block!);

    return blocks.flat();
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

// The object a row concerns: what it created once written, else what it will create, else its target.
function ObjectToken({ row }: { row: Row }) {
    if (row.created) {
        return <span><RefToken value={row.created} /></span>;
    }

    if (row.creates) {
        return <span><PlannedToken value={row.creates} /></span>;
    }

    return row.target ? <span><RefToken value={row.target} /></span> : null;
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
 * removes it, Esc cancels, Ctrl/Cmd+Enter is Done. At most `MAX_ITEMS` steps are kept.
 *
 * A step finished in an earlier version is read-only: no field, no remove, and it does not move, nor
 * does a step past it (rc.5: changing it is `422`); nor does a step past a removed one, which is not sent. A step the revision removed stays struck through
 * with "Restore", which keeps it again. A move that puts a step before one it depends on, or removing
 * one another kept step depends on, is not offered (`422 dependency_order`).
 *
 * A sub-step (`parent`, rc.5 addition) is read-only under its step: it moves with it, is removed and
 * restored with it, and a step added after it goes after its last sub-step.
 */
function PlanEditor({ rows, onChange: setRows, onDone, onCancel, invalid }: PlanEditorProps) {
    const { t } = useTranslation();
    // The row whose field gets the focus after the next render: the first editable at the start, then
    // the one added, moved or before the one removed.
    const focusKey = useRef<string | undefined>(rows.find((row) => !row.isDone && !row.isRemoved && !row.parent)?.key);
    const fields = useRef(new Map<string, HTMLInputElement>());
    const blocks = blocksOf(rows);
    const keptBlocks = blocks.filter(([head]) => !head?.isRemoved).length;
    const keptCount = rows.filter(({ isRemoved }) => !isRemoved).length;
    const canAdd = keptCount < MAX_ITEMS;

    useEffect(() => {
        if (focusKey.current) {
            fields.current.get(focusKey.current)?.focus();
            focusKey.current = undefined;
        }
    });

    function update(key: string, label: string) {
        setRows((current) => current.map((row) => (row.key === key ? { ...row, label } : row)));
    }

    // A new step after the block of `afterKey` (after its last sub-step), at the end without one.
    function add(afterKey?: string) {
        if (!canAdd) {
            return;
        }

        const row = { key: crypto.randomUUID(), label: '' };

        setRows((current) => {
            const block = blocksOf(current).find(([head]) => head?.key === afterKey);
            const at = block ? current.findIndex(({ key }) => key === block.at(-1)?.key) + 1 : current.length;

            return [...current.slice(0, at), row, ...current.slice(at)];
        });
        focusKey.current = row.key;
    }

    function remove(index: number) {
        const keys = new Set(blocks[index]?.map(({ key }) => key));

        setRows((current) => current.filter(({ key }) => !keys.has(key)));
        focusKey.current = blocks[index - 1]?.[0]?.key ?? blocks[index + 1]?.[0]?.key;
    }

    function canRestore(index: number): boolean {
        return keptCount + (blocks[index]?.length ?? 0) <= MAX_ITEMS;
    }

    function restore(index: number) {
        const keys = new Set(blocks[index]?.map(({ key }) => key));

        setRows((current) => current.map((row) => (keys.has(row.key) ? { ...row, isRemoved: false } : row)));
    }

    function canMove(index: number, by: -1 | 1): boolean {
        const block = blocks[index];
        const other = blocks[index + by];
        const isFixed = (rowsOfBlock: Row[]) => rowsOfBlock.some(({ isDone, isRemoved }) => isDone || isRemoved);

        return Boolean(block && other) && !isFixed(block!) && !isFixed(other!) && isInDependencyOrder(moved(rows, index, by));
    }

    function move(index: number, by: -1 | 1) {
        setRows((current) => moved(current, index, by));
        focusKey.current = blocks[index]?.[0]?.key;
    }

    function canRemove(index: number): boolean {
        return keptBlocks > 1 && !isNeeded(rows, blocks[index] ?? []);
    }

    function handleKeyDown(event: KeyboardEvent<HTMLInputElement>, index: number) {
        const row = blocks[index]?.[0];

        if (event.key === 'Escape') {
            event.preventDefault();
            onCancel();
        } else if (event.key === 'Enter' && (event.metaKey || event.ctrlKey)) {
            event.preventDefault();
            onDone();
        } else if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            add(row?.key);
        } else if (event.key === 'Backspace' && row?.label === '' && canRemove(index)) {
            event.preventDefault();
            remove(index);
        }
    }

    // A sub-step: read-only, struck through when removed.
    function substep(row: Row, number: string) {
        const Icon = row.isDone ? CheckIcon : row.isRemoved ? MinusIcon : undefined;

        return (
            <li key={row.key} className={[styles.item, styles.editing, styles.substep, row.isDone && styles.done, row.isRemoved && styles.removed].filter(Boolean).join(' ')}>
                <span className={styles.marker} aria-hidden>{Icon ? <Icon size={11} strokeWidth={2.4} /> : number}</span>
                <span className={styles.text}>
                    <span className={styles.label}>{row.label}</span>
                </span>
                {row.isDone && <span className={styles.status}>{t('chat.steps.status.done')}</span>}
            </li>
        );
    }

    return (
        <>
            <ol className={styles.items}>
                {blocks.map((block, index) => {
                    const [row, ...substeps] = block as [Row, ...Row[]];
                    const invalidKey = invalid?.[row.key];
                    const children = substeps.length > 0 && (
                        <ol className={styles.substeps}>{substeps.map((child, i) => substep(child, `${index + 1}.${i + 1}`))}</ol>
                    );

                    if (row.isDone || row.isRemoved) {
                        const Icon = row.isDone ? CheckIcon : MinusIcon;

                        return (
                            <li key={row.key}>
                                <div className={[styles.item, styles.editing, styles[row.isDone ? 'done' : 'removed']].join(' ')}>
                                    <span className={styles.marker} aria-hidden><Icon size={11} strokeWidth={2.4} /></span>
                                    <span className={styles.text}>
                                        <span className={styles.label}>{row.label}</span>
                                        <ObjectToken row={row} />
                                    </span>
                                    {row.isDone
                                        ? <span className={styles.status}>{t('chat.steps.status.done')}</span>
                                        : (
                                            <Button variant="ghost" size="sm" disabled={!canRestore(index)} onClick={() => restore(index)}>
                                                <Undo2Icon size={14} />
                                                {t('parts.plan.restore')}
                                            </Button>
                                        )}
                                </div>
                                {children}
                            </li>
                        );
                    }

                    return (
                        <li key={row.key}>
                            <div className={[styles.item, styles.editing, invalidKey && styles.invalid].filter(Boolean).join(' ')}>
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
                                        onChange={(event) => update(row.key, event.target.value)}
                                        onKeyDown={(event) => handleKeyDown(event, index)}
                                    />
                                    <ObjectToken row={row} />
                                    {invalidKey && <span className={styles.problem}>{t(invalidKey)}</span>}
                                </span>
                                <span className={styles.rowActions}>
                                    <Button variant="ghost" size="icon" aria-label={t('parts.plan.moveUp')} disabled={!canMove(index, -1)} onClick={() => move(index, -1)}>
                                        <ArrowUpIcon size={14} />
                                    </Button>
                                    <Button variant="ghost" size="icon" aria-label={t('parts.plan.moveDown')} disabled={!canMove(index, 1)} onClick={() => move(index, 1)}>
                                        <ArrowDownIcon size={14} />
                                    </Button>
                                    <Button variant="ghost" size="icon" aria-label={t('parts.plan.remove')} disabled={!canRemove(index)} onClick={() => remove(index)}>
                                        <XIcon size={14} />
                                    </Button>
                                </span>
                            </div>
                            {children}
                        </li>
                    );
                })}
            </ol>
            <div className={styles.add}>
                <Button variant="ghost" size="sm" disabled={!canAdd} onClick={() => add()}>
                    <PlusIcon size={14} />
                    {t('parts.plan.add')}
                </Button>
                {!canAdd && <span className={styles.caption}>{t('parts.plan.maxItems', { count: MAX_ITEMS })}</span>}
            </div>
        </>
    );
}

/**
 * The plan at a glance (rc.5 addition; design.md §12 "KPI-Kachel"): one tile per kind and object type
 * of its top-level items (`planGlance`), "+3 folders" while it is proposed, "1/3 folders" once accepted.
 * None for a plan of one item, which says the same itself (`isWorthAGlance`).
 */
function GlanceTiles({ plan }: { plan: Plan }) {
    const { t } = useTranslation();
    const entries = planGlance(plan.items ?? []);
    const withProgress = showsProgress(plan.status);

    if (!isWorthAGlance(entries)) {
        return null;
    }

    return (
        <div className={styles.glance} role="group" aria-label={t('parts.plan.glance')}>
            {entries.map((entry) => {
                const { value, label } = glanceText(t, entry, withProgress);

                return (
                    <span key={`${entry.kind}-${entry.type ?? ''}`} className={styles.tile} data-complete={withProgress && entry.done === entry.total ? '' : undefined}>
                        <span className={styles.tileValue}>{value}</span>
                        {' '}
                        <span className={styles.tileLabel}>{label}</span>
                    </span>
                );
            })}
        </div>
    );
}

/**
 * What waits for the user before the plan can go on (09-integration-guide.md "The plan at a glance",
 * "Needs you"): the question its run is blocked on, with "Answer", which takes the user to its card in
 * the conversation (`focusInteractionCard`).
 */
function NeedsYou({ interaction }: { interaction: Interaction }) {
    const { t } = useTranslation();

    return (
        <section className={styles.needsYou} aria-label={t('parts.plan.needsYou.title')}>
            <span className={styles.needsYouText}>
                <span className={styles.needsYouTitle}>
                    <CircleQuestionMarkIcon size={14} aria-hidden />
                    {t('parts.plan.needsYou.title')}
                </span>
                <span className={styles.needsYouPrompt}>{interaction.prompt}</span>
            </span>
            <Button variant="primary" size="sm" onClick={() => focusInteractionCard(interaction.id)}>
                {t('parts.plan.needsYou.answer')}
            </Button>
        </section>
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
 * (`PlanBar`; design.md §12 "Plan-Schritte"; draft `partHTML` plan, `editPlan`): the plan at a glance
 * (`GlanceTiles`), then each item with its number or state, the object it concerns and its
 * status word. While the plan is `proposed` and no
 * run works or waits on a question, Adjust edits the steps in place and Accept plan sends the plan to the agent (`planMessage`):
 * as proposed, or as adjusted. A later version says why it changed (`reason`). Refused because the plan
 * moved on meanwhile (`409 plan_outdated`, `plan_closed`, `plan_not_proposed`), the plans are read again
 * (`reloadPlans`); refused for an item (`422` pointing at `/parts/<i>/plan/items/<j>`), that row is
 * highlighted with what is wrong instead of an error notification.
 *
 * A plan is living (rc.5): a revision keeps its items while it is drafted (shown muted), then proposes a
 * new version in which each item says what it changed (`PlanItem.change`). A new version drops what the
 * user did to the previous one (an adjustment, an acceptance), so it can be adjusted and accepted again. A proposed revision marks the
 * items it added or changed; a plan never accepted has no marks, every item being `added` there. A
 * removed item is struck through until the version is accepted.
 *
 * Plans at a glance (rc.5 addition): an item shows the object it concerns, what it `created` once written,
 * else what it `creates`, else its `target`. Sub-steps (`parent`) fold under their item, opened with
 * the item's "n steps"; in a plan of one item they are listed with it on one level instead. While the plan is drafted or proposed and its run waits on a question,
 * "Needs you" (`NeedsYou`) says so above the items.
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
    // The items whose sub-steps are unfolded, by id.
    const [unfolded, setUnfolded] = useState<ReadonlySet<string>>(new Set());
    // The version the user's adjustment and acceptance are for. A revision proposes the same plan
    // (same `id`, so this card stays) at a new version (rc.5), which the user accepts afresh.
    const [ownVersion, setOwnVersion] = useState(plan.version);

    if (ownVersion !== plan.version) {
        setOwnVersion(plan.version);
        setAdjusted(undefined);
        setDraft(undefined);
        setIsAccepted(false);
        setInvalid(undefined);
    }
    const substepsId = useId();
    // The question the plan's run waits on, until it is answered.
    const question = useWorkspaceEventStore((state) => {
        const session = selectSession(sessionId)(state);

        return session.interactions?.find(({ id }) => session.interactionPlans?.[id] === plan.id);
    });
    const questionLock = useInteractionLock(question);
    const needsYou = (plan.status === 'drafting' || plan.status === 'proposed') && !questionLock ? question : undefined;
    // Only a proposed plan can be accepted (contract: `409 plan_not_proposed` otherwise).
    const isProposed = plan.status === 'proposed';
    // The user's adjustment counts only while the plan waits for them: once it is accepted, the card
    // shows the plan as GenAIx executes it.
    const adjusted = isProposed ? adjustedRows : undefined;
    // Not while the plan's run waits on a question: accepting would be refused then (`run_already_active`).
    const canAct = isProposed && canAnswer && !isAccepted && !question;
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

    function toggleSubsteps(id: string) {
        setUnfolded((current) => {
            const next = new Set(current);

            if (!next.delete(id)) {
                next.add(id);
            }

            return next;
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

    const itemsById = new Map(items.map((item) => [item.id, item]));

    // One row as shown: its marker (`number` or the status icon), label, object, detail and status word.
    function shownRow(row: Row, number: string, extra?: ReactNode, className?: string) {
        const item = adjusted ? undefined : itemsById.get(row.key);
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
            <div className={[styles.item, styles[status], invalidKey && styles.invalid, className].filter(Boolean).join(' ')}>
                <span className={styles.marker} aria-hidden>
                    {Icon ? <Icon size={11} strokeWidth={2.4} /> : number}
                </span>
                <span className={styles.text}>
                    <span className={styles.label}>
                        {row.label}
                        {showsChanges && row.change && MARKED_CHANGES.has(row.change) && (
                            <span className={styles.change}>{t(`parts.plan.change.${row.change}`)}</span>
                        )}
                    </span>
                    <ObjectToken row={row} />
                    {item?.detail && <span className={styles.detail}>{item.detail}</span>}
                    {invalidKey && (
                        <span className={styles.problem}>
                            <CircleAlertIcon size={12} aria-hidden />
                            {t(invalidKey)}
                        </span>
                    )}
                    {extra}
                </span>
                <span className={styles.status}>{t(`chat.steps.status.${status}`)}</span>
            </div>
        );
    }

    const shown = adjusted ?? rowsOf(items);
    const shownBlocks = blocksOf(shown);
    // A plan of one item: it and its sub-steps on one level, numbered in order, with nothing to fold.
    const listedBlocks = shownBlocks.length === 1 ? shown.map((row) => [row]) : shownBlocks;

    // One element for showing and adjusting, so switching keeps the plan bar's height steady. While a
    // revision is drafted, the items of the current version are shown muted.
    return (
        <div className={[styles.plan, plan.status === 'drafting' && styles.drafting].filter(Boolean).join(' ')}>
            <GlanceTiles plan={plan} />
            {needsYou && <NeedsYou interaction={needsYou} />}
            {isEditing && draft
                ? <PlanEditor rows={draft} invalid={invalid} onChange={(update) => setDraft((current) => current && update(current))} onDone={handleDone} onCancel={() => setDraft(undefined)} />
                : (
                    <ol className={styles.items}>
                        {listedBlocks.map((block, index) => {
                            const [row, ...substeps] = block as [Row, ...Row[]];
                            const isUnfolded = unfolded.has(row.key);
                            const listId = `${substepsId}-${index}`;
                            const toggle = substeps.length > 0 && (
                                <Button variant="ghost" size="sm" aria-expanded={isUnfolded} aria-controls={listId} onClick={() => toggleSubsteps(row.key)}>
                                    {isUnfolded ? <ChevronDownIcon size={14} /> : <ChevronRightIcon size={14} />}
                                    {t('parts.plan.substeps', { count: substeps.length })}
                                </Button>
                            );

                            return (
                                <li key={row.key}>
                                    {shownRow(row, String(index + 1), toggle)}
                                    {substeps.length > 0 && (
                                        <ol id={listId} className={styles.substeps} hidden={!isUnfolded}>
                                            {substeps.map((child, i) => <li key={child.key}>{shownRow(child, `${index + 1}.${i + 1}`, undefined, styles.substep)}</li>)}
                                        </ol>
                                    )}
                                </li>
                            );
                        })}
                    </ol>
                )}
            {footer && <div className={styles.foot}>{footer}</div>}
        </div>
    );
}
