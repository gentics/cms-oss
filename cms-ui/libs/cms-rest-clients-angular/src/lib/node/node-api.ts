import {
    EntityIdType,
    FeatureListResponse,
    FeatureModelListResponse,
    ItemDeleteResponse,
    ItemSaveResponse,
    Language,
    ListResponse,
    Node,
    NodeCopyRequest,
    NodeCopyRequestOptions,
    NodeCreateRequest,
    NodeDeleteRequestOptions,
    NodeFeatureListRequestOptions,
    NodeFeatures,
    NodeLanguageListRequest,
    NodeLanguagesListResponse,
    NodeListRequestOptions,
    NodeRequestOptions,
    NodeResponse,
    NodeSaveRequest,
    NodeSettingsResponse,
    PagedConstructListRequestOptions,
    PermissionListResponse,
    Raw,
    Response,
    Template,
    TemplateLinkResponse,
    TemplateListRequest,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';
import { stringifyPagingSortOptions } from '../util/sort-options/sort-options';

/**
 * API methods related to the node resource.
 *
 * Docs for the endpoints used here can be found at:
 * https://www.gentics.com/Content.Node/guides/restapi/resource_NodeResource.html
 *
 */
export class NodeApi {

    constructor(
        private apiBase: ApiBase,
    ) { }

    /**
     * List nodes
     */
    getNodes(options?: NodeListRequestOptions): Observable<PermissionListResponse<Node<Raw>>> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        return this.apiBase.get('node', options);
    }

    /**
     * Create a new node. See NodeResourceImpl#checkCreateRequest for a detailed list of constraints on the given request.
     *
     * @see https://gentics.com/infoportal/guides/restapi/resource_NodeResource.html#resource_NodeResource_add_PUT
     */
    addNode(payload: NodeCreateRequest): Observable<NodeResponse> {
        return this.apiBase.put('node', payload);
    }

    /**
     * Get list of available node features
     */
    getNodeFeatureList(options?: NodeFeatureListRequestOptions): Observable<FeatureModelListResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }
        return this.apiBase.get('node/features', options);
    }

    /**
     * Delete the given node
     */
    removeNode(nodeId: number | string, options?: NodeDeleteRequestOptions): Observable<ItemDeleteResponse> {
        return this.apiBase.delete(`node/${nodeId}`, options);
    }

    /**
     * Load a single node
     */
    getNode(nodeId: number, options?: NodeRequestOptions): Observable<NodeResponse> {
        return this.apiBase.get(`node/${nodeId}`, options);
    }

    /**
     * Saves the values specified in the request to the node.
     *
     * @see https://gentics.com/infoportal/guides/restapi/resource_NodeResource.html#resource_NodeResource_update_POST
     */
    updateNode(nodeId: number, payload: NodeSaveRequest): Observable<ItemSaveResponse> {
        return this.apiBase.post(`node/${nodeId}`, payload);
    }

    /**
     * Get list of features activated for the node
     */
    getNodeFeatures(nodeId: number, options?: NodeFeatureListRequestOptions): Observable<FeatureListResponse> {
        return this.apiBase.get(`node/${nodeId}/features`, options);
    }

    /**
     * Get list of languages activated for the node
     */
    getNodeLanguageList(nodeId: number | string, options?: NodeLanguageListRequest): Observable<ListResponse<Language>> {
        return this.apiBase.get(`node/${nodeId}/languages`, options);
    }

    /**
     * Set ordered list of languages
     *
     * @see https://gentics.com/infoportal/guides/restapi/resource_NodeResource.html#resource_NodeResource_setLanguages_POST
     */
    updateNodeLanguages(nodeId: number, payload: Language[]): Observable<NodeLanguagesListResponse> {
        return this.apiBase.post(`node/${nodeId}/languages`, payload);
    }

    /**
     * Get the constructs assigned to this node
     */
    getNodeConstructs(nodeId: number, options?: PagedConstructListRequestOptions): Observable<NodeResponse> {
        return this.apiBase.get(`node/${nodeId}/constructs`, options);
    }

    /**
     * Copy the given node.
     */
    copyNode(nodeId: number, payload: NodeCopyRequest, options?: NodeCopyRequestOptions): Observable<ItemSaveResponse> {
        return this.apiBase.post(`node/${nodeId}/copy`, payload, options);
    }

    /**
     * Load settings specific to the specified node.
     *
     * @see https://gentics.com/infoportal/guides/restapi/resource_NodeResource.html#resource_NodeResource_settings_GET
     */
    getNodeSettings(nodeId: number): Observable<NodeSettingsResponse> {
        return this.apiBase.get(`node/${nodeId}/settings`);
    }

    /**
     * Deactivate the feature for the node
     */
    deactivateNodeFeature(nodeId: number, feature: keyof NodeFeatures): Observable<ItemDeleteResponse> {
        return this.apiBase.delete(`node/${nodeId}/features/${feature}`);
    }

    /**
     * Activate the feature for the node
     */
    activateNodeFeature(nodeId: number, feature: keyof NodeFeatures): Observable<ItemSaveResponse> {
        return this.apiBase.put(`node/${nodeId}/features/${feature}`, undefined);
    }

    /**
     * Remove language from node
     */
    removeNodeLanguage(nodeId: number, languageId: number): Observable<ItemDeleteResponse> {
        return this.apiBase.delete(`node/${nodeId}/languages/${languageId}`);
    }

    /**
     * Add language to node
     */
    addNodeLanguage(nodeId: number, languageId: number): Observable<ItemSaveResponse> {
        return this.apiBase.put(`node/${nodeId}/languages/${languageId}`, undefined);
    }

    /**
     * Remove a construct from a node.
     */
    removeNodeConstruct(nodeId: number, constructId: number): Observable<ItemDeleteResponse> {
        return this.apiBase.delete(`node/${nodeId}/constructs/${constructId}`);
    }

    /**
     * Add a construct to a node.
     */
    addNodeConstruct(nodeId: number, constructId: number): Observable<ItemSaveResponse> {
        return this.apiBase.put(`node/${nodeId}/constructs/${constructId}`, undefined);
    }

    /**
     * Get the templates assigned to this node
     */
    getNodeTemplates(nodeId: number, options?: TemplateListRequest): Observable<PermissionListResponse<Template>> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        return this.apiBase.get(`node/${nodeId}/templates`, options);
    }

    hasNodeTemplate(nodeId: number, templateId: EntityIdType): Observable<Response> {
        return this.apiBase.get(`node/${nodeId}/templates/${templateId}`);
    }

    /**
     * Assign a template to a node
     */
    addNodeTemplate(nodeId: number | string, templateId: EntityIdType): Observable<TemplateLinkResponse> {
        return this.apiBase.put(`node/${nodeId}/templates/${templateId}`, null);
    }

    /**
     * Remove a template from a node.
     *
     * @see https://gentics.com/infoportal/guides/restapi/resource_NodeResource.html#resource_NodeResource_removeTemplate_DELETE
     */
    removeNodeTemplate(nodeId: number | string, templateId: EntityIdType): Observable<ItemDeleteResponse> {
        return this.apiBase.delete(`node/${nodeId}/templates/${templateId}`);
    }

}
