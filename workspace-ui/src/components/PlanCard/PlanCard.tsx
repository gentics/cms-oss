import { ArrowDownIcon, ArrowUpIcon, CheckIcon, ChevronDownIcon, ChevronRightIcon, CircleAlertIcon, CircleQuestionMarkIcon, type LucideIcon, MinusIcon, PlusIcon, Undo2Icon, XIcon } from 'lucide-react';
import { type KeyboardEvent, type ReactNode, useEffect, useId, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { focusInteractionCard } from '@/components/InteractionCard/focusInteractionCard';
import { PlannedToken, RefToken } from '@/components/MessageParts/RefToken';
import { Button } from '@/components/ui/button';
import { glanceText, isWorthAGlance, planGlance, showsProgress } from '@/helper/planGlance/planGlance';
import { type PlanRow, rowsOf } from '@/helper/planRows/planRows';
import { useInteractionLock } from '@/hooks/useInteractionLock';
import type { PlanAdjustment } from '@/hooks/usePlanAdjustment';
import type { Interaction, Plan, PlanItem, PlanStatus } from '@/services/apiService/genaix/types';
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
// While a plan is drafted or proposed, a done item was carried out by an earlier version (rc.5): only
// the next steps are shown. Done items are still sent on acceptance, as leaving one out is `422`.
function hidesDone(status: PlanStatus): boolean {
    return status === 'drafting' || status === 'proposed';
}

function statusOf(item: PlanItem): Status {
    return STATUSES.has(item.status) ? item.status : 'pending';
}

// The rows as blocks: each top-level row with its sub-steps (`parent`), in the order of the top-level
// rows. A sub-step whose parent is not among the rows stands alone.
function blocksOf(rows: PlanRow[]): PlanRow[][] {
    const heads = new Set(rows.flatMap(({ id, parent }) => (id && !parent ? [id] : [])));
    const blocks = rows.filter(({ parent }) => !parent || !heads.has(parent)).map((row) => [row]);

    for (const row of rows) {
        if (row.parent && heads.has(row.parent)) {
            blocks.find(([head]) => head?.id === row.parent)?.push(row);
        }
    }

    return blocks;
}

// Whether every kept row comes after the kept rows it depends on (rc.5: `422 dependency_order` otherwise).
function isInDependencyOrder(rows: PlanRow[]): boolean {
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
function isNeeded(rows: PlanRow[], block: PlanRow[]): boolean {
    const ids = new Set(block.flatMap(({ id }) => (id ? [id] : [])));

    return rows.some((other) => !block.includes(other) && !other.isRemoved && other.dependsOn?.some((id) => ids.has(id)));
}

// The rows with the block at `index` moved by `by` blocks, each block's sub-steps right after its head.
function moved(rows: PlanRow[], index: number, by: -1 | 1): PlanRow[] {
    const blocks = blocksOf(rows);
    const [block] = blocks.splice(index, 1);

    blocks.splice(index + by, 0, block!);

    return blocks.flat();
}

// The object a row concerns: what it created once written, else what it will create, else its target.
function ObjectToken({ row }: { row: PlanRow }) {
    if (row.created) {
        return <span><RefToken value={row.created} /></span>;
    }

    if (row.creates) {
        return <span><PlannedToken value={row.creates} /></span>;
    }

    return row.target ? <span><RefToken value={row.target} /></span> : null;
}

interface PlanEditorProps {
    rows: PlanRow[];
    onChange: (update: (rows: PlanRow[]) => PlanRow[]) => void;
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
 * A step finished in an earlier version is not shown, but kept and sent as it is, and no step moves
 * past it (rc.5: changing or leaving it out is `422`); nor does a step past a removed one, which is not
 * sent. The steps shown are numbered from 1. A step the revision removed stays struck through with
 * "Restore", which keeps it again. A move that puts a step before one it depends on, or removing
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
    // The number each block shows: its place among the blocks shown, the done ones left out.
    const numbers = blocks.map((_, index) => blocks.slice(0, index + 1).filter(([head]) => !head?.isDone).length);

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
        const isFixed = (rowsOfBlock: PlanRow[]) => rowsOfBlock.some(({ isDone, isRemoved }) => isDone || isRemoved);

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
    function substep(row: PlanRow, number: string) {
        return (
            <li key={row.key} className={[styles.item, styles.editing, styles.substep, row.isRemoved && styles.removed].filter(Boolean).join(' ')}>
                <span className={styles.marker} aria-hidden>{row.isRemoved ? <MinusIcon size={11} strokeWidth={2.4} /> : number}</span>
                <span className={styles.text}>
                    <span className={styles.label}>{row.label}</span>
                </span>
            </li>
        );
    }

    return (
        <>
            <ol className={styles.items}>
                {blocks.map((block, index) => {
                    const [row, ...substeps] = block as [PlanRow, ...PlanRow[]];

                    if (row.isDone) {
                        return null;
                    }

                    const number = numbers[index];
                    const invalidKey = invalid?.[row.key];
                    const shownSubsteps = substeps.filter(({ isDone }) => !isDone);
                    const children = shownSubsteps.length > 0 && (
                        <ol className={styles.substeps}>{shownSubsteps.map((child, i) => substep(child, `${number}.${i + 1}`))}</ol>
                    );

                    if (row.isRemoved) {
                        return (
                            <li key={row.key}>
                                <div className={[styles.item, styles.editing, styles.removed].join(' ')}>
                                    <span className={styles.marker} aria-hidden><MinusIcon size={11} strokeWidth={2.4} /></span>
                                    <span className={styles.text}>
                                        <span className={styles.label}>{row.label}</span>
                                        <ObjectToken row={row} />
                                    </span>
                                    <Button variant="ghost" size="sm" disabled={!canRestore(index)} onClick={() => restore(index)}>
                                        <Undo2Icon size={14} />
                                        {t('parts.plan.restore')}
                                    </Button>
                                </div>
                                {children}
                            </li>
                        );
                    }

                    return (
                        <li key={row.key}>
                            <div className={[styles.item, styles.editing, invalidKey && styles.invalid].filter(Boolean).join(' ')}>
                                <span className={styles.marker} aria-hidden>{number}</span>
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
                                        aria-label={t('parts.plan.step', { number })}
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
    const entries = planGlance((plan.items ?? []).filter(({ status }) => !hidesDone(plan.status) || status !== 'done'));
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
    /** The user's adjustment of the plan (`usePlanAdjustment`), whose buttons are in the plan bar's head. */
    adjustment: PlanAdjustment;
}

/**
 * The agent's work plan (`plan.updated`), shown in the plan bar above the composer while it is open
 * (`PlanBar`; design.md §12 "Plan-Schritte"; draft `partHTML` plan, `editPlan`): the plan at a glance
 * (`GlanceTiles`), then each item with its number or state, the object it concerns and its
 * status word. While the user adjusts the plan (`usePlanAdjustment`, Adjust in the plan bar's head), the
 * steps are fields right here (`PlanEditor`); once adjusted, the card shows the adjusted steps, a step
 * the server refused highlighted with what is wrong.
 *
 * A plan is living (rc.5): a revision keeps its items while it is drafted (shown muted), then proposes a
 * new version in which each item says what it changed (`PlanItem.change`). A proposed revision marks the
 * items it added or changed; a plan never accepted has no marks, every item being `added` there. A
 * removed item is struck through until the version is accepted.
 *
 * Plans at a glance (rc.5 addition): an item shows the object it concerns, what it `created` once written,
 * else what it `creates`, else its `target`. Sub-steps (`parent`) fold under their item, opened with
 * the item's "n steps"; in a plan of one item they are listed with it on one level instead. While the plan is drafted or proposed and its run waits on a question,
 * "Needs you" (`NeedsYou`) says so above the items.
 */
export function PlanCard({ plan, sessionId, adjustment }: PlanCardProps) {
    const { t } = useTranslation();
    const items = plan.items ?? [];
    const { adjusted, draft, invalid } = adjustment;
    // The items whose sub-steps are unfolded, by id.
    const [unfolded, setUnfolded] = useState<ReadonlySet<string>>(new Set());
    const substepsId = useId();
    // The question the plan's run waits on, until it is answered.
    const question = useWorkspaceEventStore((state) => {
        const session = selectSession(sessionId)(state);

        return session.interactions?.find(({ id }) => session.interactionPlans?.[id] === plan.id);
    });
    const questionLock = useInteractionLock(question);
    const needsYou = (plan.status === 'drafting' || plan.status === 'proposed') && !questionLock ? question : undefined;
    // A proposed revision against the version accepted last (`change` is stated against it, rc.5).
    const showsChanges = plan.accepted_version != null && plan.version > plan.accepted_version;

    function toggleSubsteps(id: string) {
        setUnfolded((current) => {
            const next = new Set(current);

            if (!next.delete(id)) {
                next.add(id);
            }

            return next;
        });
    }

    const itemsById = new Map(items.map((item) => [item.id, item]));

    // One row as shown: its marker (`number` or the status icon), label, object, detail and status word.
    function shownRow(row: PlanRow, number: string, extra?: ReactNode, className?: string) {
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
        const invalidKey = invalid?.[row.key];

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

    const shown = (adjusted ?? rowsOf(items)).filter(({ isDone }) => !hidesDone(plan.status) || !isDone);
    const shownBlocks = blocksOf(shown);
    // A plan of one item: it and its sub-steps on one level, numbered in order, with nothing to fold.
    const listedBlocks = shownBlocks.length === 1 ? shown.map((row) => [row]) : shownBlocks;

    // One element for showing and adjusting, so switching keeps the plan bar's height steady. While a
    // revision is drafted, the items of the current version are shown muted.
    return (
        <div className={[styles.plan, plan.status === 'drafting' && styles.drafting].filter(Boolean).join(' ')}>
            <GlanceTiles plan={plan} />
            {needsYou && <NeedsYou interaction={needsYou} />}
            {draft
                ? <PlanEditor rows={draft} invalid={invalid} onChange={adjustment.updateDraft} onDone={adjustment.finish} onCancel={adjustment.cancel} />
                : (
                    <ol className={styles.items}>
                        {listedBlocks.map((block, index) => {
                            const [row, ...substeps] = block as [PlanRow, ...PlanRow[]];
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
        </div>
    );
}
