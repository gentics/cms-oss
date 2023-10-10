import { EditMode, FolderItemType, PageVersion } from '@gentics/cms-models';
import { AppState, EditorTab, ITEM_PROPERTIES_TAB, PropertiesTab } from '../../../common/models';
import { ActionDeclaration } from '../../state-utils';

export interface EditorStateUrlParams {
    nodeId: number;
    type: FolderItemType;
    itemId: number;
    editMode: EditMode;
    options?: string;
}

export interface EditorStateUrlOptions {
    version?: PageVersion;
    oldVersion?: PageVersion;
    compareWithId?: number;
    openTab?: EditorTab;
    propertiesTab?: PropertiesTab;
    readOnly?: boolean;
}

export interface EditSettings {
    compareWithId?: number,
    editMode?: EditMode,
    itemId: number,
    itemType: 'folder' | 'page' | 'image' | 'file' | 'form',
    nodeId: number,
    openTab?: EditorTab,
    openPropertiesTab?: PropertiesTab,
    focusMode?: boolean;
}

export const EDITOR_STATE_KEY: keyof AppState = 'editor';

@ActionDeclaration(EDITOR_STATE_KEY)
export class ChangeTabAction {
    constructor(
        public tab: EditorTab,
        public propertiesTab: PropertiesTab = ITEM_PROPERTIES_TAB,
    ) {}
}

@ActionDeclaration(EDITOR_STATE_KEY)
export class CancelEditingAction {}

@ActionDeclaration(EDITOR_STATE_KEY)
export class CloseEditorAction {}

@ActionDeclaration(EDITOR_STATE_KEY)
export class SetOpenObjectPropertyGroupsAction {
    constructor(
        public groups: string[],
    ) {}
}

@ActionDeclaration(EDITOR_STATE_KEY)
export class AddExpandedTabGroupAction {
    constructor(
        public group: string,
    ) {}
}

@ActionDeclaration(EDITOR_STATE_KEY)
export class RemoveExpandedTabGroupAction {
    constructor(
        public group: string,
    ) {}
}

@ActionDeclaration(EDITOR_STATE_KEY)
export class ComparePageVersionsAction {
    constructor(
        public itemId: number,
        public nodeId: number,
        public oldVersion: PageVersion,
        public version: PageVersion,
    ) {}
}

@ActionDeclaration(EDITOR_STATE_KEY)
export class ComparePageVersionSourcesAction {
    constructor(
        public itemId: number,
        public nodeId: number,
        public oldVersion: PageVersion,
        public version: PageVersion,
    ) {}
}

@ActionDeclaration(EDITOR_STATE_KEY)
export class EditItemAction {
    constructor(
        public settings: EditSettings,
    ) {}
}

@ActionDeclaration(EDITOR_STATE_KEY)
export class FocusEditorAction {}

@ActionDeclaration(EDITOR_STATE_KEY)
export class FocusListAction {}

@ActionDeclaration(EDITOR_STATE_KEY)
export class SetFocusModeAction {
    constructor(
        public enabled: boolean,
    ) {}
}

@ActionDeclaration(EDITOR_STATE_KEY)
export class SetUploadStatusAction {
    constructor(
        public inProgress: boolean,
    ) {}
}

@ActionDeclaration(EDITOR_STATE_KEY)
export class PreviewPageVersionAction {
    constructor(
        public pageId: number,
        public nodeId: number,
        public version: PageVersion,
    ) {}
}

@ActionDeclaration(EDITOR_STATE_KEY)
export class MarkContentAsModifiedAction {
    constructor(
        public isModified: boolean,
    ) {}
}

@ActionDeclaration(EDITOR_STATE_KEY)
export class MarkObjectPropertiesAsModifiedAction {
    constructor(
        public isModified: boolean,
        public isValid: boolean,
    ) {}
}

@ActionDeclaration(EDITOR_STATE_KEY)
export class StartSavingAction {}

@ActionDeclaration(EDITOR_STATE_KEY)
export class SaveSuccessAction {}

@ActionDeclaration(EDITOR_STATE_KEY)
export class SaveErrorAction {
    constructor(
        public errorMessage: string,
    ) {}
}
