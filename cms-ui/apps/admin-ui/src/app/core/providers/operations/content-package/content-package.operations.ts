import { discard } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable, Injector } from '@angular/core';
import { downloadFromBlob } from '@gentics/cms-components';
import {
    ContentPackage,
    ContentPackageBO,
    ContentPackageCreateRequest,
    ContentPackageErrorResponse,
    ContentPackageSaveRequest,
    ContentPackageSyncOptions,
    Response,
} from '@gentics/cms-models';
import { GCMSRestClientRequestError } from '@gentics/cms-rest-client';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { NotificationService, OpenedNotification } from '@gentics/ui-core';
import { last } from 'lodash-es';
import { Observable, from, of, throwError } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { I18nNotificationService } from '../../i18n-notification';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class ContentPackageOperations extends ExtendedEntityOperationsBase<'contentPackage'> {

    constructor(
        injector: Injector,
        private client: GCMSRestClientService,
        private entityManager: EntityManagerService,
        private appState: AppStateService,
        private i18nNotification: I18nNotificationService,
        private notification: NotificationService,
    ) {
        super(injector, 'contentPackage');
    }

    // eslint-disable-next-line @typescript-eslint/no-unused-vars, @typescript-eslint/explicit-module-boundary-types
    getAll(options?: any, parentId?: string): Observable<ContentPackageBO[]> {
        return this.client.contentStaging.list(options).pipe(
            map(res => (res.items || []).map(item => this.mapToBusinessObject(item))),
            tap(packages => {
                this.entityManager.addEntities(this.entityIdentifier, packages);
            }),
            this.catchAndRethrowError(),
        )
    }

    // eslint-disable-next-line @typescript-eslint/no-unused-vars, @typescript-eslint/explicit-module-boundary-types
    get(entityId: string, options?: any, parentId?: string | number): Observable<ContentPackageBO> {
        return this.client.contentStaging.get(entityId).pipe(
            map(res => this.mapToBusinessObject(res.contentPackage)),
            tap(pkg => this.entityManager.addEntity(this.entityIdentifier, pkg)),
            this.catchAndRethrowError(),
        );
    }

    create(body: ContentPackageCreateRequest, notify: boolean = true): Observable<ContentPackageBO> {
        return this.client.contentStaging.create(body).pipe(
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
        return this.client.contentStaging.update(entityId, body).pipe(
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
        return this.client.contentStaging.delete(entityId).pipe(
            discard(() => {
                if (notify) {
                    this.i18nNotification.show({
                        type: 'success',
                        message: 'shared.item_singular_deleted',
                        translationParams: {
                            name: entityId,
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

        return this.client.contentStaging.download(entityId).pipe(
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
        return this.client.contentStaging.upload(entityId, file).pipe(
            map(res => this.mapToBusinessObject(res.contentPackage)),
            tap(pkg => {
                this.entityManager.addEntity(this.entityIdentifier, pkg);
                if (notify) {
                    this.i18nNotification.show({
                        type: 'success',
                        message: 'content_staging.upload_to_content_package_finished',
                        translationParams: {
                            packageName: entityId,
                        },
                    });
                }
            }),
            catchError(err => {
                let errMsg = err.message;

                if (err instanceof GCMSRestClientRequestError) {
                    errMsg = last(err.data?.messages)?.message
                        || err.data?.responseInfo?.responseMessage
                        || errMsg;
                }

                this.i18nNotification.show({
                    type: 'alert',
                    message: 'content_staging.upload_to_content_package_failed',
                    delay: 10_000,
                    translationParams: {
                        packageName: entityId,
                        errorMessage: errMsg,
                    },
                });

                return throwError(() => err);
            }),
        )
    }

    importFromFileSystem(entityId: string, options?: ContentPackageSyncOptions, notify: boolean = true): Observable<void> {
        let startNotif: OpenedNotification;

        return of(null).pipe(
            tap(() => {
                if (!notify) {
                    return;
                }

                startNotif = this.i18nNotification.show({
                    message: 'content_staging.start_import_message',
                    type: 'success',
                    delay: 0,
                    dismissOnClick: false,
                    translationParams: {
                        packageName: entityId,
                    },
                });
            }),
            switchMap(() => this.client.contentStaging.import(entityId, options)),
            discard(res => {
                if (startNotif) {
                    startNotif.dismiss();
                }

                if (notify) {
                    this.notification.show({
                        type: 'success',
                        delay: 5_000,
                        message: last(res.messages)?.message || res.responseInfo.responseMessage,
                    });
                }
            }),
            catchError(err => {
                if (startNotif) {
                    startNotif.dismiss();
                }

                if (!(err instanceof GCMSRestClientRequestError)) {
                    return throwError(() => err);
                }

                const res: Response = err.data;

                if(notify) {
                    this.notification.show({
                        type: 'alert',
                        message: last(res.messages)?.message || res.responseInfo.responseMessage,
                        delay: 10_000,
                    });
                }

                return of(null);
            }),
            this.catchAndRethrowError(),
        );
    }

    getImportErrors(entityId: string): Observable<ContentPackageErrorResponse> {
        return this.client.contentStaging.errors(entityId);
    }

    exportToFileSystem(entityId: string, options?: ContentPackageSyncOptions, notify: boolean = true): Observable<void> {
        let startNotif: OpenedNotification;

        return of(null).pipe(
            tap(() => {
                if (!notify) {
                    return;
                }

                startNotif = this.i18nNotification.show({
                    message: 'content_staging.start_export_message',
                    type: 'success',
                    delay: 0,
                    dismissOnClick: false,
                    translationParams: {
                        packageName: entityId,
                    },
                });
            }),
            switchMap(() => this.client.contentStaging.export(entityId, options)),
            discard(res => {
                if (startNotif) {
                    startNotif.dismiss();
                }

                if (notify) {
                    this.notification.show({
                        type: 'success',
                        delay: 5_000,
                        message: last(res.messages)?.message || res.responseInfo.responseMessage,
                    });
                }
            }),
            catchError(err => {
                if (startNotif) {
                    startNotif.dismiss();
                }

                if (!(err instanceof GCMSRestClientRequestError)) {
                    return throwError(() => err);
                }

                const res: Response = err.data;

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
