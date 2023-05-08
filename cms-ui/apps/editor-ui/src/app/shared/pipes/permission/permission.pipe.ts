import { ChangeDetectorRef, OnDestroy, Pipe, PipeTransform } from '@angular/core';
import { EditorPermissions, Folder, Form, Item, Page, getNoPermissions } from '@gentics/cms-models';
import { Observable, Subscription } from 'rxjs';
import { skip } from 'rxjs/operators';
import { PermissionService } from '../../../core/providers/permissions/permission.service';
import { ApplicationStateService } from '../../../state';

/**
 * Returns the permissions for an item and fetches them if necessary.
 *
 * @example
 *   <item-actions [permissions]="item | permissions"></item-actions>
 */
@Pipe({ name: 'permissions', pure: false })
export class PermissionsPipe implements OnDestroy, PipeTransform {

    private lastItem: Item;
    private lastLanguage: number | string;
    private lastResult: EditorPermissions;
    private stateSubscription: Subscription;
    private inputSubscription: Subscription;

    constructor(
        private appState: ApplicationStateService,
        private changeDetector: ChangeDetectorRef,
        private permissionService: PermissionService,
    ) {

        // Invalidate the pipe when switching node
        this.stateSubscription = appState.select(state => state.folder.activeNode).pipe(
            skip(1),
        ).subscribe(nodeId => {
            this.lastItem = undefined;
            this.lastLanguage = undefined;
            this.lastResult = undefined;
            changeDetector.markForCheck();
        });
    }

    ngOnDestroy(): void {
        this.stateSubscription.unsubscribe();
        if (this.inputSubscription) {
            this.inputSubscription.unsubscribe();
        }
    }

    /**
     * Transforms an input item into the permissions of that item.
     * If the permissions are unknown, returns a "no permissions" object and fetches them from the server.
     * If no language is passed, the permissions are returned for the current language.
     *
     * @param item The item to get the permissions for.
     */
    transform(item: Item, language?: string | number): EditorPermissions {
        if (!item) {
            return undefined;
        } else if (item === this.lastItem && language === this.lastLanguage) {
            return this.lastResult;
        }

        if (this.inputSubscription) {
            this.inputSubscription.unsubscribe();
        }

        const state = this.appState.now;
        // The last case below is for permissions of a base folder, which does not have a parent.
        const parentFolderId = (item as Page).folderId || (item as Folder).motherId || (item as Form).folderId || (item as Folder).id;
        const nodeId = state.folder.activeNode;

        let permissionObservable: Observable<EditorPermissions>;

        if (language) {
            let languageId: number;
            if (typeof language === 'number') {
                languageId = language;
            } else if (!state.folder.activeNodeLanguages.list.length) {
                languageId = null;
            } else {
                const languages = Object.keys(state.entities.language)
                    .filter(id => state.entities.language[+id].code === language);
                languageId = languages.length ? Number(languages[0]) : null;
            }

            permissionObservable = this.permissionService.forFolderInLanguage(parentFolderId, nodeId, languageId);
        } else {
            permissionObservable = this.permissionService.forFolder(parentFolderId, nodeId);
        }

        this.lastItem = item;
        this.lastLanguage = language;
        this.lastResult = getNoPermissions();

        // If the permissions are already in the app state, the observable emits synchronously
        // which means we don't need to trigger change detection and can return the emitted value.
        let permissionsWereAlreadyAvailable = true;
        this.inputSubscription = permissionObservable.subscribe(permissions => {
            this.lastResult = permissions;
            if (!permissionsWereAlreadyAvailable) {
                this.changeDetector.markForCheck();
            }
        });
        permissionsWereAlreadyAvailable = false;

        return this.lastResult;
    }
}
