/* eslint-disable @typescript-eslint/no-unused-vars */
import {
    BO_DISPLAY_NAME,
    BO_ID,
    BO_PERMISSIONS,
    EditableEntity,
    EditableEntityBusinessObjects,
    EditableEntityModels,
    EntityCreateRequestModel,
    EntityCreateRequestParams,
    EntityCreateResponseModel,
    EntityEditorHandler,
    EntityList,
    EntityListHandler,
    EntityListRequestModel,
    EntityListRequestParams,
    EntityListResponseModel,
    EntityLoadRequestParams,
    EntityLoadResponseModel,
    EntityUpdateRequestModel,
    EntityUpdateRequestParams,
    EntityUpdateResponseModel,
    discard,
} from '@admin-ui/common';
import { Injectable } from '@angular/core';
import { PackageCheckOptions, PackageCheckResult, PackageListOptions, PackageListResponse, PackageSyncOptions, PackageSyncResponse, ResponseCode } from '@gentics/cms-models';
import { ApiError, GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable, Subject, forkJoin, interval, of, throwError } from 'rxjs';
import { catchError, filter, map, mergeMap, startWith, takeUntil, tap } from 'rxjs/operators';
import { BaseEntityHandlerService } from '../base-entity-handler/base-entity-handler';
import { ErrorHandler } from '../error-handler';
import { I18nNotificationService } from '../i18n-notification';
import { ActivityManagerService } from '../activity-manager';
import { PackageCheckTrableLoaderOptions } from '../dev-tool-check-trable-loader/dev-tool-check-trable-loader.service';

@Injectable()
export class DevToolPackageHandlerService extends BaseEntityHandlerService
    implements EntityEditorHandler<EditableEntity.DEV_TOOL_PACKAGE>,
        EntityListHandler<EditableEntity.DEV_TOOL_PACKAGE> {

    constructor(
        errorHandler: ErrorHandler,
        protected api: GcmsApi,
        protected notification: I18nNotificationService,
        protected activityManager: ActivityManagerService,
    ) {
        super(errorHandler);
    }

    displayName(entity: EditableEntityModels[EditableEntity.DEV_TOOL_PACKAGE]): string {
        return entity.name;
    }

    public mapToBusinessObject(
        devToolPackage: EditableEntityModels[EditableEntity.DEV_TOOL_PACKAGE],
        index?: number,
    ): EditableEntityBusinessObjects[EditableEntity.DEV_TOOL_PACKAGE] {
        return {
            ...devToolPackage,
            [BO_ID]: devToolPackage.name,
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: this.displayName(devToolPackage),
        };
    }

    create(
        data: EntityCreateRequestModel<EditableEntity.DEV_TOOL_PACKAGE>,
        params?: EntityCreateRequestParams<EditableEntity.DEV_TOOL_PACKAGE>,
    ): Observable<EntityCreateResponseModel<EditableEntity.DEV_TOOL_PACKAGE>> {
        return this.api.devTools.addPackage(data).pipe(
            // "Mocked" response, as the backend only returns an empty body
            map(() => ({
                responseInfo: {
                    responseCode: ResponseCode.OK,
                },
                package: {
                    id: data.name,
                    name: data.name,
                },
            })),
        );
    }

    createMapped(
        data: EntityCreateRequestModel<EditableEntity.DEV_TOOL_PACKAGE>,
        options?: EntityCreateRequestParams<EditableEntity.DEV_TOOL_PACKAGE>,
    ): Observable<EditableEntityBusinessObjects[EditableEntity.DEV_TOOL_PACKAGE]> {
        return this.create(data, options).pipe(
            map(res => this.mapToBusinessObject(res.package)),
        );
    }

    get(
        id: string,
        params?: EntityLoadRequestParams<EditableEntity.DEV_TOOL_PACKAGE>,
    ): Observable<EntityLoadResponseModel<EditableEntity.DEV_TOOL_PACKAGE>> {
        return this.api.devTools.getPackage(id).pipe(
            tap(res => {
                const name = res.name;
                this.nameMap[res.name] = name;
            }),
            this.catchAndRethrowError(),
        );
    }

    getMapped(id: string): Observable<EditableEntityBusinessObjects[EditableEntity.DEV_TOOL_PACKAGE]> {
        return this.get(id).pipe(
            map(res => this.mapToBusinessObject(res)),
        );
    }

    /**
     * You cannot update a devtool-package via the API
     */
    update(
        id: string | number,
        data: EntityUpdateRequestModel<EditableEntity.DEV_TOOL_PACKAGE>,
        params?: EntityUpdateRequestParams<EditableEntity.DEV_TOOL_PACKAGE>,
    ): Observable<EntityUpdateResponseModel<EditableEntity.DEV_TOOL_PACKAGE>> {
        return of(null as never);
    }

    /**
     * You cannot update a devtool-package via the API
     */
    updateMapped(
        id: string | number,
        data: EntityUpdateRequestModel<EditableEntity.DEV_TOOL_PACKAGE>,
        params?: EntityUpdateRequestParams<EditableEntity.DEV_TOOL_PACKAGE>,
    ): Observable<EditableEntityBusinessObjects[EditableEntity.DEV_TOOL_PACKAGE]> {
        return of(null as never);
    }

    delete(id: string): Observable<void> {
        return this.api.devTools.removePackage(id).pipe(
            tap(() => {
                const name = this.nameMap[id];

                if (!name) {
                    return;
                }

                delete this.nameMap[id];
                this.notification.show({
                    type: 'success',
                    message: 'shared.item_singular_deleted',
                    translationParams: {
                        name,
                    },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    list(
        body?: EntityListRequestModel<EditableEntity.DEV_TOOL_PACKAGE>,
        params?: EntityListRequestParams<EditableEntity.DEV_TOOL_PACKAGE>,
    ): Observable<EntityListResponseModel<EditableEntity.DEV_TOOL_PACKAGE>> {
        return this.api.devTools.getPackages(params).pipe(
            tap(res => {
                res.items.forEach(pkg => {
                    const name = this.displayName(pkg);
                    this.nameMap[name] = name;
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    listMapped(
        body?: EntityListRequestModel<EditableEntity.DEV_TOOL_PACKAGE>,
        params?: EntityListRequestParams<EditableEntity.DEV_TOOL_PACKAGE>,
    ): Observable<EntityList<EditableEntityBusinessObjects[EditableEntity.DEV_TOOL_PACKAGE]>> {
        return this.list(body, params).pipe(
            map(res => ({
                items: res.items.map((item, index) => this.mapToBusinessObject(item, index)),
                totalItems: res.numItems,
            })),
        );
    }

    /**
     * Add an existing package to a node
     */
    assignToNode(nodeId: string | number, devToolPackages: string | string[]): Observable<void> {
        const request = (name: string): Observable<void> => {
            return this.api.devTools.addPackageToNode(nodeId, name).pipe(
                // display toast notification
                tap(() => this.notification.show({
                    type: 'success',
                    message: 'package.package_successfully_added_to_node',
                    translationParams: { name },
                })),
            );
        };
        let stream: Observable<void>;
        if (Array.isArray(devToolPackages) && devToolPackages.length > 0) {
            stream = forkJoin(devToolPackages.map(name => request(name))).pipe(
                discard(),
            );
        }
        if (typeof devToolPackages === 'string') {
            stream = request(devToolPackages);
        }
        return stream.pipe(
            this.catchAndRethrowError(),
        );
    }

    /**
     * Remove (an) existing package(s) from a node
     */
    unassignFromNode(nodeId: string | number, devToolPackages: string | string[]): Observable<void> {
        const request = (name: string): Observable<void> => {
            return this.api.devTools.removePackageFromNode(nodeId, name).pipe(
                // display toast notification
                tap(() => this.notification.show({
                    type: 'success',
                    message: 'package.package_successfully_removed_from_node',
                    translationParams: { name },
                })),
            );
        };
        let stream: Observable<void>;
        if (Array.isArray(devToolPackages) && devToolPackages.length > 0) {
            stream = forkJoin(devToolPackages.map(name => request(name))).pipe(
                discard(),
            );
        }
        if (typeof devToolPackages === 'string') {
            stream = request(devToolPackages);
        }
        return stream.pipe(
            this.catchAndRethrowError(),
        );
    }

    listFromNode(
        nodeId: string | number,
        body?: never,
        params?: PackageListOptions,
    ): Observable<PackageListResponse> {
        return this.api.devTools.getPackagesOfNode(nodeId, params).pipe(
            tap(res => {
                res.items.forEach(pkg => {
                    const name = this.displayName(pkg);
                    this.nameMap[name] = name;
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    listFromNodeMapped(
        nodeId: string | number,
        body?: never,
        params?: PackageListOptions,
    ): Observable<EntityList<EditableEntityBusinessObjects[EditableEntity.DEV_TOOL_PACKAGE]>> {
        return this.listFromNode(nodeId, body, params).pipe(
            map(res => ({
                items: res.items.map((item, index) => this.mapToBusinessObject(item, index)),
                totalItems: res.numItems,
            })),
        );
    }

    /**
     * Get the current status information for the automatic synchronization
     */
    getSyncState(): Observable<PackageSyncResponse> {
        return this.api.devTools.getSyncState();
    }

    /**
     * Start the sync
     */
    startSync(): Observable<PackageSyncResponse> {
        return this.api.devTools.startSync().pipe(
            tap(() => {
                this.notification.show({
                    message: 'package.autosync_activate_message',
                    type: 'success',
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Stop the sync, if it was started by the current user
     */
    stopSync(): Observable<PackageSyncResponse> {
        return this.api.devTools.stopSync().pipe(
            tap(() => {
                this.notification.show({
                    message: 'package.autosync_deactivate_message',
                    type: 'success',
                });
            }),
            // Hacky workaround because the backend may not respond with a body
            map(res => ({
                ...(res || {}),
                enabled: res?.enabled || false,
            } as any)),
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
        }
    }

    /**
     * Perform a consistency check on packages.
     */
    checkOneOrMoreWithSuccessMessage(packageName: string | string[], options?: PackageCheckOptions): Observable<void> {
        const request = (name: string): Observable<void> => {
            return this.api.devTools.check(name, options).pipe(
                tap(() => this.notification.show({
                    type: 'success',
                    message: 'package.consistency_check_result',
                    translationParams: { name },
                })),
                discard(),
            );
        };
        let stream: Observable<void>;
        if (Array.isArray(packageName) && packageName.length > 0) {
            stream = forkJoin(packageName.map(name => request(name))).pipe(
                discard(),
            );
        }
        if (typeof packageName === 'string') {
            stream = request(packageName);
        }
        return stream.pipe(
            this.catchAndRethrowError(),
        );
    }


    /**
     * Perform a consistency check on a package.
     */
    check(packageName: string, options?: PackageCheckOptions): Observable<PackageCheckResult> {
        return this.api.devTools.check(packageName, options);
    }

    /**
     * Get the result of the consistency check for a package
     */
    getCheckResult(packageName: string): Observable<PackageCheckResult> {
        return this.api.devTools.getCheckResult(packageName);
    }


    /**
     * Poll the check result
     */
    pollCheckResultUntilResultIsAvailable(options: PackageCheckTrableLoaderOptions): Observable<boolean> {
        const pollStop = new Subject();

        return interval(10000).pipe(
            startWith(0),
            mergeMap(() => this.isCheckResultAvailable(options)),
            filter(isAvailable => isAvailable === true),
            tap(() => {
                pollStop.next();
                pollStop.complete();
            }),
            takeUntil(pollStop),
        );
    }

    /**
     * Determines if a check result is available
     */
    isCheckResultAvailable(options: PackageCheckTrableLoaderOptions): Observable<boolean> {
        return this.api.devTools.getCheckResult(options.packageName)
            .pipe(
                map(() => true),
                catchError((err) => {
                    if (err instanceof ApiError && err.statusCode === 404) {
                        return of(false);
                    }
                    return throwError(err);
                }),
                this.catchAndRethrowError(),
            )
    }

}
