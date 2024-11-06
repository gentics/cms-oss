export const GCMSUI_EDITOR_TABS_NAMESPACE = 'gcmsui.';

export enum DefaultEditorControlTabs {
    FORMATTING = 'formatting',
    CONSTRUCTS = `${GCMSUI_EDITOR_TABS_NAMESPACE}constructs`,
}

export const INVERSE_DEFAULT_EDITOR_TABS_MAPPING = Object.entries(DefaultEditorControlTabs).reduce((acc, entry) => {
    acc[entry[1]] = entry[0];
    return acc;
}, {});

export interface PageEditorTab {
    id: string;
    icon?: string;
    label: string;
    editorUrl: string;
    hidden: boolean;
    disabled: boolean;
}
