import {
    AssignEntityToContentPackageOptions,
    ContentPackageCreateRequest,
    ContentPackageErrorResponse,
    ContentPackageListOptions,
    ContentPackageListResponse,
    ContentPackageResponse,
    ContentPackageSaveRequest,
    ContentPackageSyncOptions,
    ContentPackageSyncResponse,
    Response,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiBase } from '../base/api-base.service';
import { stringifyPagingSortOptions } from '../util/sort-options/sort-options';

type AssignableEntityType = 'file' | 'folder' | 'form' | 'image' | 'page';

export class ContentStagingApi {

    constructor(
        private apiBase: ApiBase,
    ) { }

    listContentPackages(options?: ContentPackageListOptions): Observable<ContentPackageListResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        return this.apiBase.get('content/package', options);
    }

    createContentPackage(body: ContentPackageCreateRequest): Observable<ContentPackageResponse> {
        return this.apiBase.put('content/package', body);
    }

    getContentPackage(name: string): Observable<ContentPackageResponse> {
        return this.apiBase.get(`content/package/${name}`);
    }

    updateContentPackage(name: string, body: ContentPackageSaveRequest): Observable<ContentPackageResponse> {
        return this.apiBase.post(`content/package/${name}`, body);
    }

    deleteContentPackage(name: string): Observable<Response> {
        return this.apiBase.delete(`content/package/${name}`);
    }

    exportContentPackage(name: string, options?: ContentPackageSyncOptions): Observable<ContentPackageSyncResponse> {
        return this.apiBase.post(`content/package/${name}/export`, null, options);
    }

    importContentPackage(name: string, options?: ContentPackageSyncOptions): Observable<ContentPackageSyncResponse> {
        return this.apiBase.post(`content/package/${name}/import`, null, options);
    }

    getImportErrors(name: string): Observable<ContentPackageErrorResponse> {
        return this.apiBase.get(`content/package/${name}/import/errors`, {});
    }

    uploadContentPackage(name: string, file: File): Observable<ContentPackageResponse> {
        return this.apiBase.upload([file], `content/package/${name}/zip`, {
            fileField: 'fileBinaryData',
        }).done$.pipe(
            // The response here is actually correct
            map(res => res[0].response as any),
        );
    }

    downloadContentPackage(name: string): Observable<Blob> {
        return this.apiBase.getBlob(`content/package/${name}/zip`);
    }

    assignEntityToContentPackage(
        name: string,
        entityType: AssignableEntityType,
        entityGlobalId: string,
        options?: AssignEntityToContentPackageOptions,
    ): Observable<ContentPackageResponse> {
        return this.apiBase.put(`content/package/${name}/${entityType}/${entityGlobalId}`, {}, options);
    }

    removeEntityFromContentPackage(
        name: string,
        entityType: AssignableEntityType,
        entityGlobalId: string,
    ): Observable<ContentPackageResponse> {
        return this.apiBase.delete(`content/package/${name}/${entityType}/${entityGlobalId}`);
    }
}
