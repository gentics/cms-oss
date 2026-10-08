import { skipToken, useQueries, useQuery } from '@tanstack/react-query';

import { type CmsItem, type CmsNode, listCmsNodes, searchCmsItems } from '@/services/cmsApiService/cmsApiService';

/** Query keys of the CMS server state. */
export const cmsKeys = {
    nodes: () => ['cms', 'nodes'] as const,
    search: (nodeId: number, query: string) => ['cms', 'nodes', nodeId, 'search', query] as const,
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
