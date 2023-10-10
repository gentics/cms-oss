import { Injectable } from '@angular/core';
import { StateContext } from '@ngxs/store';
import { append, iif, patch, removeItem } from '@ngxs/store/operators';
import { EditMode, EditorState, ITEM_PROPERTIES_TAB } from '../../../common/models';
import { ActionDefinition, AppStateBranch } from '../../state-utils';
import {
    AddExpandedTabGroupAction,
    CancelEditingAction,
    ChangeTabAction,
    CloseEditorAction,
    ComparePageVersionSourcesAction,
    ComparePageVersionsAction,
    EDITOR_STATE_KEY,
    EditItemAction,
    FocusEditorAction,
    FocusListAction,
    MarkContentAsModifiedAction,
    MarkObjectPropertiesAsModifiedAction,
    PreviewPageVersionAction,
    RemoveExpandedTabGroupAction,
    SaveErrorAction,
    SaveSuccessAction,
    SetFocusModeAction,
    SetOpenObjectPropertyGroupsAction,
    SetUploadStatusAction,
    StartSavingAction,
} from './editor.actions';

const INITIAL_EDITOR_STATE: EditorState = {
    editorIsOpen: false,
    editorIsFocused: false,
    fetching: false,
    focusMode: false,
    lastError: '',
    saving: false,
    openTab: 'properties',
    openPropertiesTab: ITEM_PROPERTIES_TAB,
    openObjectPropertyGroups: [],
    contentModified: false,
    objectPropertiesModified: false,
    modifiedObjectPropertiesValid: false,
    uploadInProgress: false,
};

@AppStateBranch<EditorState>({
    name: EDITOR_STATE_KEY,
    defaults: INITIAL_EDITOR_STATE,
})
@Injectable()
export class EditorStateModule {

    @ActionDefinition(CancelEditingAction)
    cancelEditing(ctx: StateContext<EditorState>, action: CancelEditingAction): void {
        ctx.patchState({
            contentModified: false,
            objectPropertiesModified: false,
        });
    }

    /**
     * Changes to the specified `EditorTab`.
     *
     * @param tab The `EditorTab` that should be switched to.
     * @param propertiesTab (optional) The properties tab to switch to; if this is omitted, `ITEM_PROPERTIES_TAB` will be used as the default value.
     */
    @ActionDefinition(ChangeTabAction)
    handleChangeTabAction(ctx: StateContext<EditorState>, action: ChangeTabAction): void {
        if (!['preview', 'properties'].includes(action.tab)) {
            return;
        }

        ctx.setState(patch({
            openTab: action.tab,
            openPropertiesTab: iif(action.tab === 'properties', action.propertiesTab),
        }));
    }

    @ActionDefinition(CloseEditorAction)
    handleCloseEditorAction(ctx: StateContext<EditorState>, action: CloseEditorAction): void {
        ctx.patchState({
            editorIsOpen: false,
            editorIsFocused: false,
        });
    }

    @ActionDefinition(SetOpenObjectPropertyGroupsAction)
    handleSetOpenObjectPropertyGroupsAction(ctx: StateContext<EditorState>, action: SetOpenObjectPropertyGroupsAction): void {
        ctx.patchState({
            openObjectPropertyGroups: action.groups.slice(),
        });
    }

    @ActionDefinition(AddExpandedTabGroupAction)
    handleAddExpandedTabGroupAction(ctx: StateContext<EditorState>, action: AddExpandedTabGroupAction): void {
        ctx.setState(patch({
            openObjectPropertyGroups: append([action.group]),
        }));
    }

    @ActionDefinition(RemoveExpandedTabGroupAction)
    handleRemoveExpandedTabGroupAction(ctx: StateContext<EditorState>, action: RemoveExpandedTabGroupAction): void {
        ctx.setState(patch({
            openObjectPropertyGroups: removeItem(group => group === action.group),
        }));
    }

    @ActionDefinition(ComparePageVersionsAction)
    handleComparePageVersionsAction(ctx: StateContext<EditorState>, action: ComparePageVersionsAction): void {
        ctx.patchState({
            // Static changes
            compareWithId: undefined,
            contentModified: false,
            editMode: EditMode.COMPARE_VERSION_CONTENTS,
            editorIsOpen: true,
            editorIsFocused: true,
            itemType: 'page',
            objectPropertiesModified: false,
            uploadInProgress: false,
            // Action values
            itemId: action.itemId,
            nodeId: action.nodeId,
            oldVersion: action.oldVersion,
            version: action.version,
        });
    }

    @ActionDefinition(ComparePageVersionSourcesAction)
    handleComparePageVersionSourcesAction(ctx: StateContext<EditorState>, action: ComparePageVersionSourcesAction): void {
        ctx.patchState({
            // Static changes
            compareWithId: undefined,
            contentModified: false,
            editMode: EditMode.COMPARE_VERSION_SOURCES,
            editorIsOpen: true,
            editorIsFocused: true,
            itemType: 'page',
            objectPropertiesModified: false,
            uploadInProgress: false,
            // Action values
            itemId: action.itemId,
            nodeId: action.nodeId,
            oldVersion: action.oldVersion,
            version: action.version,
        });
    }

    /**
     * Note: This is also called when preview, properties or object properties are opened.
     * The editMode parameter is used to handle the distinctive cases.
     */
    @ActionDefinition(EditItemAction)
    handleEditItemAction(ctx: StateContext<EditorState>, action: EditItemAction): void {
        ctx.setState(patch({
            // Static changes
            contentModified: false,
            editorIsOpen: true,
            editorIsFocused: true,
            objectPropertiesModified: false,
            oldVersion: undefined,
            version: undefined,
            uploadInProgress: false,
            // Action values
            compareWithId: action.settings.compareWithId,
            editMode: action.settings.editMode,
            itemId: action.settings.itemId,
            itemType: action.settings.itemType,
            nodeId: action.settings.nodeId,
            openTab: action.settings.openTab,
            openPropertiesTab: action.settings.openPropertiesTab,
            // Only set the focus-mode, if provided.
            focusMode: iif(typeof action.settings.focusMode === 'boolean', action.settings.focusMode),
        }));
    }

    @ActionDefinition(FocusEditorAction)
    handleFocusEditorAction(ctx: StateContext<EditorState>, action: FocusEditorAction): void {
        const state = ctx.getState();

        ctx.setState(patch({
            editorIsFocused: iif(state.editorIsOpen, true),
        }));
    }

    @ActionDefinition(FocusListAction)
    handleFocusListAction(ctx: StateContext<EditorState>, action: FocusListAction): void {
        ctx.patchState({
            editorIsFocused: false,
        });
    }

    @ActionDefinition(SetFocusModeAction)
    handleSetFocusModeAction(ctx: StateContext<EditorState>, action: SetFocusModeAction): void {
        ctx.patchState({
            focusMode: action.enabled,
        });
    }

    @ActionDefinition(SetUploadStatusAction)
    handleSetUploadStatusAction(ctx: StateContext<EditorState>, action: SetUploadStatusAction): void {
        ctx.patchState({
            uploadInProgress: action.inProgress,
        });
    }

    @ActionDefinition(PreviewPageVersionAction)
    handlePreviewPageVersionAction(ctx: StateContext<EditorState>, action: PreviewPageVersionAction): void {
        ctx.patchState({
            // Static changes
            compareWithId: undefined,
            contentModified: false,
            editMode: EditMode.PREVIEW_VERSION,
            editorIsOpen: true,
            editorIsFocused: true,
            itemType: 'page',
            objectPropertiesModified: false,
            oldVersion: undefined,
            uploadInProgress: false,
            // Action values
            itemId: action.pageId,
            nodeId: action.nodeId,
            version: action.version,
        });
    }

    @ActionDefinition(MarkContentAsModifiedAction)
    handleMarkContentAsModifiedAction(ctx: StateContext<EditorState>, action: MarkContentAsModifiedAction): void {
        ctx.patchState({
            contentModified: action.isModified,
        });
    }

    @ActionDefinition(MarkObjectPropertiesAsModifiedAction)
    handleMarkObjectPropertiesAsModifiedAction(ctx: StateContext<EditorState>, action: MarkObjectPropertiesAsModifiedAction): void {
        ctx.patchState({
            objectPropertiesModified: action.isModified,
            modifiedObjectPropertiesValid: action.isValid,
        });
    }

    @ActionDefinition(StartSavingAction)
    handleStartSavingAction(ctx: StateContext<EditorState>, action: StartSavingAction): void {
        ctx.patchState({
            saving: true,
        });
    }

    @ActionDefinition(SaveSuccessAction)
    handleSaveSuccessAction(ctx: StateContext<EditorState>, action: SaveSuccessAction): void {
        ctx.patchState({
            saving: false,
        });
    }

    @ActionDefinition(SaveErrorAction)
    handleSaveErrorAction(ctx: StateContext<EditorState>, action: SaveErrorAction): void {
        ctx.patchState({
            saving: false,
        });
    }

}
