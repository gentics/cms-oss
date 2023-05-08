import {
    EntityIdType,
    PagedTemplateLinkListResponse,
    PagedTemplateListResponse,
    Response,
    TagStatusOptions,
    TemplateCreateRequest,
    TemplateLinkListOptions,
    TemplateLinkRequestOptions,
    TemplateLinkResponse,
    TemplateListRequest,
    TemplateMultiLinkRequest,
    TemplateRequestOptions,
    TemplateResponse,
    TemplateSaveRequest,
    TemplateSaveResponse,
    TemplateTagsRequestOptions,
    TemplateTagsResponse,
    TemplateTagStatusResponse,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';
import { stringifyPagingSortOptions } from '../util/sort-options/sort-options';

/**
 * API methods related to the template resource.
 *
 * Docs for the endpoints used here can be found at:
 * https://www.gentics.com/Content.Node/guides/restapi/resource_TemplateResource.html
 */
export class TemplateApi {

    constructor(
        private apiBase: ApiBase,
    ) { }

    /**
     * Create a new Template
     */
    createTemplate(request: TemplateCreateRequest): Observable<TemplateResponse> {
        return this.apiBase.post('template', request);
    }

    /**
     * Get a list of templates.
     */
    getTemplates(options?: TemplateListRequest): Observable<PagedTemplateListResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        return this.apiBase.get('template', options);
    }

    /**
     * Get a single template by id.
     */
    getTemplate(templateId: EntityIdType, options?: TemplateRequestOptions): Observable<TemplateResponse> {
        return this.apiBase.get(`template/${templateId}`, options);
    }

    /**
     * Update a single template by id.
     */
    updateTemplate(templateId: EntityIdType, body: TemplateSaveRequest): Observable<TemplateSaveResponse> {
        return this.apiBase.post(`template/${templateId}`, body);
    }

    /**
     * Gets the a page of all currently linked folders from a specified template.
     */
    getLinkedFolders(templateId: EntityIdType, options?: TemplateLinkListOptions): Observable<PagedTemplateLinkListResponse> {
        return this.apiBase.get(`template/${templateId}/folders`, options);
    }

    /**
     * Links a template to folders
     *
     * @param templateId to be linked to folders listed in options
     * @param options options defining nodeId associated with templateId and list of folderIds
     */
    linkTemplateToFolders(templateId: EntityIdType, options: TemplateLinkRequestOptions): Observable<TemplateLinkResponse> {
        return this.apiBase.post(`template/link/${templateId}`, options);
    }

    /**
     * Links multiple templates to multiple folders in one request.
     * @param request Request body which contains the info which templates and folders should be linked.
     * @returns A generic response
     */
    linkTemplatesToFolders(request: TemplateMultiLinkRequest): Observable<Response> {
        return this.apiBase.post('template/link', request);
    }

    /**
     * Unlinks a template from folders
     *
     * @param templateId to be unlinked from folders listed in options
     * @param options options defining nodeId associated with templateId and list of folderIds
     */
    unlinkTemplateFromFolders(templateId: EntityIdType, options: TemplateLinkRequestOptions): Observable<TemplateLinkResponse> {
        return this.apiBase.post(`template/unlink/${templateId}`, options);
    }

    /**
     * Unlinks multiple templates from multiple folders in one request.
     * @param request Request body which contains the info which templates and folders should be unlinked.
     * @returns A generic response
     */
    unlinkTemplatesFromFolders(request: TemplateMultiLinkRequest): Observable<Response> {
        return this.apiBase.post('template/unlink', request);
    }

    /**
     * Loads the Template-Tags of the specified template
     */
    getTemplateTags(templateId: EntityIdType, options?: TemplateTagsRequestOptions): Observable<TemplateTagsResponse> {
        return this.apiBase.get(`template/${templateId}/tag`, options);
    }

    /**
     * Loads the status of all template-tags of the specified template.
     */
    getTemplateTagStatus(templateId: EntityIdType, options?: TagStatusOptions): Observable<TemplateTagStatusResponse> {
        return this.apiBase.get(`template/${templateId}/tagstatus`, options);
    }

    /**
     * Deletes the specified template
     */
    deleteTemplate(templateId: EntityIdType): Observable<void> {
        return this.apiBase.delete(`template/${templateId}`);
    }

    /**
     * Unlocks a template to make it editable for other users again.
     */
    unlock(templateId: EntityIdType): Observable<Response> {
        return this.apiBase.post(`template/${templateId}/unlock`, null);
    }
}
