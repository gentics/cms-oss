import { Injectable } from '@angular/core';
import { EditMode } from '@gentics/cms-integration-api-models';
import { PageVersion } from '@gentics/cms-models';
import { throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { EditorTab, ITEM_PROPERTIES_TAB, PropertiesTab } from '../../../common/models';
import { ApiError } from '../../../core/providers/api';
import { Api } from '../../../core/providers/api/api.service';
import {
    AddEditedEntityToRecentItemsAction,
    CancelEditingAction,
    ChangeTabAction,
    CloseEditorAction,
    ComparePageVersionSourcesAction,
    ComparePageVersionsAction,
    EditItemAction,
    LockItemAction,
    PreviewPageVersionAction,
    ResetPageLockAction,
} from '../../modules';
import { ApplicationStateService } from '../../providers';

@Injectable()
export class EditorActionsService {

    constructor(
        private appState: ApplicationStateService,
        private api: Api,
    ) {}

    previewPage(id: number, nodeId: number): void {
        this.appState.dispatch(new EditItemAction({
            itemType: 'page',
            itemId: id,
            nodeId: nodeId,
            editMode: EditMode.PREVIEW,
        }));
        this.appState.dispatch(new AddEditedEntityToRecentItemsAction());
    }

    previewForm(id: number, nodeId: number): void {
        this.appState.dispatch(new EditItemAction({
            itemType: 'form',
            itemId: id,
            nodeId: nodeId,
            editMode: EditMode.PREVIEW,
        }));
        this.appState.dispatch(new AddEditedEntityToRecentItemsAction());
    }

    /**
     * Preview a page at a specific version
     */
    previewPageVersion(id: number, nodeId: number, version: PageVersion): void {
        this.appState.dispatch(new PreviewPageVersionAction(id, nodeId, version));
        this.appState.dispatch(new AddEditedEntityToRecentItemsAction());
    }

    /**
     * Compare the differences between two versions of a page by their contents
     */
    comparePageVersions(pageId: number, nodeId: number, oldVersion: PageVersion, version: PageVersion): void {
        this.appState.dispatch(new ComparePageVersionsAction(pageId, nodeId, oldVersion, version));
        this.appState.dispatch(new AddEditedEntityToRecentItemsAction());
    }

    /**
     * Compare the differences between two versions of a page by their source code
     */
    comparePageVersionSources(pageId: number, nodeId: number, oldVersion: PageVersion, version: PageVersion): void {
        this.appState.dispatch(new ComparePageVersionSourcesAction(pageId, nodeId, oldVersion, version));
        this.appState.dispatch(new AddEditedEntityToRecentItemsAction());
    }

    /**
     * If the compareWith argument is passed, the editor will go into split screen mode and compare the page against
     * another page with the given id.
     */
    editPage(id: number, nodeId: number, compareWith?: number): void {
        this.appState.dispatch(new EditItemAction({
            compareWithId: compareWith,
            editMode: EditMode.EDIT,
            itemId: id,
            nodeId: nodeId,
            itemType: 'page',
            focusMode: true,
        }));
        this.appState.dispatch(new LockItemAction('page', id, EditMode.EDIT));
        this.appState.dispatch(new AddEditedEntityToRecentItemsAction());
    }

    editPageInheritance(id: number, nodeId: number): void {
        this.appState.dispatch(new EditItemAction({
            compareWithId: null,
            editMode: EditMode.EDIT_INHERITANCE,
            itemId: id,
            nodeId: nodeId,
            itemType: 'page',
            focusMode: true,
        }));
        this.appState.dispatch(new LockItemAction('page', id, EditMode.EDIT_INHERITANCE));
        this.appState.dispatch(new AddEditedEntityToRecentItemsAction());
    }

    /**
     * Edit a form
     */
    editForm(id: number, nodeId: number, compareWith?: number): void {
        this.appState.dispatch(new EditItemAction({
            editMode: EditMode.EDIT,
            itemId: id,
            nodeId: nodeId,
            itemType: 'form',
            focusMode: true,
        }));
        this.appState.dispatch(new LockItemAction('form', id, EditMode.EDIT));
        this.appState.dispatch(new AddEditedEntityToRecentItemsAction());
    }

    /**
     * Revert the page to the last saved version and unlock.
     */
    cancelEditing(id: number): void {
        this.api.folders.cancelEditing(id).pipe(
            catchError((err: ApiError) => {
                if (err.reason === 'failed' && err.response.responseInfo.responseCode === 'NOTFOUND') {
                    // We tried to unlock a file that was deleted / unlocalized. We can ignore the error
                    return [0];
                } else {
                    return throwError(err);
                }
            }),
        ).subscribe(() => {
            this.appState.dispatch(new CancelEditingAction());
            this.appState.dispatch(new ResetPageLockAction(id));
        });
    }

    editImage(id: number, nodeId: number): void {
        this.appState.dispatch(new EditItemAction({
            editMode: EditMode.EDIT,
            itemId: id,
            itemType: 'image',
            nodeId: nodeId,
        }));
        this.appState.dispatch(new AddEditedEntityToRecentItemsAction());
    }

    /**
     * Edit the properties of an item.
     */
    editProperties(
        id: number,
        type: 'file' | 'folder' | 'form' | 'image' | 'page',
        nodeId: number,
        defaultTab?: EditorTab,
        propertiesTab?: PropertiesTab,
    ): void {
        this.appState.dispatch(new EditItemAction({
            editMode: EditMode.EDIT_PROPERTIES,
            itemId: id,
            itemType: type,
            nodeId: nodeId,
            openTab: defaultTab || (type === 'image' || type === 'file' ? 'preview' : 'properties'),
            openPropertiesTab: propertiesTab || ITEM_PROPERTIES_TAB,
        }));
        this.appState.dispatch(new LockItemAction(type as any, id, EditMode.EDIT_PROPERTIES));
        this.appState.dispatch(new AddEditedEntityToRecentItemsAction());
    }

    /**
     * Change the open tab in the contentFrame
     */
    changeTab(newTab: EditorTab, propertiesTab?: PropertiesTab): void {
        this.appState.dispatch(new ChangeTabAction(newTab, propertiesTab));
    }

    closeEditor(): void {
        this.appState.dispatch(new CloseEditorAction());
    }

}
