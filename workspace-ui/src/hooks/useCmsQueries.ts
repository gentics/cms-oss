import { keepPreviousData, type QueryClient, skipToken, useInfiniteQuery, useQueries, useQuery, useQueryClient } from '@tanstack/react-query';

import { type Guideline, guidelineFromFile, GUIDELINES_DIR, sortGuidelines } from '@/helper/guidelines/guidelines';
import {
    canPublishCmsPage,
    type CmsItem,
    type CmsItemType,
    type CmsNode,
    isCmsFeatureActive,
    listCmsFolders,
    listCmsLanguages,
    listCmsNodePackages,
    listCmsNodes,
    listCmsPackageInternalFiles,
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
    folderPages: (node: CmsNode | undefined, folderId: number | undefined) => ['cms', 'nodes', node?.id, 'folder-pages', folderId] as const,
    page: (id: number, nodeId: number | undefined) => ['cms', 'pages', id, nodeId] as const,
    publishPermission: (id: number, nodeId: number | undefined) => ['cms', 'pages', id, nodeId, 'perm', 'publish'] as const,
    templates: (folderId: number | undefined, nodeId: number | undefined) => ['cms', 'folders', folderId, 'templates', nodeId] as const,
    languages: (nodeId: number) => ['cms', 'nodes', nodeId, 'languages'] as const,
    guidelines: (nodeId: number) => ['cms', 'nodes', nodeId, 'guidelines'] as const,
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

/**
 * The languages of the nodes `nodeIds` (`listCmsLanguages`), each code once, in the order the nodes list
 * them; like the nodes, 5 minutes fresh. `isPending` while any node's list is still loading.
 */
export function useCmsLanguages(nodeIds: number[]) {
    return useQueries({
        queries: nodeIds.map((nodeId) => ({
            queryKey: cmsKeys.languages(nodeId),
            queryFn: ({ signal }: { signal: AbortSignal }) => listCmsLanguages(nodeId, signal),
            staleTime: 5 * 60 * 1000,
        })),
        combine: (results) => ({
            languages: results
                .flatMap((result) => result.data ?? [])
                .filter((language, index, all) => all.findIndex(({ code }) => code === language.code) === index),
            isPending: results.some((result) => result.isPending),
        }),
    });
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

/**
 * The pages directly in the folder `folderId` of `node` (`searchCmsItems` with that folder, pages only,
 * not recursive). Off while the node or the folder is not known.
 */
export function useCmsFolderPages(node: CmsNode | undefined, folderId: number | undefined) {
    return useQuery({
        queryKey: cmsKeys.folderPages(node, folderId),
        queryFn: node && folderId !== undefined ? ({ signal }) => searchCmsItems(node, 'page', '', { folderId, signal }) : skipToken,
    });
}

/** The page `id` as the CMS has it now (`loadCmsPage`); each mount loads it again, so the page settings show its current values. */
export function useCmsPage(id: number, nodeId: number | undefined) {
    return useQuery({
        queryKey: cmsKeys.page(id, nodeId),
        queryFn: ({ signal }) => loadCmsPage(id, nodeId, signal),
    });
}

/**
 * Whether the current CMS user may publish every one of `pages` (`canPublishCmsPage`, one query per
 * page). `false` while a permission is still loading or could not be read, so nothing is offered that
 * the user may not do; `true` for no pages.
 */
export function useCanPublishCmsPages(pages: readonly { id: number; node_id?: number }[]): boolean {
    return useQueries({
        queries: pages.map(({ id, node_id: nodeId }) => ({
            queryKey: cmsKeys.publishPermission(id, nodeId),
            queryFn: ({ signal }: { signal: AbortSignal }) => canPublishCmsPage(id, nodeId, signal),
        })),
        combine: (results) => results.every(({ data }) => data === true),
    });
}

/**
 * Whether the current CMS user may publish `page` (`canPublishCmsPage`, the query `useCanPublishCmsPages`
 * shares); `undefined` while it loads, when it could not be read, and without a page.
 */
export function useCanPublishCmsPage(page: { id: number; node_id?: number } | undefined): boolean | undefined {
    return useQuery({
        queryKey: cmsKeys.publishPermission(page?.id ?? 0, page?.node_id),
        queryFn: page ? ({ signal }) => canPublishCmsPage(page.id, page.node_id, signal) : skipToken,
    }).data;
}

/** The templates linked to the folder `folderId` (`listCmsTemplates`); off while the folder is not known. */
export function useCmsTemplates(folderId: number | undefined, nodeId: number | undefined) {
    return useQuery({
        queryKey: cmsKeys.templates(folderId, nodeId),
        queryFn: folderId === undefined ? skipToken : ({ signal }) => listCmsTemplates(folderId, nodeId, signal),
    });
}

// Every guideline of the node `nodeId`: the files in `files-internal/guidelines/` of each of its
// packages that are named as guidelines (`guidelineFromFile`), by priority.
async function listCmsGuidelines(nodeId: number, signal: AbortSignal): Promise<Guideline[]> {
    const packages = await listCmsNodePackages(nodeId, signal);
    const perPackage = await Promise.all(packages.map(async ({ name }) => {
        const files = await listCmsPackageInternalFiles(name, GUIDELINES_DIR, signal);

        return files.flatMap((file) => (file.directory ? [] : guidelineFromFile(name, file.name) ?? []));
    }));

    return sortGuidelines(perPackage.flat());
}

/**
 * The guidelines of the node `nodeId`, default and available ones, by priority (`listCmsGuidelines`).
 * Asked anew on every mount, as a Chief Content Creator may have added or renamed one meanwhile. Off
 * without a node.
 */
export function useCmsGuidelines(nodeId: number | undefined) {
    return useQuery({
        queryKey: cmsKeys.guidelines(nodeId ?? 0),
        queryFn: nodeId === undefined ? skipToken : ({ signal }) => listCmsGuidelines(nodeId, signal),
    });
}
