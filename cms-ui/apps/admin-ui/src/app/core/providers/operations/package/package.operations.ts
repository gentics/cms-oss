import { discard } from '@admin-ui/common';
import { ActivityManagerService } from '@admin-ui/core/providers/activity-manager/activity-manager.service';
import { AppStateService } from '@admin-ui/state';
import { Injectable, Injector } from '@angular/core';
import {
    ConstructLoadResponse,
    ContentRepository,
    ContentRepositoryBO, ContentRepositoryListOptions,
    ContentRepositoryListResponse,
    ContentRepositoryResponse,
    DataSource,
    DataSourceBO,
    DataSourceListOptions,
    DataSourceListResponse,
    DataSourceLoadResponse,
    DevToolsConstructListResponse,
    NormalizableEntityType,
    ObjectProperty,
    ObjectPropertyBO,
    ObjectPropertyListOptions,
    ObjectPropertyListResponse,
    ObjectPropertyLoadResponse,
    PackageBO,
    PackageCreateRequest,
    PackageListOptions,
    PackageListResponse,
    PackageLoadResponse,
    PackageSyncOptions,
    PagedConstructListRequestOptions,
    Raw,
    TagType,
    TagTypeBO,
    Template,
    TemplateBO,
    TemplateFolderListRequest,
    TemplateResponse
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { forkJoin, Observable, throwError } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { I18nNotificationService } from '../../i18n-notification';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';
import { PackageEntitiesManagerService } from './package-entities-manager.service';

@Injectable()
export class PackageOperations extends ExtendedEntityOperationsBase<'package'> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private appState: AppStateService,
        private entityManager: EntityManagerService,
        private notification: I18nNotificationService,
        private packageEntitiesManager: PackageEntitiesManagerService,
        private activityManager: ActivityManagerService,
    ) {
        super(injector, 'package');
    }

    // SYNC ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get the current status information for the automatic synchronization
     */
    getSyncState(): Observable<{
        enabled: boolean;
    }> {
        return this.api.devTools.getSyncState();
    }

    /**
     * Stop the sync, if it was started by the current user
     */
    stopSync(): Observable<{
        enabled: boolean;
    }> {
        return this.api.devTools.stopSync();
    }

    // PACKAGES ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get a list of all packages and adds them to the AppState.
     *
     * **Important:** A large list of entities is added to the AppState in batches.
     * Thus the AppState may not yet contain all loaded packages when the returned observable emits.
     */
    getAll(options?: PackageListOptions): Observable<PackageBO<Raw>[]> {
        return this.api.devTools.getPackages(options).pipe(
            map((res: PackageListResponse) => {
                // fake entity's `id` property to enforce internal application entity uniformity
                return res.items.map(item => Object.assign(item, { id: item.name }) as PackageBO<Raw>);
            }),
            tap((items: PackageBO<Raw>[]) => {
                // clear all existing entities
                this.entityManager.deleteAllEntitiesInBranch('package');
                // update state with server response
                this.entityManager.addEntities('package', items);
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get a single package and add it to the AppState.
     */
    get(packageName: string): Observable<PackageBO<Raw>> {
        return this.api.devTools.getPackage(packageName).pipe(
            // due to inconsistent REST API no unpackaging is required
            map((res: PackageLoadResponse) => Object.assign(res, { id: res.name }) as PackageBO<Raw>),
            // update state with server response
            tap((item: PackageBO<Raw>) => this.entityManager.addEntity('package', item)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Add the package with given name.
     */
    addPackage(payload: PackageCreateRequest): Observable<void> {
        return this.api.devTools.addPackage(payload).pipe(
            tap(() => {
                // fake entity's `id` property to enforce internal application entity uniformity
                const newPackage: PackageBO<Raw> = { id: payload.name, name: payload.name };
                this.entityManager.addEntity('package', newPackage);
            }),
            // display toast notification
            tap(() => this.notification.show({
                type: 'success',
                message: 'shared.item_created',
                translationParams: { name: payload.name },
            })),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Delete package(s).
     */
    removePackage(packageName: string | string[]): Observable<void> {
        const request = (name: string): Observable<void> => {
            return this.api.devTools.removePackage(name);
        };
        let stream: Observable<void>;
        if (Array.isArray(packageName) && packageName.length > 0) {
            stream = forkJoin(packageName.map(name => request(name))).pipe(
                map(() => { return; }),
                tap(() => this.entityManager.deleteEntities('package', packageName)),
                tap(() => {
                    return this.notification.show({
                        type: 'success',
                        message: 'shared.item_plural_deleted',
                        translationParams: { name: packageName },
                    });
                }),
            );
        }
        if (typeof packageName === 'string') {
            stream = request(packageName).pipe(
                tap(() => this.entityManager.deleteEntities('package', [packageName])),
                tap(() => {
                    return this.notification.show({
                        type: 'success',
                        message: 'shared.item_singular_deleted',
                        translationParams: { name: packageName },
                    });
                }),
            );
        }
        return stream.pipe(
            this.catchAndRethrowError(),
        );
    }

    /**
     * Trigger synchronization of all objects in the given package to the filesystem.
     */
    async syncPackageToFilesystem(packageNames: string | string[], options?: PackageSyncOptions): Promise<void> {
        if (typeof packageNames === 'string') {
            packageNames = [packageNames];
        } else if (!Array.isArray(packageNames)) {
            return;
        }

        try {
            for (const pkgName of packageNames) {
                const operation = this.api.devTools.syncPackageToFilesystem(pkgName, options).pipe(
                    tap(() => {
                        this.notification.show({
                            type: 'success',
                            message: 'package.package_successfully_synced_to_filesystem',
                            translationParams: {
                                name: pkgName,
                            },
                        });
                    }),
                );

                await new Promise<string>((resolve, reject) => {
                    // configure and add activity to activity manager
                    this.activityManager.activityAdd(
                        'package.package_syncing_cms_to_filesystem',
                        operation,
                        true,
                        true,
                        {
                            labelOnSuccess: {
                                label: 'shared.package_synced_cms_to_filesystem',
                                params: { name: 'common.package_singular' },
                                translateParams: true,
                            },
                            callBackOnSuccess: (successMessage: string) => resolve(successMessage),
                            callBackOnFailed: (errorMessage: string) => reject(errorMessage),
                        },
                    );
                });
            }
        } catch (error) {
            this.errorHandler.notifyAndRethrow(error);
        } finally {
            await this.getAll().toPromise();
        }
    }

    /**
     * Trigger synchronization of all objects in the given package to the filesystem.
     */
    async syncPackageFromFilesystem(packageNames: string | string[], options?: PackageSyncOptions): Promise<void> {
        if (typeof packageNames === 'string') {
            packageNames = [packageNames];
        } else if (!Array.isArray(packageNames)) {
            return;
        }

        try {
            for (const pkgName of packageNames) {
                const operation = this.api.devTools.syncPackageFromFilesystem(pkgName, options).pipe(
                    tap(() => {
                        this.notification.show({
                            type: 'success',
                            message: 'package.package_successfully_synced_from_filesystem',
                            translationParams: {
                                name: pkgName,
                            },
                        });
                    }),
                );

                await new Promise<string>((resolve, reject) => {
                    // configure and add activity to activity manager
                    this.activityManager.activityAdd(
                        'package.package_syncing_filesystem_to_cms',
                        operation,
                        true,
                        true,
                        {
                            labelOnSuccess: {
                                label: 'shared.package_synced_filesystem_to_cms',
                                params: { name: 'common.package_singular' },
                                translateParams: true,
                            },
                            callBackOnSuccess: (successMessage: string) => resolve(successMessage),
                            callBackOnFailed: (errorMessage: string) => reject(errorMessage),
                        },
                    );
                });
            }
        } catch (error) {
            this.errorHandler.notifyAndRethrow(error);
        } finally {
            await this.getAll().toPromise();
        }
    }

    removeEntitiesFromPackage(
        packageName: string,
        entityIdentifier: 'construct' | 'contentRepository' | 'contentRepositoryFragment' | 'dataSource' | 'objectProperty' | 'template',
        ids: string[],
    ): Observable<void> {
        let operation: Observable<any>;

        switch (entityIdentifier) {
            case 'construct':
                operation = this.removeConstructFromPackage(packageName, ids);
                break;

            case 'contentRepository':
                operation = this.removeContentRepositoryFromPackage(packageName, ids);
                break;

            case 'contentRepositoryFragment':
                operation = this.removeContentRepositoryFragmentFromPackage(packageName, ids);
                break;

            case 'dataSource':
                operation = this.removeDataSourcesFromPackage(packageName, ids);
                break;

            case 'objectProperty':
                operation = this.removeObjectPropertyFromPackage(packageName, ids);
                break;

            case 'template':
                operation = this.removeTemplateFromPackage(packageName, ids);
                break;

            default:
                // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
                return throwError(new Error(`No operation to remove entity-type ${entityIdentifier} from package!`));
        }

        return operation.pipe(discard());
    }

    // NODE PACKAGES ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get all package assigned to a node
     */
    getPackagesOfNode(nodeId: number, options?: PackageListOptions): Observable<PackageBO<Raw>[]> {
        return this.api.devTools.getPackagesOfNode(nodeId, options).pipe(
            map((res: PackageListResponse) => {
                // fake entity's `id` property to enforce internal application entity uniformity
                return res.items.map(item => Object.assign(item, { id: item.name }) as PackageBO<Raw>);
            }),
            tap((items: PackageBO<Raw>[]) => this.entityManager.addEntities('package', items)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Create a package in a node
     */
    createPackageInNode(nodeId: number, payload: PackageCreateRequest): Observable<PackageBO<Raw>> {
        return this.api.devTools.createPackageInNode(nodeId, payload).pipe(
            // due to inconsistent REST API no unpackaging is required
            map(() => Object.assign(payload, { id: payload.name }) as PackageBO<Raw>),
            // update state with server response
            tap((cmsPackage: PackageBO<Raw>) => this.entityManager.addEntity('package', cmsPackage)),
            // display toast notification
            tap(() => this.notification.show({
                type: 'success',
                message: 'shared.item_created',
                translationParams: { name: name },
            })),
            this.catchAndRethrowError(),
        );
    }

    changePackagesOfNode(
        nodeId: number,
        packageIds: string[],
        preselected: string[],
    ): Observable<boolean> {

        // calculate minimal amount of requests required
        const entitiesShallBeLinked = packageIds.filter(id => !preselected.includes(id));
        const entitiesShallNotBeLinked = preselected.filter(id => !packageIds.includes(id));

        return forkJoin([
            entitiesShallBeLinked.length > 0
                ? this.addPackageToNode(nodeId, entitiesShallBeLinked).pipe(map(() => true))
                : Promise.resolve(false),
            entitiesShallNotBeLinked.length > 0
                ? this.removePackageFromNode(nodeId, entitiesShallNotBeLinked).pipe(map(() => true))
                : Promise.resolve(false),
        ]).pipe(
            map(([didLink, didUnlink]) => didLink || didUnlink),
        );
    }

    /**
     * Add an existing package to a node
     */
    addPackageToNode(nodeId: number, packageName: string | string[]): Observable<void> {
        const request = (id: number, name: string): Observable<void> => {
            return this.api.devTools.addPackageToNode(id, name).pipe(
                // display toast notification
                tap(() => this.notification.show({
                    type: 'success',
                    message: 'package.package_successfully_added_to_node',
                    translationParams: { name },
                })),
            );
        };
        let stream: Observable<void>;
        if (Array.isArray(packageName) && packageName.length > 0) {
            stream = forkJoin(packageName.map(name => request(nodeId, name))).pipe(
                map(() => { return; }),
            );
        }
        if (typeof packageName === 'string') {
            stream = request(nodeId, packageName);
        }
        return stream.pipe(
            this.catchAndRethrowError(),
        );
    }

    /**
     * Remove (an) existing package(s) from a node
     */
    removePackageFromNode(nodeId: number, packageName: string | string[]): Observable<void> {
        const request = (id: number, name: string): Observable<void> => {
            return this.api.devTools.removePackageFromNode(id, name).pipe(
                // display toast notification
                tap(() => this.notification.show({
                    type: 'success',
                    message: 'package.package_successfully_removed_from_node',
                    translationParams: { name },
                })),
            );
        };
        let stream: Observable<void>;
        if (Array.isArray(packageName) && packageName.length > 0) {
            stream = forkJoin(packageName.map(name => request(nodeId, name))).pipe(
                map(() => { return; }),
            );
        }
        if (typeof packageName === 'string') {
            stream = request(nodeId, packageName);
        }
        return stream.pipe(
            this.catchAndRethrowError(),
        );
    }

    // GENERIC ENTITIES ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    removeFromPackage(entityType: NormalizableEntityType, packageId: string, entityIds: string | string[]): Observable<void> {
        switch (entityType) {
            case 'construct':
                return this.removeConstructFromPackage(packageId, entityIds);

            case 'contentRepository':
                return this.removeContentRepositoryFromPackage(packageId, entityIds);

            case 'contentRepositoryFragment':
                return this.removeContentRepositoryFragmentFromPackage(packageId, entityIds);

            case 'objectProperty':
                return this.removeObjectPropertyFromPackage(packageId, entityIds);

            case 'template':
                return this.removeTemplateFromPackage(packageId, entityIds);

            default:
                throwError(new Error(`Unknown type ${entityType}`));
        }
    }

    // CONSTRUCTS ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get list of constructs
     */
    getConstructs(packageName: string, options?: PagedConstructListRequestOptions): Observable<TagTypeBO<Raw>[]> {
        return this.api.devTools.getConstructs(packageName, options).pipe(
            map((res: DevToolsConstructListResponse) => {
                // fake entity's `id` property to enforce internal application entity uniformity
                return res.items.map(item => Object.assign(item, { id: item.keyword }) as TagTypeBO<Raw>);
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get a single construct
     */
    getConstruct(packageName: string, keyword: string): Observable<TagTypeBO<Raw>> {
        return this.api.devTools.getConstruct(packageName, keyword).pipe(
            map((res: ConstructLoadResponse) => res.construct),
            // fake entity's `id` property to enforce internal application entity uniformity
            map((item: TagType<Raw>) => Object.assign(item, { id: item.globalId })),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Change constructs of a package
     */
    changeConstructOfPackage(packageName: string, keyword: string[], preselected: string[]): Observable<void> {
        const addFn = (a: string, b: string) => {
            return this.api.devTools.addConstructToPackage(a, b);
        };
        const removeFn = (a: string, b: string) => {
            return this.api.devTools.removeConstructFromPackage(a, b);
        };
        const refreshFn = (p: string) => this.get(packageName);
        return this.packageEntitiesManager.changeEntitiesOfPackage(
            packageName,
            'construct',
            keyword,
            preselected,
            addFn.bind(this),
            'construct.construct_successfully_added_to_package',
            removeFn.bind(this),
            'construct.construct_successfully_removed_from_package',
            refreshFn.bind(this),
        );
    }

    /**
     * Add an existing construct to a package
     */
    addConstructToPackage(packageName: string, keyword: string | string[], preselected?: string[]): Observable<void> {
        const addFn = (a: string, b: string) => {
            return this.api.devTools.addConstructToPackage(a, b);
        };
        const refreshFn = (p: string) => this.get(packageName);
        return this.packageEntitiesManager.addEntityToPackage(
            packageName,
            'construct',
            keyword,
            addFn.bind(this),
            'construct.construct_successfully_added_to_package',
            refreshFn.bind(this),
        );
    }

    /**
     * Remove an existing construct from a package
     */
    removeConstructFromPackage(packageName: string, keyword: string | string[]): Observable<void> {
        const removeFn = (a: string, b: string) => {
            return this.api.devTools.removeConstructFromPackage(a, b);
        };
        const refreshFn = (p: string) => this.get(packageName);
        return this.packageEntitiesManager.removeEntityFromPackage(
            packageName,
            'construct',
            keyword,
            removeFn.bind(this),
            'construct.construct_successfully_removed_from_package',
            refreshFn.bind(this),
        );
    }

    // CONTENTREPOSITORIES ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get list of contentrepositories
     */
    getContentrepositories(packageName: string, options?: ContentRepositoryListOptions): Observable<ContentRepositoryBO<Raw>[]> {
        return this.api.devTools.getContentrepositories(packageName, options).pipe(
            map((res: ContentRepositoryListResponse) => {
                // fake entity's `id` property to enforce internal application entity uniformity
                return res.items.map(item => Object.assign(item, { id: item.globalId }) as ContentRepositoryBO<Raw>);
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get a single contentRepository
     */
    getContentRepository(packageName: string, globalId: string): Observable<ContentRepositoryBO<Raw>> {
        return this.api.devTools.getContentRepository(packageName, globalId).pipe(
            map((res: ContentRepositoryResponse) => res.contentRepository),
            // fake entity's `id` property to enforce internal application entity uniformity
            map((item: ContentRepository<Raw>) => Object.assign(item, { id: item.globalId })),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Change contentrepositories of a package
     */
    changeContentRepositoryOfPackage(packageName: string, globalId: string[], preselected: string[]): Observable<void> {
        const addFn = (a: string, b: string) => {
            return this.api.devTools.addContentRepositoryToPackage(a, b);
        };
        const removeFn = (a: string, b: string) => {
            return this.api.devTools.removeContentRepositoryFromPackage(a, b);
        };
        const refreshFn = (p: string) => this.get(packageName);
        return this.packageEntitiesManager.changeEntitiesOfPackage(
            packageName,
            'contentRepository',
            globalId,
            preselected,
            addFn.bind(this),
            'contentRepository.contentRepository_successfully_added_to_package',
            removeFn.bind(this),
            'contentRepository.contentRepository_successfully_removed_from_package',
            refreshFn.bind(this),
        );
    }

    /**
     * Add an existing contentRepository to a package
     */
    addContentRepositoryToPackage(packageName: string, globalId: string | string[], preselected?: string[]): Observable<void> {
        const addFn = (a: string, b: string) => {
            return this.api.devTools.addContentRepositoryToPackage(a, b);
        };
        const refreshFn = (p: string) => this.get(packageName);
        return this.packageEntitiesManager.addEntityToPackage(
            packageName,
            'contentRepository',
            globalId,
            addFn.bind(this),
            'contentRepository.contentRepository_successfully_added_to_package',
            refreshFn.bind(this),
        );
    }

    /**
     * Remove an existing contentRepository from a package
     */
    removeContentRepositoryFromPackage(packageName: string, globalId: string | string[]): Observable<void> {
        const removeFn = (a: string, b: string) => {
            return this.api.devTools.removeContentRepositoryFromPackage(a, b);
        };
        const refreshFn = (p: string) => this.get(packageName);
        return this.packageEntitiesManager.removeEntityFromPackage(
            packageName,
            'contentRepository',
            globalId,
            removeFn.bind(this),
            'contentRepository.contentRepository_successfully_removed_from_package',
            refreshFn.bind(this),
        );
    }

    // CR_FRAGMENTS ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Change cr_fragments of a package
     */
    changeContentRepositoryFragmentOfPackage(packageName: string, globalId: string[], preselected: string[]): Observable<void> {
        const addFn = (a: string, b: string) => {
            return this.api.devTools.addContentRepositoryFragmentToPackage(a, b);
        };
        const removeFn = (a: string, b: string) => {
            return this.api.devTools.removeContentRepositoryFragmentFromPackage(a, b);
        };
        const refreshFn = (p: string) => this.get(packageName);
        return this.packageEntitiesManager.changeEntitiesOfPackage(
            packageName,
            'contentRepositoryFragment',
            globalId,
            preselected,
            addFn.bind(this),
            'contentRepositoryFragment.contentRepositoryFragment_successfully_added_to_package',
            removeFn.bind(this),
            'contentRepositoryFragment.contentRepositoryFragment_successfully_removed_from_package',
            refreshFn.bind(this),
        );
    }

    /**
     * Add an existing cr_fragment to a package
     */
    addContentRepositoryFragmentToPackage(packageName: string, globalId: string | string[], preselected?: string[]): Observable<void> {
        const addFn = (a: string, b: string) => {
            return this.api.devTools.addContentRepositoryFragmentToPackage(a, b);
        };
        const refreshFn = (p: string) => this.get(packageName);
        return this.packageEntitiesManager.addEntityToPackage(
            packageName,
            'contentRepositoryFragment',
            globalId,
            addFn.bind(this),
            'contentRepositoryFragment.contentRepositoryFragment_successfully_added_to_package',
            refreshFn.bind(this),
        );
    }

    /**
     * Remove an existing cr_fragment from a package
     */
    removeContentRepositoryFragmentFromPackage(packageName: string, globalId: string | string[]): Observable<void> {
        const removeFn = (a: string, b: string) => {
            return this.api.devTools.removeContentRepositoryFragmentFromPackage(a, b);
        };
        const refreshFn = (p: string) => this.get(packageName);
        return this.packageEntitiesManager.removeEntityFromPackage(
            packageName,
            'contentRepositoryFragment',
            globalId,
            removeFn.bind(this),
            'contentRepositoryFragment.contentRepositoryFragment_successfully_removed_from_package',
            refreshFn.bind(this),
        );
    }

    // DATASOURCE ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get list of dataSources
     */
    getDataSources(packageName: string, options?: DataSourceListOptions): Observable<DataSourceBO<Raw>[]> {
        return this.api.devTools.getDataSources(packageName, options).pipe(
            map((res: DataSourceListResponse) => {
                // fake entity's `id` property to enforce internal application entity uniformity
                return res.items.map(item => Object.assign(item, { id: item.globalId }) as DataSourceBO<Raw>);
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get a single dataSource
     */
    getDataSource(packageName: string, globalId: string): Observable<DataSourceBO<Raw>> {
        return this.api.devTools.getDataSource(packageName, globalId).pipe(
            map((res: DataSourceLoadResponse) => res.datasource),
            // fake entity's `id` property to enforce internal application entity uniformity
            map((item: DataSource<Raw>) => Object.assign(item, { id: item.globalId })),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Change dataSources of a package
     */
    changeDataSourceOfPackage(packageName: string, globalId: string[], preselected: string[]): Observable<void> {
        const addFn = (a: string, b: string) => {
            return this.api.devTools.addDataSourceToPackage(a, b);
        };
        const removeFn = (a: string, b: string) => {
            return this.api.devTools.removeDataSourceFromPackage(a, b);
        };
        const refreshFn = (p: string) => this.get(packageName);
        return this.packageEntitiesManager.changeEntitiesOfPackage(
            packageName,
            'dataSource',
            globalId,
            preselected,
            addFn.bind(this),
            'dataSource.dataSource_successfully_added_to_package',
            removeFn.bind(this),
            'dataSource.dataSource_successfully_removed_from_package',
            refreshFn.bind(this),
        );
    }

    /**
     * Add an existing dataSource to a package
     */
    addDataSourceToPackage(packageName: string, globalId: string | string[], preselected?: string[]): Observable<void> {
        const addFn = (a: string, b: string) => {
            return this.api.devTools.addDataSourceToPackage(a, b);
        };
        const refreshFn = (p: string) => this.get(packageName);
        return this.packageEntitiesManager.addEntityToPackage(
            packageName,
            'dataSource',
            globalId,
            addFn.bind(this),
            'dataSource.dataSource_successfully_added_to_package',
            refreshFn.bind(this),
        );
    }

    /**
     * Remove an existing dataSource from a package
     */
    removeDataSourceFromPackage(packageName: string, dataSourceId: string | number, notify = true): Observable<void> {
        const dsToBeRemoved: DataSource | null = this.appState.now.entity.dataSource[dataSourceId];

        return this.api.devTools.removeDataSourceFromPackage(packageName, dataSourceId).pipe(
            discard(() => {
                this.entityManager.deleteEntities(this.entityIdentifier, [dataSourceId]);

                if (!dsToBeRemoved) {
                    return;
                }

                if (notify) {
                    this.notification.show({
                        type: 'success',
                        message: 'dataSource.dataSource_successfully_removed_from_package',
                        translationParams: {
                            name: dsToBeRemoved.name,
                        },
                    });
                }
            }),
        );
    }

    removeDataSourcesFromPackage(packageName: string, dataSourceIds: (string | number)[]): Observable<void> {
        return forkJoin(dataSourceIds.map(id => this.removeDataSourceFromPackage(packageName, id))).pipe(
            discard(),
        );
    }

    // OBJECTPROPERTY ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get list of objectproperties
     */
    getObjectproperties(packageName: string, options?: ObjectPropertyListOptions): Observable<ObjectPropertyBO<Raw>[]> {
        return this.api.devTools.getObjectproperties(packageName, options).pipe(
            map((res: ObjectPropertyListResponse) => {
                // fake entity's `id` property to enforce internal application entity uniformity
                return res.items.map(item => Object.assign(item, { id: item.globalId }) as ObjectPropertyBO<Raw>);
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get a single objectProperty
     */
    getObjectProperty(packageName: string, globalId: string): Observable<ObjectPropertyBO<Raw>> {
        return this.api.devTools.getObjectProperty(packageName, globalId).pipe(
            map((res: ObjectPropertyLoadResponse) => res.objectProperty),
            // fake entity's `id` property to enforce internal application entity uniformity
            map((item: ObjectProperty<Raw>) => Object.assign(item, { id: item.globalId })),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Change objectPropertys of a package
     */
    changeObjectPropertyOfPackage(packageName: string, globalId: string[], preselected: string[]): Observable<void> {
        const addFn = (a: string, b: string) => {
            return this.api.devTools.addObjectPropertyToPackage(a, b);
        };
        const removeFn = (a: string, b: string) => {
            return this.api.devTools.removeObjectPropertyFromPackage(a, b);
        };
        const refreshFn = (p: string) => this.get(packageName);
        return this.packageEntitiesManager.changeEntitiesOfPackage(
            packageName,
            'objectProperty',
            globalId,
            preselected,
            addFn.bind(this),
            'objectProperty.objectProperty_successfully_added_to_package',
            removeFn.bind(this),
            'objectProperty.objectProperty_successfully_removed_from_package',
            refreshFn.bind(this),
        );
    }

    /**
     * Add an existing objectProperty to a package
     */
    addObjectPropertyToPackage(packageName: string, globalId: string | string[], preselected?: string[]): Observable<void> {
        const addFn = (a: string, b: string) => {
            return this.api.devTools.addObjectPropertyToPackage(a, b);
        };
        const refreshFn = (p: string) => this.get(packageName);
        return this.packageEntitiesManager.addEntityToPackage(
            packageName,
            'objectProperty',
            globalId,
            addFn.bind(this),
            'objectProperty.objectProperty_successfully_added_to_package',
            refreshFn.bind(this),
        );
    }

    /**
     * Remove an existing objectProperty from a package
     */
    removeObjectPropertyFromPackage(packageName: string, globalId: string | string[]): Observable<void> {
        const removeFn = (a: string, b: string) => {
            return this.api.devTools.removeObjectPropertyFromPackage(a, b);
        };
        const refreshFn = (p: string) => this.get(packageName);
        return this.packageEntitiesManager.removeEntityFromPackage(
            packageName,
            'objectProperty',
            globalId,
            removeFn.bind(this),
            'objectProperty.objectProperty_successfully_removed_from_package',
            refreshFn.bind(this),
        );
    }

    // TEMPLATES ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get list of templates
     */
    getTemplates(packageName: string, options?: TemplateFolderListRequest): Observable<TemplateBO<Raw>[]> {
        return this.api.devTools.getTemplates(packageName, options).pipe(
            map(res => {
                // fake entity's `id` property to enforce internal application entity uniformity
                return res.items.map(item => Object.assign(item, { id: item.globalId }) as TemplateBO<Raw>);
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get a single template
     */
    getTemplate(packageName: string, globalId: string): Observable<TemplateBO<Raw>> {
        return this.api.devTools.getTemplate(packageName, globalId).pipe(
            map((res: TemplateResponse) => res.template),
            // fake entity's `id` property to enforce internal application entity uniformity
            map((item: Template<Raw>) => Object.assign(item, { id: item.globalId })),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Change templates of a package
     */
    changeTemplateOfPackage(packageName: string, globalId: string[], preselected: string[]): Observable<void> {
        const addFn = (a: string, b: string) => {
            return this.api.devTools.addTemplateToPackage(a, b);
        };
        const removeFn = (a: string, b: string) => {
            return this.api.devTools.removeTemplateFromPackage(a, b);
        };
        const refreshFn = (p: string) => this.get(packageName);
        return this.packageEntitiesManager.changeEntitiesOfPackage(
            packageName,
            'template',
            globalId,
            preselected,
            addFn.bind(this),
            'template.template_successfully_added_to_package',
            removeFn.bind(this),
            'template.template_successfully_removed_from_package',
            refreshFn.bind(this),
        );
    }

    /**
     * Add an existing template to a package
     */
    addTemplateToPackage(packageName: string, globalId: string | string[], preselected?: string[]): Observable<void> {
        const addFn = (a: string, b: string) => {
            return this.api.devTools.addTemplateToPackage(a, b);
        };
        const refreshFn = (p: string) => this.get(packageName);
        return this.packageEntitiesManager.addEntityToPackage(
            packageName,
            'template',
            globalId,
            addFn.bind(this),
            'template.template_successfully_added_to_package',
            refreshFn.bind(this),
        );
    }

    /**
     * Remove an existing template from a package
     */
    removeTemplateFromPackage(packageName: string, globalId: string | string[]): Observable<void> {
        const removeFn = (a: string, b: string) => {
            return this.api.devTools.removeTemplateFromPackage(a, b);
        };
        const refreshFn = (p: string) => this.get(packageName);
        return this.packageEntitiesManager.removeEntityFromPackage(
            packageName,
            'template',
            globalId,
            removeFn.bind(this),
            'template.template_successfully_removed_from_package',
            refreshFn.bind(this),
        );
    }

}
