import {
    Form,
    FormDataListResponse,
    FormListRequest,
    FormListResponse,
    FormReportsRequest,
    FormResponse,
    FormSaveRequest,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';

/**
 * API methods related to the form resource.
 *
 * Docs for the endpoints used here can be found at:
 * https://www.gentics.com/Content.Node/guides/restapi/resource_FormResource.html
 */
export class FormApi {

    constructor(
        private apiBase: ApiBase,
    ) { }

    /**
     * Get a list of forms.
     */
    getForms(options?: FormListRequest): Observable<FormListResponse> {
        return this.apiBase.get('form', options);
    }

    /**
     * Permanently delete a single data entry from Mesh, which has been submitted to the form.
     */
    deleteReport(formId: number, dataId: string): Observable<void> {
        return this.apiBase.delete(`form/${formId}/data/${dataId}`);
    }

    /**
     * Get a list of data entries from Mesh, which have been submitted to the form.
     */
    getReports(id: number, options?: FormReportsRequest): Observable<FormDataListResponse> {
        return this.apiBase.get(`form/${id}/data`, options) as any as Observable<FormDataListResponse>;
    }

    /**
     * Get a single form by id.
     */
    getForm(id: number): Observable<FormResponse> {
        return this.apiBase.get(`form/${id}`);
    }

    /**
     * Update a single form by id.
     */
    updateForm(id: number, payload: Partial<Form>): Observable<FormResponse> {
        const dto = this.sanitizeFormSaveRequest(payload);
        return this.apiBase.put(`form/${id}`, payload);
    }

    /**
     * Delete a single form by id.
     */
    deleteForm(id: number): Observable<void> {
        return this.apiBase.delete(`form/${id}`);
    }

    /**
     * Publish a single form by id.
     */
    publishForm(id: number): Observable<FormResponse> {
        return this.apiBase.put(`form/${id}`, null);
    }

    /**
     * Unpublish a single form by id.
     */
    unpublishForm(id: number): Observable<FormResponse> {
        return this.apiBase.delete(`form/${id}`);
    }

    private sanitizeFormSaveRequest(payload: Partial<Form>): FormSaveRequest {
        // read-only properties not meant to send in DTO
        const readOnlyProperties = [
            'creator',
            'currentVersion',
            'deleted',
            'edate',
            'editor',
            'folder',
            'folderDeleted',
            'locked',
            'lockedBy',
            'lockedSince',
            'master',
            'masterDeleted',
            'modified',
            'online',
            'published',
            'publishedVersion',
            'publisher',
            'usage',
            'versions',
        ];
        readOnlyProperties.forEach(k => {
            if (payload[k]) {
                delete payload[k];
            }
        });

        return payload;
    }


}
