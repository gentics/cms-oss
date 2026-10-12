import { skipToken, useQueries, useQuery } from '@tanstack/react-query';

import { type CmsItem, type CmsNode, listCmsNodes, listCmsTemplates, loadCmsPage, searchCmsItems } from '@/services/cmsApiService/cmsApiService';

/** Query keys of the CMS server state. */
export const cmsKeys = {
    nodes: () => ['cms', 'nodes'] as const,
    search: (nodeId: number, query: string) => ['cms', 'nodes', nodeId, 'search', query] as const,
    images: (node: CmsNode | undefined, query: string) => ['cms', 'nodes', node?.id, 'images', query] as const,
    page: (id: number, nodeId: number | undefined) => ['cms', 'pages', id, nodeId] as const,
    templates: (folderId: number | undefined, nodeId: number | undefined) => ['cms', 'folders', folderId, 'templates', nodeId] as const,
};

/** The CMS nodes the user can see (`listCmsNodes`); they rarely change, so 5 minutes fresh. */
export function useCmsNodes() {
    return useQuery({ queryKey: cmsKeys.nodes(), queryFn: listCmsNodes, staleTime: 5 * 60 * 1000 });
}

/** One node's search result: the node and what it found. */
export interface CmsNodeResult {
    node: CmsNode;
    items: CmsItem[];
}

/**
 * Searches every node in `nodes` for `query` (`searchCmsItems`), one query per node; off for an
 * empty query. `results` holds the nodes answered so far, in the order of `nodes`.
 */
export function useCmsSearch(nodes: CmsNode[], query: string) {
    return useQueries({
        queries: nodes.map((node) => ({
            queryKey: cmsKeys.search(node.id, query),
            queryFn: query === '' ? skipToken : ({ signal }: { signal: AbortSignal }) => searchCmsItems(node, query, signal),
        })),
        combine: (queries) => ({
            results: nodes.flatMap((node, index): CmsNodeResult[] => {
                const items = queries[index]?.data;

                return items ? [{ node, items }] : [];
            }),
            isLoading: queries.some((result) => result.isLoading),
            isError: queries.some((result) => result.isError),
        }),
    });
}

/**
 * The images in `node` that match `query` (`searchCmsItems`, images only); an empty query lists the
 * node's images. Off while there is no node.
 */
export function useCmsImages(node: CmsNode | undefined, query: string) {
    return useQuery({
        queryKey: cmsKeys.images(node, query),
        queryFn: node ? ({ signal }) => searchCmsItems(node, query, signal, ['image']) : skipToken,
    });
}

/** The page `id` as the CMS has it now (`loadCmsPage`); each mount loads it again, so the page settings show its current values. */
export function useCmsPage(id: number, nodeId: number | undefined) {
    return useQuery({
        queryKey: cmsKeys.page(id, nodeId),
        queryFn: ({ signal }) => loadCmsPage(id, nodeId, signal),
    });
}

/** The templates linked to the folder `folderId` (`listCmsTemplates`); off while the folder is not known. */
export function useCmsTemplates(folderId: number | undefined, nodeId: number | undefined) {
    return useQuery({
        queryKey: cmsKeys.templates(folderId, nodeId),
        queryFn: folderId === undefined ? skipToken : ({ signal }) => listCmsTemplates(folderId, nodeId, signal),
    });
}
