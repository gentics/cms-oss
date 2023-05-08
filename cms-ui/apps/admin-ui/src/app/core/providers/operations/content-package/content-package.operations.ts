import { discard } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable, Injector } from '@angular/core';
import { downloadFromBlob } from '@gentics/cms-components';
import {
    ContentPackage,
    ContentPackageBO,
    ContentPackageCreateRequest,
    ContentPackageSaveRequest,
    ContentPackageSyncOptions,
    Response,
} from '@gentics/cms-models';
import { ApiError, GcmsApi } from '@gentics/cms-rest-clients-angular';
import { NotificationService } from '@gentics/ui-core';
import { last } from 'lodash';
import { from, Observable, of, throwError } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { I18nNotificationService } from '../../i18n-notification';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class ContentPackageOperations extends ExtendedEntityOperationsBase<'contentPackage'> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private entityManager: EntityManagerService,
        private appState: AppStateService,
        private i18nNotification: I18nNotificationService,
        private notification: NotificationService,
    ) {
        super(injector, 'contentPackage');
    }

    getAll(options?: any, parentId?: string): Observable<ContentPackageBO[]> {
        return this.api.contentStaging.listContentPackages(options).pipe(
            map(res => (res.items || []).map(item => this.mapToBusinessObject(item))),
            tap(packages => this.entityManager.addEntities(this.entityIdentifier, packages)),
            this.catchAndRethrowError(),
        )
    }

    get(entityId: string, options?: any, parentId?: string | number): Observable<ContentPackageBO> {
        return this.api.contentStaging.getContentPackage(entityId).pipe(
            map(res => this.mapToBusinessObject(res.contentPackage)),
            tap(pkg => this.entityManager.addEntity(this.entityIdentifier, pkg)),
            this.catchAndRethrowError(),
        );
    }

    create(body: ContentPackageCreateRequest, notify: boolean = true): Observable<ContentPackageBO> {
        return this.api.contentStaging.createContentPackage(body).pipe(
            map(res => this.mapToBusinessObject(res.contentPackage)),
            tap(pkg => {
                this.entityManager.addEntity(this.entityIdentifier, pkg);
                if (notify) {
                    this.i18nNotification.show({
                        type: 'success',
                        message: 'shared.item_created',
                        translationParams: {
                            name: pkg.name,
                        },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    update(entityId: string, body: ContentPackageSaveRequest, notify: boolean = true): Observable<ContentPackageBO> {
        return this.api.contentStaging.updateContentPackage(entityId, body).pipe(
            map(res => this.mapToBusinessObject(res.contentPackage)),
            tap(pkg => {
                this.entityManager.addEntity(this.entityIdentifier, pkg);
                if (notify) {
                    this.i18nNotification.show({
                        type: 'success',
                        message: 'shared.item_updated',
                        translationParams: {
                            name: pkg.name,
                        },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    delete(entityId: string, notify: boolean = true): Observable<void> {
        const pktToBeDeleted = this.appState.now.entity.contentPackage[entityId];

        return this.api.contentStaging.deleteContentPackage(entityId).pipe(
            discard(() => {
                if (notify && pktToBeDeleted) {
                    this.i18nNotification.show({
                        type: 'success',
                        message: 'shared.item_singular_deleted',
                        translationParams: {
                            name: pktToBeDeleted.name,
                        },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    download(entityId: string, notify: boolean = true): Observable<boolean> {
        const entity: ContentPackageBO = this.appState.now.entity.contentPackage[entityId];
        let downloadName: string;
        if (entity) {
            const date = new Date(entity.timestamp * 1000);
            downloadName = `gcms_export_${entityId}_${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}_${date.getHours()}-${date.getMinutes()}-${date.getSeconds()}.zip`;
        } else {
            downloadName = `gcms_export_${entityId}.zip`;
        }

        return this.api.contentStaging.downloadContentPackage(entityId).pipe(
            switchMap(blob => from(downloadFromBlob(blob, downloadName))),
            map(() => true),
            catchError(err => {
                if (notify) {
                    this.i18nNotification.show({
                        type: 'alert',
                        delay: 10_000,
                        message: 'content_staging.download_from_content_package_failed',
                        translationParams: {
                            packageName: entityId,
                            errorMessage: err.message,
                        },
                    });
                }
                console.error(err);

                return of(false);
            }),
        );
    }

    upload(entityId: string, file: File, notify: boolean = true): Observable<ContentPackageBO> {
        return this.api.contentStaging.uploadContentPackage(entityId, file).pipe(
            map(res => this.mapToBusinessObject(res.contentPackage)),
            tap(pkg => {
                this.entityManager.addEntity(this.entityIdentifier, pkg);
                if (notify) {
                    this.i18nNotification.show({
                        type: 'success',
                        message: 'content_staging.upload_to_content_package_finished',
                        translationParams: {
                            packageName: pkg.name,
                        },
                    });
                }
            }),
            catchError(err => {
                const pkg = this.appState.now.entity.contentPackage[entityId];

                this.i18nNotification.show({
                    type: 'alert',
                    message: 'content_staging.upload_to_content_package_failed',
                    translationParams: {
                        packageName: pkg?.name,
                        errorMessage: err.message,
                    },
                });

                return throwError(err);
            }),
            this.catchAndRethrowError(),
        )
    }

    importFromFileSystem(entityId: string, options?: ContentPackageSyncOptions, notify: boolean = true): Observable<void> {
        return this.api.contentStaging.importContentPackage(entityId, options).pipe(
            discard(res => {
                if (notify) {
                    this.notification.show({
                        type: 'success',
                        message: last(res.messages)?.message || res.responseInfo.responseMessage,
                    });
                }
            }),
            catchError(err => {
                if (!(err instanceof ApiError)) {
                    return throwError(err);
                }

                const res: Response = err.response;

                this.notification.show({
                    type: 'alert',
                    message: last(res.messages)?.message || res.responseInfo.responseMessage,
                    delay: 10_000,
                });

                return of(null);
            }),
            this.catchAndRethrowError(),
        );
    }

    exportToFileSystem(entityId: string, options?: ContentPackageSyncOptions, notify: boolean = true): Observable<void> {
        return this.api.contentStaging.exportContentPackage(entityId, options).pipe(
            discard(res => {
                if (notify) {
                    this.notification.show({
                        type: 'success',
                        message: last(res.messages)?.message || res.responseInfo.responseMessage,
                    });
                }
            }),
            catchError(err => {
                if (!(err instanceof ApiError)) {
                    return throwError(err);
                }

                const res: Response = err.response;

                this.notification.show({
                    type: 'alert',
                    message: last(res.messages)?.message || res.responseInfo.responseMessage,
                    delay: 10_000,
                });

                return of(null);
            }),
            this.catchAndRethrowError(),
        );
    }

    private mapToBusinessObject(contentPackage: ContentPackage): ContentPackageBO {
        return {
            ...contentPackage,
            id: contentPackage.name,
        };
    }
}
