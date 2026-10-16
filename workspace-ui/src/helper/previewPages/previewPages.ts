import type { PlanItem, Preview } from '@/services/apiService/genaix/types';

/** The kind of a group of pages: the kind of the plan item that made or changed them, `other` for none. */
export type PreviewPageKind = Exclude<PlanItem['kind'], 'report' | 'other'> | 'other';

/** The previews of the pages one kind of plan item made or changed. */
export interface PreviewPageGroup {
    kind: PreviewPageKind;
    previews: Preview[];
}

// The top-level item a page belongs to: the one that created it (`created`), else the one acting on it
// (`target`), in plan order.
function itemOf(preview: Preview, items: PlanItem[]): PlanItem | undefined {
    const id = String(preview.cms_ref.id);
    const isPage = (ref: PlanItem['created']) => ref?.type === 'page' && String(ref.id) === id;
    const topLevel = items.filter(({ parent, status }) => !parent && status !== 'removed');

    return topLevel.find(({ created }) => isPage(created)) ?? topLevel.find(({ creates, target }) => !creates && isPage(target));
}

/**
 * A plan's page previews (`PlanState.previews`, one per page) grouped by the kind of the item each page
 * belongs to (`created`, else `target`; rc.5 addition), for the preview's page menu. Groups in the order
 * their first page comes, pages in the order of `previews`; a page no item names, or one of a `report`
 * or `other` item, is in `other`.
 */
export function previewPages(previews: Preview[], items: PlanItem[]): PreviewPageGroup[] {
    const groups = new Map<PreviewPageKind, Preview[]>();

    for (const preview of previews) {
        const kind = itemOf(preview, items)?.kind;
        const groupKind: PreviewPageKind = kind === undefined || kind === 'report' || kind === 'other' ? 'other' : kind;

        groups.set(groupKind, [...(groups.get(groupKind) ?? []), preview]);
    }

    return [...groups].map(([kind, pages]) => ({ kind, previews: pages }));
}
