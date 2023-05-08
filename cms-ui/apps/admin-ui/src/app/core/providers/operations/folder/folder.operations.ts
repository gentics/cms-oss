import { Injectable, Injector } from '@angular/core';
import { Folder, FolderListOptions, FolderSaveRequest, Raw } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map, switchMap, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { I18nNotificationService } from '../../i18n-notification';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class FolderOperations extends ExtendedEntityOperationsBase<'folder'> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private entityManager: EntityManagerService,
        private notification: I18nNotificationService,
    ) {
        super(injector, 'folder');
    }

    /**
     * Loads a single folder from the CMS and adds it to the EntityState.
     */
    get(folderId: number): Observable<Folder<Raw>> {
        return this.api.folders.getItem(folderId, 'folder').pipe(
            map(res => res.folder),
            tap(folder => this.entityManager.addEntity(this.entityIdentifier, folder)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Gets the nested list of all subfolders for some root folder that are visible to the current user and adds them to the EntityState.
     */
    getFolders(folderId: number, options?: FolderListOptions): Observable<Folder<Raw>[]> {
        return this.api.folders.getFolders(folderId, { ...(options || {}) , wastebin: 'exclude' }).pipe(
            map(response => response.folders),
            tap(folders => this.entityManager.addEntities(this.entityIdentifier, folders)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Gets the nested list of root folders that are visible to the current user and adds them to the EntityState.
     */
    getAll(): Observable<Folder<Raw>[]> {
        return this.api.folders.getFolders(0, { wastebin: 'exclude' }).pipe(
            map(response => response.folders),
            tap(folders => this.entityManager.addEntities(this.entityIdentifier, folders)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Updates the `Folder` with the specified `id`
     */
    update(id: number, update: FolderSaveRequest, notification: boolean = true): Observable<Folder<Raw>> {
        return this.api.folders.updateItem(this.entityIdentifier, id, update.folder).pipe(
            switchMap(() => this.get(id)),
            tap((folder: Folder<Raw>) => {
                if (notification) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_updated',
                        translationParams: { name: folder.name },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

}
