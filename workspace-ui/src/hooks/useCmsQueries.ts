import { keepPreviousData, type QueryClient, skipToken, useInfiniteQuery, useQueries, useQuery, useQueryClient } from '@tanstack/react-query';

import {
    type CmsItem,
    type CmsItemType,
    type CmsNode,
    isCmsFeatureActive,
    listCmsFolders,
    listCmsNodes,
    listCmsTemplates,
    loadCmsPage,
    searchCmsItems,
} from '@/services/cmsApiService/cmsApiService';

/** Query keys of the CMS server state. */
export const cmsKeys = {
    nodes: () => ['cms', 'nodes'] as const,
    feature: (name: string) => ['cms', 'features', name] as const,
    searches: (nodeId: number, type: CmsItemType) => ['cms', 'nodes', nodeId, 'search', type] as const,
    search: (nodeId: number, type: CmsItemType, query: string, elastic: boolean) => [...cmsKeys.searches(nodeId, type), query, elastic] as const,
    folderLevel: (nodeId: number, folderId: number) => ['cms', 'nodes', nodeId, 'folders', folderId] as const,
    folderSearch: (nodeId: number, folderId: number, query: string) => ['cms', 'nodes', nodeId, 'folder-search', folderId, query] as const,
    images: (node: CmsNode | undefined, query: string) => ['cms', 'nodes', node?.id, 'images', query] as const,
    page: (id: number, nodeId: number | undefined) => ['cms', 'pages', id, nodeId] as const,
    templates: (folderId: number | undefined, nodeId: number | undefined) => ['cms', 'folders', folderId, 'templates', nodeId] as const,
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

/** How many folders of one level the folder search shows at a time. */
export const FOLDER_LEVEL_PAGE_SIZE = 60;

/** How many folders the folder search shows for a text. */
export const FOLDER_SEARCH_MAX_ITEMS = 50;

/**
 * The folders in the folder `folderId` of the node `nodeId` (`listCmsFolders`), `FOLDER_LEVEL_PAGE_SIZE`
 * at a time: `fetchNextPage` loads the next ones while `hasNextPage`. A minute fresh, like a node's
 * root level. Nothing is asked without both ids.
 */
export function useCmsFolderLevel(nodeId: number | undefined, folderId: number | undefined) {
    return useInfiniteQuery({
        queryKey: cmsKeys.folderLevel(nodeId ?? 0, folderId ?? 0),
        queryFn: nodeId === undefined || folderId === undefined
            ? skipToken
            : ({ pageParam, signal }) => listCmsFolders(nodeId, folderId, { skipCount: pageParam, maxItems: FOLDER_LEVEL_PAGE_SIZE, signal }),
        initialPageParam: 0,
        getNextPageParam: (last, pages) => (last.hasMore ? pages.length * FOLDER_LEVEL_PAGE_SIZE : undefined),
        staleTime: ROOT_LEVEL_STALE_MS,
    });
}

/**
 * The first `FOLDER_SEARCH_MAX_ITEMS` folders of `node` that `listCmsFolders` finds for `query`, and
 * how many it found in all; nothing is asked for an empty `query`. While a new text is searched, the
 * last answer stands in (`isPlaceholderData`).
 */
export function useCmsFolderSearch(node: CmsNode | undefined, query: string) {
    const nodeId = node?.id;
    const rootFolderId = node?.folderId;

    return useQuery({
        queryKey: cmsKeys.folderSearch(nodeId ?? 0, rootFolderId ?? 0, query),
        queryFn: nodeId === undefined || rootFolderId === undefined || query === ''
            ? skipToken
            : ({ signal }) => listCmsFolders(nodeId, rootFolderId, { search: query, maxItems: FOLDER_SEARCH_MAX_ITEMS, signal }),
        placeholderData: keepPreviousData,
    });
}

/**
 * The images in `node` that match `query` (`searchCmsItems`, images only); an empty query lists all
 * of the node's images, also those below its root folder (`recursive`). Off while there is no node.
 */
export function useCmsImages(node: CmsNode | undefined, query: string) {
    return useQuery({
        queryKey: cmsKeys.images(node, query),
        queryFn: node ? ({ signal }) => searchCmsItems(node, 'image', query, { recursive: true, signal }) : skipToken,
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
