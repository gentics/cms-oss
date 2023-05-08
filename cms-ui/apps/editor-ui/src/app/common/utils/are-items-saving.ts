import { AppState } from '../../common/models';

export function areItemsSaving(state: AppState): boolean {
    const folderState = state.folder;
    return state.editor.saving
        || folderState.folders.saving
        || folderState.pages.saving
        || folderState.files.saving
        || folderState.images.saving;
}
