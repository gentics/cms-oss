import type { PlanStepInput } from '@/helper/planMessage/planMessage';
import type { ContextReference, PlanItem, PlannedObject, Problem } from '@/services/apiService/genaix/types';

/** A step of the plan as the card shows it and the user adjusts it; `key` keeps a row's field while rows move. */
export interface PlanRow extends PlanStepInput {
    key: string;
    /** Carried out in an earlier version (rc.5): sent as it is, never edited, removed or moved. */
    isDone?: boolean;
    /** Marked `removed` by the revision: struck through and not sent, unless the user restores it. */
    isRemoved?: boolean;
    /** The ids of the items this one needs, which stay before it (`PlanItem.depends_on`). */
    dependsOn?: string[];
    /** What the revision did to the item (`PlanItem.change`). */
    change?: NonNullable<PlanItem['change']>;
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

export function rowsOf(items: PlanItem[]): PlanRow[] {
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

/** The rows as they are kept: trimmed, empty ones dropped; a done one or a sub-step exactly as it is. */
export function normalized(rows: PlanRow[]): PlanRow[] {
    return rows.map((row) => (row.isDone || row.parent ? row : { ...row, label: row.label.trim() })).filter(({ label }) => label !== '');
}

/** The steps as they would be sent: removed ones left out (sending a removed item's id would bring it back). */
export function stepsOf(rows: PlanRow[]): PlanRow[] {
    return normalized(rows).filter(({ isRemoved }) => !isRemoved);
}

/**
 * Whether the steps that would be sent differ from those the plan sends as proposed (every item but the
 * removed ones): an item, its wording, order or target.
 */
export function isChanged(steps: PlanStepInput[], items: PlanItem[]): boolean {
    const signature = (step: PlanStepInput) => `${step.id ?? ''}\u0000${step.label}\u0000${step.target ? JSON.stringify(step.target) : ''}`;

    return steps.map(signature).join('\u0001') !== items.filter(({ status }) => status !== 'removed').map(signature).join('\u0001');
}

// An item of a sent `plan` part the server refused (`422 validation_failed`), by its pointer
// `/parts/<i>/plan/items/<j>` (rc.5: a done item changed, `dependency_order`).
const ITEM_POINTER = /^\/parts\/\d+\/plan\/items\/(\d+)(?:\/|$)/;

/** The rows the problem points at, by row key, each with the i18n key of what is wrong with it. */
export function invalidRowsOf(problem: Problem | undefined, sent: PlanRow[]): Record<string, string> {
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
