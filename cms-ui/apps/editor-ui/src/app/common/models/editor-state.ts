import { EditMode } from '@gentics/cms-integration-api-models';
import { FolderItemType, PageVersion } from '@gentics/cms-models';

export type EditorTab = 'preview' | 'properties';

export enum EditorOutlet {
    LIST = 'list',
    DETAIL = 'detail',
    MODAL = 'modal',
}

/** Defines the properties tab: either the item's properties or one of its object properties. */
export type PropertiesTab = string;

/** The PropertiesTab with the item's (non object-) properties. */
export const ITEM_PROPERTIES_TAB = 'item-properties';
export const ITEM_REPORTS_TAB = 'item-reports';
export const ITEM_TAG_LIST_TAB = 'item-tag-list';

export interface EditorState {
    editorIsOpen: boolean;
    editorIsFocused: boolean;
    itemType?: FolderItemType | 'node';
    itemId?: number;
    nodeId?: number;
    compareWithId?: number;
    editMode?: EditMode;
    focusMode?: boolean;
    version?: PageVersion;
    oldVersion?: PageVersion;
    fetching: boolean;
    saving: boolean;
    lastError: string;
    openTab: EditorTab;
    openPropertiesTab?: PropertiesTab;
    openObjectPropertyGroups: string[];
    contentModified: boolean;
    objectPropertiesModified: boolean;
    modifiedObjectPropertiesValid: boolean;
    uploadInProgress: boolean;
}
