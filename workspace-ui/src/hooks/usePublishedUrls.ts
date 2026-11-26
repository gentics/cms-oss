import { useQueries } from '@tanstack/react-query';

import { cmsKeys, useCmsNodes } from '@/hooks/useCmsQueries';
import type { CmsObjectRef, Publication } from '@/services/apiService/genaix/types';
import { loadCmsPage } from '@/services/cmsApiService/cmsApiService';
import { isSameCmsObject } from '@/store/useWorkspaceEventStore';

/**
 * The publication among `publications` that puts `page` online: one of it (`isSameCmsObject`: a preview names the
 * page without its connection) that is `published` and not scheduled for a `publish_at` still to come.
 * `undefined` while the page is not published, only in review, or failed.
 */
export function onlinePublicationOf(publications: Publication[], page: CmsObjectRef, now = Date.now()): Publication | undefined {
    return publications.find(({ cms_ref: ref, status, publish_at: publishAt }) => (
        isSameCmsObject(ref, page) && status === 'published' && !(publishAt && Date.parse(publishAt) > now)
    ));
}

/**
 * A page's address on the website: its `publishPath` on its node's `host`, which the CMS gives with the protocol
 * (`CmsNode.host`, `CmsPage.publishPath`). `undefined` when either is missing, or for anything but `http(s)`.
 */
export function publishedUrlFrom(host: string | undefined, publishPath: string | undefined): URL | undefined {
    if (!host || !publishPath) {
        return undefined;
    }

    try {
        const url = new URL(publishPath, host);

        return url.protocol === 'https:' || url.protocol === 'http:' ? url : undefined;
    } catch {
        return undefined;
    }
}

/**
 * Where each of `pages` is on the website, in their order: for a page `publications` show online
 * (`onlinePublicationOf`), its `publishPath` on its node's `host`, both read from the CMS (`publishedUrlFrom`),
 * which needs no CMS feature, unlike `liveUrl`. GenAIx's `cms_ref.url` is not used: GenAIx may leave it out.
 * Only an online page is loaded (`GET /rest/page/load`, under `cmsKeys.page` like `useCmsPage`); the nodes come
 * from `useCmsNodes`. `undefined` for a page not online, while loading, and when a load fails.
 */
export function usePublishedUrls(publications: Publication[], pages: CmsObjectRef[]): (URL | undefined)[] {
    const online = pages.map((page) => onlinePublicationOf(publications, page) !== undefined);
    const nodes = useCmsNodes().data;
    const loaded = useQueries({
        queries: pages.map((page, index) => ({
            queryKey: cmsKeys.page(page.id, page.node_id),
            queryFn: ({ signal }: { signal: AbortSignal }) => loadCmsPage(page.id, page.node_id, signal),
            enabled: online[index],
        })),
    });

    return pages.map((page, index) => {
        const host = nodes?.find(({ id }) => id === page.node_id)?.host;

        return online[index] ? publishedUrlFrom(host, loaded[index]?.data?.publishPath) : undefined;
    });
}
