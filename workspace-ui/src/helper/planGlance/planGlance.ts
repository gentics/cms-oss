import type { TFunction } from 'i18next';

import type { PlanItem, PlanStatus } from '@/services/apiService/genaix/types';

type Kind = PlanItem['kind'];

// The statuses of an accepted plan, whose glance shows how far it got ("1/3").
const ACCEPTED: ReadonlySet<PlanStatus> = new Set<PlanStatus>(['accepted', 'running', 'done', 'released', 'published', 'review_requested']);

/** Whether a plan's glance shows its progress (`done/total`): once it is accepted, not while drafted or proposed. */
export function showsProgress(status: PlanStatus): boolean {
    return ACCEPTED.has(status);
}

// The kinds a glance counts: the items that write. `report` and `other` are never counted.
const COUNTED: ReadonlySet<Kind> = new Set<Kind>(['create', 'update', 'link', 'translate', 'publish', 'construct']);

// The kinds that bring an object into being, counted as "+3".
const NEW_KINDS: ReadonlySet<Kind> = new Set<Kind>(['create', 'translate']);

/** One group of a plan at a glance: the top-level items of one kind on one object type. */
export interface GlanceEntry {
    kind: Kind;
    /** The type of the object the items concern: `creates.type`, else `target.type`; absent when they name none. */
    type?: string;
    /** How many items. */
    total: number;
    /** How many of them are `done`. */
    done: number;
    /** They bring objects into being (`create`, `translate`, or every item says what it `creates`): shown as "+3". */
    isNew: boolean;
}

/**
 * The plan at a glance (rc.5 addition, 09-integration-guide.md "The plan at a glance"): its items
 * without a `parent`, but neither `removed` nor `report` or `other`, grouped by `kind` and by the type of
 * the object each concerns (`creates.type`, else `target.type`), in the order the groups first appear.
 */
export function planGlance(items: PlanItem[]): GlanceEntry[] {
    const groups = new Map<string, GlanceEntry & { allCreate: boolean }>();

    for (const item of items) {
        if (item.parent || item.status === 'removed' || !COUNTED.has(item.kind)) {
            continue;
        }

        const type = item.creates?.type ?? item.target?.type;
        const key = `${item.kind}\u0000${type ?? ''}`;
        const group = groups.get(key) ?? { kind: item.kind, ...(type && { type }), total: 0, done: 0, isNew: false, allCreate: true };

        group.total += 1;
        group.done += item.status === 'done' ? 1 : 0;
        group.allCreate &&= item.creates !== undefined;
        groups.set(key, group);
    }

    return [...groups.values()].map(({ allCreate, ...entry }) => ({ ...entry, isNew: NEW_KINDS.has(entry.kind) || allCreate }));
}

/** Whether a glance says more than the plan's items themselves: it counts two items or more. */
export function isWorthAGlance(entries: GlanceEntry[]): boolean {
    return entries.reduce((count, { total }) => count + total, 0) > 1;
}

/**
 * How a glance entry reads: its figure ("+3", "2", or "1/3" with `withProgress`) and its label ("folders",
 * "pages edited"), from `planGlance.<kind>_<type>`, else `planGlance.<kind>` (i18next context, by count).
 */
export function glanceText(t: TFunction, entry: GlanceEntry, withProgress: boolean): { value: string; label: string } {
    let value = String(entry.total);

    if (withProgress) {
        value = `${entry.done}/${entry.total}`;
    } else if (entry.isNew) {
        value = `+${entry.total}`;
    }

    return { value, label: t(`planGlance.${entry.kind}`, { context: entry.type, count: entry.total }) };
}
