import { type QueryClient, useQueries, useQuery, useQueryClient } from '@tanstack/react-query';

import { type CmsItem, type CmsItemType, type CmsNode, isCmsFeatureActive, listCmsNodes, searchCmsItems } from '@/services/cmsApiService/cmsApiService';

/** Query keys of the CMS server state. */
export const cmsKeys = {
    nodes: () => ['cms', 'nodes'] as const,
    feature: (name: string) => ['cms', 'features', name] as const,
    searches: (nodeId: number, type: CmsItemType) => ['cms', 'nodes', nodeId, 'search', type] as const,
    search: (nodeId: number, type: CmsItemType, query: string, elastic: boolean) => [...cmsKeys.searches(nodeId, type), query, elastic] as const,
};

/**
 * Whether the CMS feature `name` is on (`isCmsFeatureActive`); it changes only with the CMS
 * configuration, so 5 minutes fresh. Not retried: a failed request counts as off.
 */
export function useCmsFeature(name: string) {
    return useQuery({ queryKey: cmsKeys.feature(name), queryFn: () => isCmsFeatureActive(name), staleTime: 5 * 60 * 1000, retry: false });
}

/** The CMS nodes the user can see (`listCmsNodes`); they rarely change, so 5 minutes fresh. */
export function useCmsNodes() {
    return useQuery({ queryKey: cmsKeys.nodes(), queryFn: listCmsNodes, staleTime: 5 * 60 * 1000 });
}

/** One node's search result: the node and what it found. */
export interface CmsNodeResult {
    node: CmsNode;
    items: CmsItem[];
}

// How long a node's root level counts as fresh; a search is asked anew every time.
const ROOT_LEVEL_STALE_MS = 60 * 1000;

// The objects of the last search for text answered in the node `nodeId` for `type`, if any.
// `useQueries` starts every new key without data, so the query client's cache is asked instead.
function lastSearchAnswer(queryClient: QueryClient, nodeId: number, type: CmsItemType): CmsItem[] | undefined {
    const [last] = queryClient.getQueryCache()
        .findAll({ queryKey: cmsKeys.searches(nodeId, type) })
        .filter((cached) => cached.queryKey[5] !== '' && cached.state.data !== undefined)
        .sort((first, second) => second.state.dataUpdatedAt - first.state.dataUpdatedAt);

    return last?.state.data as CmsItem[] | undefined;
}

/**
 * The objects of `types` in every node of `nodes` (`searchCmsItems`), one query per node and type:
 * for an empty `query` those on the node's root level, otherwise those that match it, with
 * Elasticsearch while the CMS feature `elasticsearch` is on; a query waits until that is known.
 * `results` holds the nodes answered so far, in the order of `nodes`, each with its objects in the
 * order of `types`. While a search for new text runs, the last answer for the same node and type
 * stands in (`isStale`); `isLoading` holds until every query has answered.
 */
export function useCmsSearch(nodes: CmsNode[], query: string, types: readonly CmsItemType[]) {
    const queryClient = useQueryClient();
    const elasticFeature = useCmsFeature('elasticsearch');
    const elastic = elasticFeature.data === true;
    const pairs = nodes.flatMap((node) => types.map((type) => ({ node, type })));

    return useQueries({
        queries: pairs.map(({ node, type }) => ({
            queryKey: cmsKeys.search(node.id, type, query, elastic),
            queryFn: ({ signal }: { signal: AbortSignal }) => searchCmsItems(node, type, query, { elastic, signal }),
            enabled: query === '' || !elasticFeature.isPending,
            staleTime: query === '' ? ROOT_LEVEL_STALE_MS : 0,
            placeholderData: () => (query === '' ? undefined : lastSearchAnswer(queryClient, node.id, type)),
        })),
        combine: (queries) => ({
            results: nodes.flatMap((node): CmsNodeResult[] => {
                const answered = pairs.flatMap((pair, index) => (pair.node === node ? [queries[index]?.data] : []));
                const items = answered.flatMap((found) => found ?? []);

                return answered.some(Boolean) ? [{ node, items }] : [];
            }),
            isLoading: (query !== '' && elasticFeature.isPending) || queries.some((result) => result.isLoading || result.isPlaceholderData),
            isStale: queries.some((result) => result.isPlaceholderData),
            isError: queries.some((result) => result.isError),
        }),
    });
}
