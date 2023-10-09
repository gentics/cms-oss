export enum DefaultEditorControlTabs {
    FORMATTING = 'formatting',
    CONSTRUCTS = 'constructs',
}

export interface PageEditorTab {
    id: string;
    icon?: string;
    label: string;
    editorUrl: string;
    hidden: boolean;
    disabled: boolean;
}
