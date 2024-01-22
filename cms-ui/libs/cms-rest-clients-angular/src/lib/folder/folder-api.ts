import {
    AnyModelType,
    ChannelSyncRequest,
    CmsFormElement,
    CmsFormElementI18nValue,
    CmsFormType,
    CropResizeParameters,
    ElasticSearchQuery,
    ElasticSearchQueryResponse,
    FileCopyRequest,
    FileCreateRequest,
    FileListResponse,
    FileRequestOptions,
    FileResponse,
    FileUploadResponse,
    FileUsageResponse,
    FolderCreateRequest,
    FolderCreateResponse,
    FolderItemOrNodeSaveOptionsMap,
    FolderItemType,
    FolderItemTypePlural,
    folderItemTypePlurals,
    FolderListOptions,
    FolderListResponse,
    FolderPublishDirSanitizeRequest,
    FolderPublishDirSanitizeResponse,
    FolderRequestOptions,
    FolderResponse,
    FolderUsageResponse,
    Form,
    FormCreateRequest,
    FormCreateResponse,
    FormListOptions,
    FormListResponse,
    FormRequestOptions,
    FormResponse, ImageRequestOptions,
    ImageResponse,
    InheritableItem,
    InheritanceRequest,
    InheritanceResponse,
    Item,
    ItemCopyResponse,
    ItemDeleteResponse,
    ItemListResponse,
    ItemMoveResponse,
    ItemType,
    ItemTypeMap,
    LocalizationsResponse,
    MultiUnlocalizeRequest,
    Node,
    NodeFeatureResponse,
    NodeLanguagesResponse,
    NodeResponse,
    NodeSettingsResponse,
    PageCopyRequest,
    PageCreateRequest,
    PageCreateResponse,
    PageListOptions,
    PageListResponse,
    PageRequestOptions,
    PageResponse,
    PageUsageResponse,
    PermissionListResponse,
    QueuedActionRequestClear,
    QueuedActionRequestPublishAt,
    QueuedActionRequestTakeOfflineAt,
    Raw,
    Response,
    ResponseCode,
    RotateParameters,
    SearchPagesOptions,
    StagedItemsMap,
    SuggestPageFileNameRequest,
    SuggestPageFileNameResponse,
    SyncObjectsRequest,
    SyncObjectsResponse,
    Template, TemplateFolderListRequest,
    TemplateListRequest,
    TemplateListResponse,
    TemplateRequestOptions,
    TemplateResponse,
    TemplateUsageResponse,
    TotalUsageResponse,
    TypedItemListResponse,
    UnlocalizeRequest,
    UsageType,
    WastebinDeleteResponse,
    WastebinRestoreResponse,
} from '@gentics/cms-models';
import { forkJoin, Observable, of as observableOf, Subject, throwError } from 'rxjs';
import { catchError, filter, map, mergeAll, mergeMap, switchMap, takeUntil } from 'rxjs/operators';
import { ApiBase, GENERIC_REQUEST_FAILED_ERROR } from '../base/api-base.service';
import { FileUploader } from '../util/file-uploader/file-uploader.service';
import { stringifyPagingSortOptions } from '../util/sort-options/sort-options';

/**
 * API methods related to the folder resource.
 *
 * Docs for the endpoints used here can be found at:
 * http://www.gentics.com/Content.Node/guides/restapi/resource_FolderResource.html
 */
export class FolderApi {
    // private elasticFolderCache: ElasticFolderCache;
    // When searching with Elastic, the response time can vary quite a bit which leads to race conditions and occasionally stale responses
    // (which are slower) coming in last and therefore "winning" over the correct, more recent yet faster response. This subject is used to
    // cancel older requests via .takeUntil() to mitigate this issue.
    private cancelInFlightSearchRequest$ = new Subject<string>();

    constructor(private apiBase: ApiBase) {
        // this.elasticFolderCache = new ElasticFolderCache();
        // this.elasticFolderCache.setFetchFunction((parentId: number) =>
        //     this.getFolders(parentId, { recursive: true, privilegeMap: false }),
        // );
    }

    /**
     * Get a single node by id.
     */
    getNode(nodeId: number): Observable<NodeResponse> {
        return this.apiBase.get(`node/load/${nodeId}`);
    }

    /**
     * Get the activated features for a node.
     */
    getNodeFeatures(nodeId: number): Observable<NodeFeatureResponse> {
        return this.apiBase.get(`node/features/${nodeId}`);
    }

    /**
     * Get the activated features for a node.
     */
    getNodeSettings(nodeId: number): Observable<NodeSettingsResponse> {
        return this.apiBase.get(`node/${nodeId}/settings`);
    }

    /**
     * Get a list of the root folders in each node. In the REST API this is done
     * by passing the "magic" id `0` to the `getFolders` endpoint.
     */
    getNodes(): Observable<FolderListResponse & { nodes: Node<Raw>[] }> {
        return this.getFolders(0).pipe(
            map((res) => {
                const nodeRequests = res.folders.map((folder) =>
                    this.getNode(folder.nodeId),
                );
                return forkJoin([observableOf(res), ...nodeRequests]);
            }),
            mergeAll(),
            map((val: any[]) => {
                const originalResponse = val[0];
                originalResponse.nodes = val.slice(1).map((res) => res.node);
                return originalResponse;
            }),
        );
    }

    /**
     * Get the sub-folders of the folder with id `parentId`.
     */
    getFolders(
        parentId: number,
        options?: FolderListOptions,
    ): Observable<FolderListResponse> {
        let request = this.apiBase.createListRequest(parentId, options);
        return this.apiBase.get(`folder/getFolders/${parentId}`, request);
    }

    /**
     * Get the pages which are children of the given folder.
     */
    getPages(
        parentId: number,
        options?: PageListOptions,
    ): Observable<PageListResponse> {
        let request = this.apiBase.createListRequest(parentId, options);
        return this.apiBase.get(`folder/getPages/${parentId}`, request);
    }

    /**
     * Get the files which are children of the given folder.
     */
    getFiles(
        parentId: number,
        options?: FolderListOptions,
    ): Observable<FileListResponse> {
        let request = this.apiBase.createListRequest(parentId, options);
        return this.apiBase.get(`folder/getFiles/${parentId}`, request);
    }

    /**
     * Get the images which are children of the given folder.
     */
    getImages(
        parentId: number,
        options?: FolderListOptions,
    ): Observable<FileListResponse> {
        let request = this.apiBase.createListRequest(parentId, options);
        return this.apiBase.get(`folder/getImages/${parentId}`, request);
    }

    /**
     * Get the forms which are children of the given folder.
     */
    getForms(
        parentId: number,
        options?: FormListOptions,
    ): Observable<FormListResponse> {
        let request = {
            ...this.apiBase.createListRequest(parentId, options),
            folderId: parentId,
        };
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }
        return this.apiBase.get('form', request);
    }

    /**
     * Sanitize the folder path.
     */
    sanitizeFolderPath(request: FolderPublishDirSanitizeRequest): Observable<FolderPublishDirSanitizeResponse> {
        return this.apiBase.post('folder/sanitize/publishDir', request);
    }

    /**
     * Query the ElasticSearch endpoint.
     */
    searchItems(
        type: FolderItemType,
        parentId: number,
        query: ElasticSearchQuery,
        options: any = {},
    ): Observable<ItemListResponse> {
        const requestId = `${type}_${parentId}`;
        this.cancelInFlightSearchRequest$.next(requestId);
        const endpoint = `elastic/${type}/_search`;

        return this.apiBase.post(endpoint, query, options).pipe(
            takeUntil(
                this.cancelInFlightSearchRequest$.pipe(
                    filter((id) => id === requestId),
                ),
            ),
            catchError((err) => {
                if (err.message === GENERIC_REQUEST_FAILED_ERROR) {
                    throw new Error('message.search_syntax_error');
                }
                throw err;
            }),
            map((res) => {
                // convert the elastic search response into the same format as used by the REST API
                const esResponse: ElasticSearchQueryResponse<
                InheritableItem<Raw>
                > = res as any;
                const items = esResponse.hits.hits.map((hit) => hit._object);
                const numItems = esResponse.hits.total;
                const hasMoreItems = items.length < numItems;
                let stagingMap: StagedItemsMap = {};

                if (typeof esResponse.hits.staging === 'string' && typeof options.package) {
                    try {
                        const parsed = JSON.parse(esResponse.hits.staging);
                        if (parsed != null && typeof parsed === 'object') {
                            // Get the Staging-Data from the current package
                            stagingMap = parsed[options.package] || {};
                        }
                    } catch (jsonParseErr) {
                        console.warn('Could not parse staging result from search!', esResponse.hits.staging);
                    }
                }

                const itemListResponse: ItemListResponse = {
                    items,
                    numItems,
                    hasMoreItems,
                    messages: [],
                    responseInfo: {
                        responseCode: ResponseCode.OK,
                    },
                    stagingStatus: stagingMap,
                };

                return itemListResponse;
            }),
        );
    }

    /**
     * Get the items of a given type which are children of the folder with parentId.
     */
    getItems(
        parentId: number,
        type: ItemType | ItemType[],
        options?: FolderListOptions,
    ): Observable<TypedItemListResponse> {
        let request = this.apiBase.createListRequest(parentId, options);
        request.type = type;
        return this.apiBase.get(`folder/getItems/${parentId}`, request);
    }

    /**
     * Get existing items of a specified type from the provided ids in a node.
     */
    getExistingItems(
        ids: number[],
        nodeId: number,
        type: ItemType,
        options?: any,
    ): Observable<Item<Raw>[]> {
        return this.apiBase
            .post(`${type}/load`, { ids: ids, nodeId: nodeId }, options)
            .pipe(
                map((res) => {
                    // Filter for just the relevant keys from response
                    let itemKey = Object.keys(res)
                        .filter(
                            (item) =>
                                folderItemTypePlurals.indexOf(
                                    <FolderItemTypePlural> item,
                                ) !== -1,
                        )[0]
                        .toString();

                    return (<any> Object).values(
                        (<any> res)[itemKey].map((item: any) => {
                            item._checkedNodeId = nodeId; // Add previous nodeId for reference
                            return item;
                        }),
                    );
                }),
            );
    }

    /**
     * Get the total usage for the given item.
     */
    getTotalUsage(
        type: ItemType,
        ids: number[],
        nodeId: number,
    ): Observable<TotalUsageResponse> {
        const idsParams = ids.map((id) => `id=${id}`).join('&');
        return this.apiBase.get(`${type}/usage/total?${idsParams}`, { nodeId });
    }

    /**
     * Get the total usage for the given item.
     */
    getUsageBy(
        type: ItemType,
        id: number,
        usedBy: 'page' | 'variant' | 'tag' | 'linkedPage',
        nodeId: number,
    ): Observable<PageUsageResponse>;
    getUsageBy(
        type: ItemType,
        id: number,
        usedBy: 'file' | 'image' | 'linkedFile' | 'linkedImage',
        nodeId: number,
    ): Observable<FileUsageResponse>;
    getUsageBy(
        type: ItemType,
        id: number,
        usedBy: 'folder',
        nodeId: number,
    ): Observable<FolderUsageResponse>;
    getUsageBy(
        type: ItemType,
        id: number,
        usedBy: 'template',
        nodeId: number,
    ): Observable<TemplateUsageResponse>;
    getUsageBy(
        type: ItemType,
        id: number,
        usedBy: UsageType,
        nodeId: number,
    ): Observable<any>;

    getUsageBy(
        type: ItemType,
        id: number,
        usedBy: any,
        nodeId: number,
    ): Observable<any> {
        return this.apiBase.get(`${type}/usage/${usedBy}`, {
            nodeId,
            id,
            template: true,
        });
    }

    /**
     * Get any localizations associated with the given item.
     */
    getLocalizations(
        type: ItemType,
        id: number,
    ): Observable<LocalizationsResponse> {
        return this.apiBase.get(`${type}/localizations/${id}`);
    }

    /**
     * Get any localizations associated with the given item.
     */
    getSyncableObjects(
        type: ItemType,
        syncOptions: SyncObjectsRequest,
    ): Observable<SyncObjectsResponse> {
        const strParams = Object.keys(syncOptions)
            .reduce((acc, key) => {
                let strOption = '';
                switch (key) {
                    case 'types':
                        strOption = syncOptions[key].join('&types=');
                        break;
                    case 'dstNodeId':
                    case 'ids':
                    case 'srcNodeId':
                    case 'recursive':
                        strOption = syncOptions[key].toString();
                }
                return acc + key + '=' + strOption + '&';
            }, '')
            .slice(0, -1);
        return this.apiBase.get(
            `${type}/usage/syncableObjects?${strParams}`,
            {},
        );
    }

    /**
     * Get an individual item.
     */
    getItem(
        id: number,
        type: 'folder',
        options?: FolderRequestOptions,
    ): Observable<FolderResponse>;
    getItem(
        id: number,
        type: 'form',
        options?: FormRequestOptions,
    ): Observable<FormResponse>;
    getItem(
        id: number,
        type: 'page',
        options?: PageRequestOptions,
    ): Observable<PageResponse>;
    getItem(
        id: number,
        type: 'file',
        options?: FileRequestOptions,
    ): Observable<FileResponse>;
    getItem(
        id: number,
        type: 'image',
        options?: ImageRequestOptions,
    ): Observable<ImageResponse>;
    getItem(
        id: number | string,
        type: 'template',
        options?: TemplateRequestOptions,
    ): Observable<TemplateResponse>;
    getItem(
        id: number,
        type: ItemType,
        options?: any,
    ): Observable<
    | FolderResponse
    | PageResponse
    | FileResponse
    | ImageResponse
    | TemplateResponse
    >;

    getItem(id: number, type: ItemType, options?: any): Observable<Response> {
        if (type === 'form' || type === 'template') {
            return this.apiBase.get(`${type}/${id}`, options).pipe(
                map((res) => {
                    // Templates don't receive a "type" by the API, so we add it
                    if (
                        type === 'template' &&
                        (res as TemplateResponse).template
                    ) {
                        (res as TemplateResponse).template.type = 'template';
                    }
                    return res;
                }),
            );
        }

        return this.apiBase.get(`${type}/load/${id}`, options);
    }

    /**
     * Get the templates which are children of the folder with parentId.
     */
    getTemplates(
        parentId: number,
        options?: TemplateFolderListRequest,
    ): Observable<TemplateListResponse> {
        const requestParams: TemplateFolderListRequest = this.apiBase.createListRequest(
            parentId,
            options,
        );
        requestParams.checkPermission = false;
        return this.apiBase
            .get(`folder/getTemplates/${parentId}`, requestParams)
            .pipe(
                map((res: TemplateListResponse) => {
                    // Templates don't receive a "type" by the API, so we add it
                    if (res.templates) {
                        res.templates.forEach(
                            (template) => (template.type = 'template'),
                        );
                    }
                    return res;
                }),
            );
    }

    /**
     * Get all the templates which are linked to any folder within the node with the specified nodeId.
     */
    getAllTemplatesOfNode(
        nodeId: number,
        options: TemplateListRequest = {},
    ): Observable<PermissionListResponse<Template>> {
        return this.apiBase.get(`node/${nodeId}/templates`, options).pipe(
            map((res: PermissionListResponse<Template>) => {
                // Templates don't receive a "type" by the API, so we add it
                if (res.items) {
                    res.items.forEach(
                        (template) => (template.type = 'template'),
                    );
                }
                return res;
            }),
        );
    }

    /**
     * Get the items of a given type which are children of the folder with parentId.
     */
    getBreadcrumbs(
        parentId: number,
        options?: FolderListOptions,
    ): Observable<FolderListResponse> {
        return this.apiBase.get(`folder/breadcrumb/${parentId}`, options);
    }

    /**
     * Get the ordered list of languages of the node.
     */
    getLanguagesOfNode(nodeId: number): Observable<NodeLanguagesResponse> {
        return this.apiBase.get(`node/getLanguages/${nodeId}`);
    }

    /**
     * Create a new folder inside a parent folder or channel
     */
    createFolder(props: {
        name: string;
        motherId: number;
        nodeId: number;
        publishDir: string;
        description: string;
        failOnDuplicate?: boolean;
    }): Observable<FolderCreateResponse> {
        let request: FolderCreateRequest = props;
        return this.apiBase.post('folder/create', request);
    }

    /**
     * Create a new form inside a parent folder or channel
     */
    createForm(props: {
        nodeId: number;
        folderId: number;
        name: string;
        description?: string;
        successPageId?: number;
        successNodeId?: number;
        data: {
            email?: string;
            successurl_i18n?: CmsFormElementI18nValue<string>;
            mailsubject_i18n?: CmsFormElementI18nValue<string>;
            mailtemp_i18n?: CmsFormElementI18nValue<string>;
            templateContext: string;
            type: CmsFormType;
            elements: CmsFormElement[];
        };
        languages: string[];
    }): Observable<FormCreateResponse> {
        let request: FormCreateRequest = {
            name: props.name,
            folderId: props.folderId,
            description: props.description,
            languages: props.languages,
            successPageId: props.successPageId,
            successNodeId: props.successNodeId,
            data: {
                templateContext: '',
                type: CmsFormType.GENERIC,
                elements: [],
            },
        };
        if (props.data) {
            request.data.email = props.data.email;
            request.data.successurl_i18n = props.data.successurl_i18n;
            request.data.mailsubject_i18n = props.data.mailsubject_i18n;
            request.data.mailtemp_i18n = props.data.mailtemp_i18n;
            request.data.templateContext = props.data.templateContext;
            request.data.type = props.data.type;
            request.data.elements = props.data.elements
                ? props.data.elements
                : [];
        }
        return this.apiBase.post('form', request, {
            nodeId: props.nodeId,
            folderId: props.folderId,
        });
    }

    /**
     * Create a new page inside a parent folder or channel
     */
    createPage(props: {
        nodeId: number;
        folderId: number;
        pageName: string;
        fileName: string;
        description: string;
        language: string;
        templateId: number;
        priority: number;
    }): Observable<PageCreateResponse> {
        let request: PageCreateRequest = props;
        return this.apiBase.post('page/create', request);
    }

    /**
     * Create a new page variation based on an existing page.
     */
    createPageVariation(config: {
        nodeId: number;
        folderId: number;
        variantId: number;
        variantChannelId: number;
    }): Observable<PageCreateResponse> {
        return this.apiBase.post('page/create', config);
    }

    /**
     * Create a new language variant of an existing page.
     */
    translatePage(
        nodeId: number,
        pageId: number,
        languageCode: string,
    ): Observable<PageCreateResponse> {
        return this.apiBase.post(
            `page/translate/${pageId}`,
            {},
            { channelId: nodeId, language: languageCode },
        );
    }

    /**
     * Save changes back to the API. The payload can be any number of properties of the item.
     */
    updateItem<T extends ItemType, R extends FolderItemOrNodeSaveOptionsMap[T]>(
        type: T,
        id: number,
        payload: Partial<ItemTypeMap<AnyModelType>[T]>,
        requestOptions: Partial<R> = {},
    ): Observable<Response> {
        const propName = type !== 'channel' ? type : 'node';
        const requestBody: any = {
            ...requestOptions,
            [propName]: payload,
        };
        if (type === 'page' && (payload as any).fileName === '') {
            requestBody.deriveFileName = true;
        }

        // Diversity in REST API standards is caused by standard migration per resource
        // POST /${type}/save/${id} might deprecate and be replaced by PUT /${type}/${id} in the future
        if (type === 'form') {
            return this.apiBase.put(`${type}/${id}`, payload);
        } else if (type === 'template') {
            return this.apiBase.post(`${type}/${id}`, requestBody);
        } else {
            return this.apiBase.post(`${type}/save/${id}`, requestBody);
        }
    }

    /**
     * Special case of item of type 'form' update clearing timemanagement property settings.
     */
    formTimeManagementClear(
        formId: number,
        payload: QueuedActionRequestClear,
    ): Observable<void> {
        let clearRequests: Observable<void>[] = [];

        if (payload.clearPublishAt) {
            clearRequests.push(
                this.apiBase.delete(`form/${formId}/publish/at`),
            );
        }
        if (payload.clearOfflineAt) {
            clearRequests.push(
                this.apiBase.delete(`form/${formId}/offline/at`),
            );
        }

        return forkJoin(clearRequests).pipe(
            mergeMap(() => observableOf(null)),
        );
    }

    /**
     * Special case of item of type 'page' update clearing timemanagement property settings.
     */
    pageTimeManagementClear(
        pageId: number,
        payload: QueuedActionRequestClear,
    ): Observable<Response> {
        return this.apiBase.post(`page/save/${pageId}`, payload);
    }

    /**
     * Create a local version of an inherited item that can be changed independently of the parent item.
     */
    localizeItem(
        type: ItemType,
        id: number,
        channelId: number,
    ): Observable<Response> {
        return this.apiBase.post(`${type}/localize/${id}`, { channelId });
    }

    /**
     * Delete a local version of an inherited item and re-inherit the item from the parent node.
     */
    unlocalizeItem(
        type: ItemType,
        id: number,
        channelId: number,
    ): Observable<Response> {
        let options: UnlocalizeRequest = {
            channelId,
            recursive: false,
        };
        return this.apiBase.post(`${type}/unlocalize/${id}`, options);
    }

    /**
     * Delete multiple local versions of an inherited item and re-inherit the items from the parent node.
     */
    unlocalizeItems(
        type: ItemType,
        ids: number[],
        channelId: number,
    ): Observable<Response> {
        let options: MultiUnlocalizeRequest = {
            ids,
            channelId,
            recursive: false,
        };
        return this.apiBase.post(`${type}/unlocalize`, options);
    }

    /**
     * Get inheritance information for an item.
     */
    getInheritance(
        type: ItemType,
        id: number,
        nodeId: number,
    ): Observable<InheritanceResponse> {
        return this.apiBase.get(`${type}/disinherit/${id}`, { nodeId });
    }

    /**
     * Set the inheritance properties of an item.
     */
    setInheritance(
        type: ItemType,
        id: number,
        settings: InheritanceRequest,
    ): Observable<Response> {
        return this.apiBase.post(`${type}/disinherit/${id}`, settings);
    }

    /**
     * Request to synchronize a local item with the channel's master node (push2master).
     */
    pushToMaster(
        type: ItemType,
        id: number,
        settings: ChannelSyncRequest,
    ): Observable<Response> {
        return this.apiBase.post(`${type}/push2master/${id}`, settings);
    }

    /**
     * Request to synchronize a local item with the channel's master node (push2master).
     */
    pushItemsToMaster(type: ItemType, settings: any): Observable<Response> {
        return this.apiBase.post(`${type}/push2master`, settings);
    }

    /**
     * Copy pages to a folder in the same or a different node
     */
    copyPages(
        ids: number[],
        sourceNode: number,
        targetFolder: number,
        targetNode: number,
    ): Observable<ItemCopyResponse> {
        let request: PageCopyRequest = {
            createCopy: true,
            nodeId: sourceNode,
            sourcePageIds: ids,
            targetFolders: [{ id: targetFolder, channelId: targetNode }],
        };
        return this.apiBase.post('page/copy', request);
    }

    /**
     * Copy forms to a folder in the same or a different node
     * As there is no dedicated API to copy a form:
     * We first get data for each form and then we create a new form based on the data.
     */
    copyForms(ids: number[], targetFolder: number, targetNode: number): Observable<any> {
        return forkJoin(ids.map(id => this.apiBase.get(`form/${id}`).pipe(
            switchMap((form: any) => {
                form.item.folderId = targetFolder;
                form.item.nodeId = targetNode;
                return this.createForm(form.item);
            }),
        )));
    }

    /**
     * Create a copy of a file it its current folder
     */
    copyFile(payload: FileCopyRequest): Observable<FileUploadResponse> {
        return this.apiBase.post('file/copy', payload);
    }

    /**
     * Move items to a different folder in the same or a different node
     */
    moveItems(type: ItemType, ids: number[], targetFolder: number, targetNode: number): Observable<ItemMoveResponse | any> {
        let params = { ids, folderId: targetFolder, nodeId: targetNode };
        if (type === 'form') {
            return forkJoin(ids.map(id => this.apiBase.put(`form/${id}/folder/${targetFolder}`, {})));
        } else {
            return this.apiBase.post(`${type}/move`, params);
        }
    }

    /**
     * Delete an item from the server
     */
    deleteItem(
        type: 'folder' | 'page' | 'file' | 'image'  | 'form',
        id: number,
        nodeId: number,
    ): Observable<ItemDeleteResponse> {
        if (type === 'form') {
            return this.apiBase.delete(`${type}/${id}`);
        }
        return this.apiBase.post(`${type}/delete/${id}`, { id }, { nodeId });
    }

    /**
     * Restore a page at a specific version
     */
    restorePageVersion(
        pageId: number,
        versionTimestamp: number,
    ): Observable<PageResponse> {
        let params = {
            version: Number(versionTimestamp),
        };
        return this.apiBase.post(`page/restore/${pageId}`, {}, params);
    }

    /**
     * Undelete items from the server
     */
    restoreFromWastebin(
        type: 'folder' | 'form' | 'page' | 'file' | 'image',
        idOrIDs: number | number[],
    ): Observable<WastebinRestoreResponse | void> {
        let ids: number[] = [].concat(idOrIDs);
        if (type === 'form') {
            return forkJoin(
                ids.map((id) =>
                    this.apiBase.post(`${type}/wastebin/${id}/restore`, {}),
                ),
            ).pipe(
                mergeMap(() => observableOf(null)),
                catchError((error: any) => throwError(new Error(error))),
            );
        } else {
            // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
            return this.apiBase.post(`${type}/wastebin/restore`, {
                ids,
            }) as Observable<WastebinRestoreResponse>;
        }
    }

    /**
     * Permanently delete items by removing from wastebin.
     */
    deleteFromWastebin(
        type: 'folder' | 'form' | 'page' | 'file' | 'image',
        idOrIDs: number | number[],
    ): Observable<WastebinDeleteResponse | void> {
        let ids: number[] = [].concat(idOrIDs);
        if (type === 'form') {
            return forkJoin(
                ids.map((id) => this.apiBase.delete(`${type}/wastebin/${id}`)),
            ).pipe(
                mergeMap(() => observableOf(null)),
                catchError((error: any) => throwError(new Error(error))),
            );
        } else {
            // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
            return this.apiBase.post(`${type}/wastebin/delete`, {
                ids,
            }) as Observable<WastebinDeleteResponse>;
        }
    }

    /**
     * Set the start page of a folder
     */
    setFolderStartpage(folderId: number, pageId: number): Observable<Response> {
        return this.apiBase.post(`folder/startpage/${folderId}`, { pageId });
    }

    /**
     * Upload files / images to the server
     */
    upload(
        type: 'file' | 'image',
        files: File[],
        params: { nodeId: number; folderId: number },
    ): FileUploader {
        return this.apiBase.upload(files, 'file/create', {
            fileField: 'fileBinaryData',
            fileNameField: 'fileName',
            params,
        });
    }

    /**
     * Trigger the CMS backend to download a file from give `sourceURL`.
     */
    uploadFromSourceUrl(payload: FileCreateRequest): Observable<FileUploadResponse> {
        return this.apiBase.post('file/create', payload);
    }


    /**
     * Upload files / images to the server to replace an existing one
     */
    replaceFile(
        type: 'file' | 'image',
        fileId: number,
        file: File,
        params: { nodeId: number; folderId: number },
        fileName?: string,
    ): FileUploader {
        let randomFilename = 'tmpfile' + Math.random().toString(36).substr(2);
        params = Object.assign({}, params, { name: randomFilename });

        return this.apiBase.upload(
            [file],
            `file/save/${fileId}`,
            {
                fileField: 'fileBinaryData',
                fileNameField: 'fileName',
                params,
            },
            fileName,
        );
    }

    /**
     * Perform a crop/resize operation on an image.
     */
    cropAndResizeImage(
        editorParameters: CropResizeParameters,
    ): Observable<FileResponse & ImageResponse> {
        return this.apiBase.post('image/resize', editorParameters);
    }

    /**
     * Perform a rotation operation on an image.
     */
    rotateImage(rotateParameters: RotateParameters): Observable<ImageResponse> {
        return this.apiBase.post<ImageResponse>(
            'image/rotate',
            rotateParameters,
        );
    }

    /**
     * Take a page offline (unpublished)
     */
    takePageOffline(pageId: number): Observable<Response> {
        return this.apiBase.post(`page/takeOffline/${pageId}`, {});
    }

    /**
     * Revert the page to the last saved version and unlock it.
     */
    cancelEditing(pageId: number): Observable<Response> {
        return this.apiBase.post(`page/cancel/${pageId}`, {});
    }

    /**
     * Publish a page.
     */
    publishPage(pageId: number, nodeId: number): Observable<Response> {
        return this.apiBase.post(
            `page/publish/${pageId}`,
            { alllang: false },
            { nodeId },
        );
    }

    /**
     * Publish a page or multiple pages. If the pageIds array contains only a single element, the simple
     * publish endpoint will be used. This endpoint enables "instant publishing". When more than one pageId is
     * passed, the multiple endpoint will be used, which cannot do instant publishing.
     */
    publishPages(pageIds: number[], nodeId: number): Observable<Response> {
        const endpoint =
            pageIds.length === 1
                ? `page/publish/${pageIds[0]}`
                : 'page/publish';
        let payload: any = { alllang: false };
        if (1 < pageIds.length) {
            payload.ids = pageIds;
        }
        return this.apiBase.post(endpoint, payload, { nodeId });
    }

    /**
     * Publish a form at a certain time.
     */
    publishFormAt(
        formId: number,
        timestamp: number,
        keepVersion: boolean = false,
    ): Observable<Response> {
        return this.apiBase.put(
            `form/${formId}/online`,
            { keepVersion },
            { at: timestamp },
        );
    }

    /**
     * Publish a page at a certain time.
     */
    publishPageAt(
        pageId: number,
        nodeId: number,
        timestamp: number,
        keepVersion: boolean = false,
    ): Observable<Response> {
        return this.apiBase.post(
            `page/publish/${pageId}`,
            {
                alllang: false,
                at: timestamp,
                keepVersion,
            } as QueuedActionRequestPublishAt,
            { nodeId },
        );
    }

    /**
     * Take form offline at a certain time.
     */
    takeFormOfflineAt(formId: number, timestamp: number): Observable<Response> {
        return this.apiBase.delete(`form/${formId}/online/`, { at: timestamp });
    }

    /**
     * Take page offline at a certain time.
     */
    takePageOfflineAt(
        pageId: number,
        nodeId: number,
        timestamp: number,
    ): Observable<Response> {
        return this.apiBase.post(
            `page/takeOffline/${pageId}`,
            {
                alllang: false,
                at: timestamp,
            } as QueuedActionRequestTakeOfflineAt,
            { nodeId },
        );
    }

    /**
     * Search pages
     *
     * @param nodeId is optional. If not set, searches will be searched among all nodes.
     */
    searchPages(
        nodeId?: number,
        options?: SearchPagesOptions,
    ): Observable<PageResponse> {
        let queryParams = {
            ...(options && { ...options }),
            folder: true,
            ...(nodeId && { nodeId }),
        };
        return this.apiBase.get('page/search', queryParams);
    }

    /**
     * Suggest the filename to be used for a (new) page, based on other metadata
     */
    suggestPageFileName(
        request: SuggestPageFileNameRequest,
    ): Observable<SuggestPageFileNameResponse> {
        return this.apiBase.post('page/suggest/filename', request);
    }

    /* FORMS ******************************************************************************************************** */

    /**
     * Publish a form.
     */
    publishForm(formId: number): Observable<Response> {
        return this.apiBase.put(`form/${formId}/online`, {});
    }

    /**
     * Publish multiple forms.
     */
    publishForms(formIds: number[]): Observable<Response[]> {
        return forkJoin(formIds.map((id) => this.publishForm(id)));
    }

    /**
     * Take a form offline (unpublished)
     */
    takeFormOffline(formId: number): Observable<Response> {
        return this.apiBase.delete(`form/${formId}/online`);
    }
}
