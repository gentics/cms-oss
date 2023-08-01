import {
    ConstructLoadResponse,
    ContentRepositoryFragmentListOptions,
    ContentRepositoryFragmentListResponse,
    ContentRepositoryFragmentResponse,
    ContentRepositoryListOptions,
    ContentRepositoryListResponse,
    ContentRepositoryResponse,
    DataSourceListOptions,
    DataSourceListResponse,
    DataSourceLoadResponse,
    DevToolsConstructListResponse,
    ListResponse,
    ObjectPropertyListOptions,
    ObjectPropertyListResponse,
    ObjectPropertyLoadResponse,
    PackageCreateRequest,
    PackageListOptions,
    PackageListResponse,
    PackageLoadResponse,
    PackageSyncOptions,
    PackageSyncResponse,
    PagedConstructListRequestOptions,
    Response,
    Template,
    TemplateListRequest,
    TemplateResponse,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';
import { stringifyEmbedOptions, stringifyPagingSortOptions } from '../util/sort-options/sort-options';

/**
 * API methods related to the devtools resource.
 *
 * Docs for the endpoints used here can be found at:
 * https://www.gentics.com/Content.Node/guides/restapi/resource_PackageResource.html
 *
 */
export class DevToolsApi {

    constructor(
        private apiBase: ApiBase,
    ) { }

    // SYNC ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get the current status information for the automatic synchronization
     */
    getSyncState(): Observable<PackageSyncResponse> {
        return this.apiBase.get('devtools/sync');
    }

    startSync(): Observable<PackageSyncResponse> {
        return this.apiBase.put('devtools/sync', null);
    }

    /**
     * Stop the sync, if it was started by the current user
     */
    stopSync(): Observable<PackageSyncResponse> {
        return this.apiBase.delete('devtools/sync');
    }

    // PACKAGES ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get list of packages
     */
    getPackages(options?: PackageListOptions): Observable<PackageListResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        return this.apiBase.get('devtools/packages', options);
    }

    /**
     * Get a single package
     */
    getPackage(packageName: string): Observable<PackageLoadResponse> {
        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.get(`devtools/packages/${sanitizedPackageName}`) as unknown as Observable<PackageLoadResponse>;
    }

    /**
     * Add the package with given name
     */
    addPackage(payload: PackageCreateRequest): Observable<void> {
        return this.apiBase.put(`devtools/packages/${payload.name}`, null) as unknown as Observable<void>;
    }

    /**
     * Delete a single package
     */
    removePackage(packageName: string): Observable<void> {
        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.delete(`devtools/packages/${sanitizedPackageName}`);
    }

    // NODE PACKAGES ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get all package assigned to a node
     */
    getPackagesOfNode(nodeId: string | number, options?: PackageListOptions): Observable<PackageListResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        return this.apiBase.get(`devtools/nodes/${nodeId}/packages`, options);
    }

    /**
     * Create a package in a node
     */
    createPackageInNode(nodeId: number, payload: PackageCreateRequest): Observable<void> {
        return this.apiBase.put(`devtools/nodes/${nodeId}/packages`, payload) as unknown as Observable<void>;
    }

    /**
     * Add an existing package to a node
     */
    addPackageToNode(nodeId: number, packageName: string): Observable<void> {
        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.put(`devtools/nodes/${nodeId}/packages/${sanitizedPackageName}`, null) as unknown as Observable<void>;
    }

    /**
     * Remove an existing package from a node
     */
    removePackageFromNode(nodeId: number, packageName: string): Observable<void> {
        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.delete(`devtools/nodes/${nodeId}/packages/${sanitizedPackageName}`);
    }

    /**
     * Trigger synchronization of all objects in the given package to the filesystem
     */
    syncPackageToFilesystem(packageName: string, options?: PackageSyncOptions): Observable<Response> {
        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.put(`devtools/packages/${sanitizedPackageName}/cms2fs`, null, options);
    }

    /**
     * Trigger synchronization of all objects in the given package to the cms
     */
    syncPackageFromFilesystem(packageName: string, options?: PackageSyncOptions): Observable<Response> {
        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.put(`devtools/packages/${sanitizedPackageName}/fs2cms`, null, options);
    }

    // CONSTRUCTS ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get list of constructs
     */
    getConstructs(packageName: string, options?: PagedConstructListRequestOptions): Observable<DevToolsConstructListResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }
        stringifyEmbedOptions(options);

        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.get(`devtools/packages/${sanitizedPackageName}/constructs`, options);
    }

    /**
     * Get a single construct
     */
    getConstruct(packageName: string, constructGlobalId: string | number): Observable<ConstructLoadResponse> {
        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.get(`devtools/packages/${sanitizedPackageName}/constructs/${constructGlobalId}`);
    }

    /**
     * Add an existing construct to a package
     */
    addConstructToPackage(packageName: string, constructGlobalId: string | number): Observable<void> {
        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.put(`devtools/packages/${sanitizedPackageName}/constructs/${constructGlobalId}`, null) as unknown as Observable<void>;
    }

    /**
     * Remove an existing construct from a package
     */
    removeConstructFromPackage(packageName: string, constructGlobalId: string | number): Observable<void> {
        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.delete(`devtools/packages/${sanitizedPackageName}/constructs/${constructGlobalId}`);
    }

    // CONTENTREPOSITORIES ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get list of contentrepositories
     */
    getContentrepositories(packageName: string, options?: ContentRepositoryListOptions): Observable<ContentRepositoryListResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.get(`devtools/packages/${sanitizedPackageName}/contentrepositories`, options);
    }

    /**
     * Get a single contentRepository
     */
    getContentRepository(packageName: string, contentRepositoryGlobalId: string | number): Observable<ContentRepositoryResponse> {
        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.get(`devtools/packages/${sanitizedPackageName}/contentrepositories/${contentRepositoryGlobalId}`);
    }

    /**
     * Add an existing contentRepository to a package
     */
    addContentRepositoryToPackage(packageName: string, contentRepositoryGlobalId: string | number): Observable<void> {
        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.put(
            `devtools/packages/${sanitizedPackageName}/contentrepositories/${contentRepositoryGlobalId}`,
            null,
        ) as unknown as Observable<void>;
    }

    /**
     * Remove an existing contentRepository from a package
     */
    removeContentRepositoryFromPackage(packageName: string, contentRepositoryGlobalId: string | number): Observable<void> {
        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.delete(`devtools/packages/${sanitizedPackageName}/contentrepositories/${contentRepositoryGlobalId}`);
    }

    // CR_FRAGMENTS ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get list of cr_fragments
     */
    getContentRepositoryFragments(packageName: string, options?: ContentRepositoryFragmentListOptions): Observable<ContentRepositoryFragmentListResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.get(`devtools/packages/${sanitizedPackageName}/cr_fragments`, options);
    }

    /**
     * Get a single cr_fragment
     */
    getContentRepositoryFragment(packageName: string, crfragmentGlobalId: string | number): Observable<ContentRepositoryFragmentResponse> {
        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.get(`devtools/packages/${sanitizedPackageName}/cr_fragments/${crfragmentGlobalId}`);
    }

    /**
     * Add an existing cr_fragment to a package
     */
    addContentRepositoryFragmentToPackage(packageName: string, crfragmentGlobalId: string | number): Observable<void> {
        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.put(`devtools/packages/${sanitizedPackageName}/cr_fragments/${crfragmentGlobalId}`, null) as unknown as Observable<void>;
    }

    /**
     * Remove an existing cr_fragment from a package
     */
    removeContentRepositoryFragmentFromPackage(packageName: string, crfragmentGlobalId: string | number): Observable<void> {
        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.delete(`devtools/packages/${sanitizedPackageName}/cr_fragments/${crfragmentGlobalId}`);
    }

    // DATASOURCE ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get list of dataSources
     */
    getDataSources(packageName: string, options?: DataSourceListOptions): Observable<DataSourceListResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.get(`devtools/packages/${sanitizedPackageName}/datasources`, options);
    }

    /**
     * Get a single dataSource
     */
    getDataSource(packageName: string, dataSourceGlobalId: string | number): Observable<DataSourceLoadResponse> {
        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.get(`devtools/packages/${sanitizedPackageName}/datasources/${dataSourceGlobalId}`);
    }

    /**
     * Add an existing dataSource to a package
     */
    addDataSourceToPackage(packageName: string, dataSourceGlobalId: string | number): Observable<void> {
        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.put(`devtools/packages/${sanitizedPackageName}/datasources/${dataSourceGlobalId}`, null) as unknown as Observable<void>;
    }

    /**
     * Remove an existing dataSource from a package
     */
    removeDataSourceFromPackage(packageName: string, dataSourceId: string | number): Observable<void> {
        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.delete(`devtools/packages/${sanitizedPackageName}/datasources/${dataSourceId}`);
    }

    // OBJECTPROPERTY ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get list of objectproperties
     */
    getObjectproperties(packageName: string, options?: ObjectPropertyListOptions): Observable<ObjectPropertyListResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }
        stringifyEmbedOptions(options);

        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.get(`devtools/packages/${sanitizedPackageName}/objectproperties`, options);
    }

    /**
     * Get a single objectProperty
     */
    getObjectProperty(packageName: string, objectPropertyGlobalId: string | number): Observable<ObjectPropertyLoadResponse> {
        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.get(`devtools/packages/${sanitizedPackageName}/objectproperties/${objectPropertyGlobalId}`);
    }

    /**
     * Add an existing objectProperty to a package
     */
    addObjectPropertyToPackage(packageName: string, objectPropertyGlobalId: string | number): Observable<void> {
        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.put(`devtools/packages/${sanitizedPackageName}/objectproperties/${objectPropertyGlobalId}`, null) as unknown as Observable<void>;
    }

    /**
     * Remove an existing objectProperty from a package
     */
    removeObjectPropertyFromPackage(packageName: string, objectPropertyGlobalId: string | number): Observable<void> {
        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.delete(`devtools/packages/${sanitizedPackageName}/objectproperties/${objectPropertyGlobalId}`);
    }

    // TEMPLATES ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get list of templates
     */
    getTemplates(packageName: string, options?: TemplateListRequest): Observable<ListResponse<Template>> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.get(`devtools/packages/${sanitizedPackageName}/templates`, options);
    }

    /**
     * Get a single template
     */
    getTemplate(packageName: string, templateGlobalId: string | number): Observable<TemplateResponse> {
        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.get(`devtools/packages/${sanitizedPackageName}/templates/${templateGlobalId}`);
    }

    /**
     * Add an existing template to a package
     */
    addTemplateToPackage(packageName: string, templateGlobalId: string | number): Observable<void> {
        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.put(`devtools/packages/${sanitizedPackageName}/templates/${templateGlobalId}`, null) as unknown as Observable<void>;
    }

    /**
     * Remove an existing template from a package
     */
    removeTemplateFromPackage(packageName: string, templateGlobalId: string | number): Observable<void> {
        const sanitizedPackageName = this.sanitizePackageNameString(packageName);
        return this.apiBase.delete(`devtools/packages/${sanitizedPackageName}/templates/${templateGlobalId}`);
    }

    /**
     * Sanitizes Package name as it can contain spaces.
     */
    private sanitizePackageNameString(packageName: string): string {
        return encodeURI(packageName);
    }

}
