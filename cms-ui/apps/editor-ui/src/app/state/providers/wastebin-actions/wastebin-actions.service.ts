import { Injectable } from '@angular/core';
import {
    BaseListResponse,
    FileOrImage,
    Folder,
    FolderListOptions,
    GtxCmsQueryOptions,
    Item,
    ItemType,
    Page,
    SortField,
} from '@gentics/cms-models';
import { Observable, forkJoin, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { Api, ApiError } from '../../../core/providers/api';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { I18nNotification } from '../../../core/providers/i18n-notification/i18n-notification.service';
import {
    CloseEditorAction,
    RestoreWasteBinItemsAction,
    SetWasteBinSortingAction,
    StartWasteBinItemsDeletionAction,
    StartWasteBinItemsFetchingAction,
    WasteBinItemsDeletionErrorAction,
    WasteBinItemsDeletionSuccessAction,
    WasteBinItemsFetchingErrorAction,
    WasteBinItemsFetchingSuccessAction,
} from '../../modules';
import { ApplicationStateService } from '../application-state/application-state.service';
import { FolderActionsService } from '../folder-actions/folder-actions.service';

@Injectable()
export class WastebinActionsService {

    constructor(
        private appState: ApplicationStateService,
        private api: Api,
        private errorHandler: ErrorHandler,
        private entityResolver: EntityResolver,
        private folderActions: FolderActionsService,
        private notification: I18nNotification,
    ) {}

    /**
     * Fetches all the contents of the wastebin for the given node.
     */
    getWastebinContents(
        nodeId: number,
        sortBy: SortField,
        sortOrder: 'asc' | 'desc',
        nodeFeatIsActiveForms: boolean = false,
    ): void {
        this.getWastebinContentsByType(nodeId, 'folder', sortBy, sortOrder);
        if (nodeFeatIsActiveForms) {
            this.getWastebinContentsByType(nodeId, 'form', sortBy, sortOrder);
        }
        this.getWastebinContentsByType(nodeId, 'page', sortBy, sortOrder);
        this.getWastebinContentsByType(nodeId, 'file', sortBy, sortOrder);
        this.getWastebinContentsByType(nodeId, 'image', sortBy, sortOrder);
    }

    getWastebinContentsByType(
        nodeId: number,
        type: 'file' | 'folder' | 'form' | 'image' | 'page',
        sortBy: SortField,
        sortOrder: 'asc' | 'desc',
    ): void {
        this.appState.dispatch(new StartWasteBinItemsFetchingAction(type));

        const nodeFolderId = this.entityResolver.getNode(nodeId).folderId;
        const options: FolderListOptions = {
            wastebin: 'only',
            recursive: true,
            inherited: false,
            nodeId,
            sortby: sortBy,
            sortorder: sortOrder,
        };

        this.getApiListMethod(type)(nodeFolderId, options).subscribe((res) => {
            let items: Item[];

            if (type === 'form') {
                items = res.items;
            } else {
                items = res[type === 'image' ? 'files' : `${type}s`];
            }

            this.appState.dispatch(new WasteBinItemsFetchingSuccessAction(type, items));
        }, (error) => {
            this.appState.dispatch(new WasteBinItemsFetchingErrorAction(type, error.message));
            this.errorHandler.catch(error);
        });
    }

    /**
     * Move items to the wastebin and show a notification with an
     * "undo" button that restores the deleted items from the wastebin
     */
    moveItemsToWastebin(
        type: 'folder' | 'page' | 'file' | 'image' | 'form',
        ids: number[],
        nodeId: number,
    ): Promise<{ succeeded: number; failed: number; error: ApiError }> {
        if (!ids.length) {
            return;
        }

        const requests = ids.map((id) =>
            this.api.folders.deleteItem(type, id, nodeId).pipe(
                map((res) => Object.assign(res, { id, error: null })),
                catchError((error) => of({ id, error })),
            ),
        );

        this.appState.dispatch(new StartWasteBinItemsDeletionAction(type, ids));

        return forkJoin(requests)
            .toPromise()
            .then((results) => {
                const succeeded = results
                    .filter((res) => !res.error)
                    .map((res) => res.id);
                const badResponses = results.filter((res: any) => res.error);
                const failed = badResponses.map((res) => res.id);
                const error = badResponses.length && badResponses[0].error;

                if (failed.length) {
                    this.appState.dispatch(new WasteBinItemsDeletionErrorAction(type, failed, error.message));
                }

                if (succeeded.length) {
                    this.appState.dispatch(new WasteBinItemsDeletionSuccessAction(type, succeeded));

                    // if the item that is currently being edited is deleted, close the editor
                    const { itemType, itemId } = this.appState.now.editor;
                    if (itemType === type && succeeded.indexOf(itemId) >= 0) {
                        this.appState.dispatch(new CloseEditorAction());
                    }
                }

                return {
                    succeeded: succeeded.length,
                    failed: failed.length,
                    error: error,
                    ids: {
                        succeeded: succeeded,
                        failed: failed,
                    },
                };
            });
    }

    /**
     * Restore one or more items from the wastebin. Returns true if one or more restored items belong to the
     * currently-active folder. This indicates that the folder contents will be refreshed.
     */
    restoreItemsFromWastebin(
        type: 'folder' | 'page' | 'file' | 'form' | 'image',
        ids: number[],
        localizationIds: number[] = [],
    ): Promise<void> {
        const removedIds = [...ids, ...localizationIds];
        return this.api.folders.restoreFromWastebin(type, removedIds).toPromise().then(() => {
            this.appState.dispatch(new RestoreWasteBinItemsAction(type, removedIds));

            this.notification.show({
                message: ids.length === 1
                    ? 'message.wastebin_restored_single'
                    : 'message.wastebin_restored_multiple',
                translationParams: {
                    _type: type,
                    count: ids.length,
                },
                type: 'default',
                delay: 2000,
            });
            const idsInActiveFolder = this.getIdsInActiveFolder(
                type,
                ids,
            );
            const isSearching = !!this.appState.now.folder.searchTerm;
            if (idsInActiveFolder.length || isSearching) {
                this.folderActions.refreshList(type);
            }
        }, (error) => {
            this.errorHandler.catch(error, { notification: false });
            this.notification.show({
                message: ids.length === 1
                    ? 'message.wastebin_restore_failed_single'
                    : 'message.wastebin_restore_failed_multiple',
                translationParams: {
                    _type: type,
                    count: ids.length,
                },
                type: 'alert',
                delay: 2000,
                action: {
                    label: 'common.retry_button',
                    onClick: (): any => this.restoreItemsFromWastebin(
                        type,
                        ids,
                        localizationIds,
                    ),
                },
            });
        });
    }

    /**
     * Permanently delete the items by removing from wastebin.
     */
    deleteItemsFromWastebin(
        type: 'folder' | 'page' | 'file' | 'form' | 'image',
        ids: number[],
    ): Promise<any> {
        this.appState.dispatch(new StartWasteBinItemsDeletionAction(type, ids));

        return (
            this.api.folders.deleteFromWastebin(type, ids).toPromise().then(() => {
                this.appState.dispatch(new WasteBinItemsDeletionSuccessAction(type, ids));

                this.notification.show({
                    message: ids.length === 1
                        ? 'message.wastebin_deleted_single'
                        : 'message.wastebin_deleted_multiple',
                    translationParams: {
                        _type: type,
                        count: ids.length,
                    },
                    type: 'default',
                    delay: 2000,
                });
            }, (error) => {
                this.appState.dispatch(new WasteBinItemsDeletionErrorAction(type, ids, error.message));

                this.errorHandler.catch(error, { notification: false });
                this.notification.show({
                    message: 'message.wastebin_delete_failed',
                    translationParams: {
                        _type: type,
                        count: ids.length,
                    },
                    type: 'alert',
                    delay: 5000,
                    action: {
                        label: 'common.retry_button',
                        onClick: (): any =>
                            this.deleteItemsFromWastebin(type, ids),
                    },
                });
            })
            // refresh list to update deleted items visible if toggle deleted
                .then(() => this.folderActions.refreshList(type))
        );
    }

    /**
     * For any items whose parent folder is the active folder, we need to refresh that list to reflect the restoration
     * of the item to the list.
     */
    private getIdsInActiveFolder(
        type: 'folder' | 'form' | 'page' | 'file' | 'image',
        allIds: number[],
    ): number[] {
        const folderState = this.appState.now.folder;
        const activeFolderId = folderState.activeFolder;

        const idsInActiveFolder = allIds
            .map((id) => {
                const item = this.entityResolver.getEntity(type, id);
                const parentFolderId =
                    (item as Page | FileOrImage).folderId ||
                    (item as Folder).motherId ||
                    -1;
                const isInActiveFolder = parentFolderId === activeFolderId;
                return isInActiveFolder ? id : -1;
            })
            .filter((id) => id >= 0);

        return idsInActiveFolder;
    }

    /**
     * Given the itemType, returns a function which calls the correct API method to get a list of that type of
     * entity.
     */
    private getApiListMethod(
        type: ItemType,
    ): (
            parentId: number,
            options?: FolderListOptions
        ) => Observable<BaseListResponse & { [type: string]: any }> {
        return (parentId: number, options?: GtxCmsQueryOptions) => {
            switch (type) {
                case 'folder':
                    return this.api.folders.getFolders(parentId, options);
                case 'page': {
                    const languageId = this.appState.now.folder.activeLanguage;
                    const language = this.entityResolver.getLanguage(languageId);
                    let pageListOptions: GtxCmsQueryOptions = options;
                    if (language) {
                        pageListOptions = Object.assign(options, {
                            langvars: true,
                            language: language.code,
                        });
                    }
                    return this.api.folders.getPages(parentId, pageListOptions);
                }
                case 'file':
                    return this.api.folders.getFiles(parentId, options);
                case 'image':
                    return this.api.folders.getImages(parentId, options);
                case 'form':
                    return this.api.folders.getForms(parentId, options);
                default:
                    throw new Error(`The type "${type}" was not recognised`);
            }
        };
    }

    /**
     * Set the sortBy and sortOrder for a given type of item.
     */
    setSorting(sortBy: SortField, sortOrder: 'asc' | 'desc'): void {
        this.appState.dispatch(new SetWasteBinSortingAction(sortBy, sortOrder));
    }
}
