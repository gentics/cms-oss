export const ACTION_BUTTON_WIDTH = 36;
export const ACTION_BUTTON_PADDING = 14;

export const LIST_LOADER = Symbol();
export const LOAD_BACKGROUND = Symbol();

export enum ListId {
    SCHEDULE = 'schedule',
    SCHEDULE_TASK = 'schedule-task',
    SCHEDULE_EXECUTION = 'schedule-execution',
}

export enum ObjectPropertyListColumn {
    GLOBAL_ID,
    KEYWORD,
    NAME,
    DESCRIPTION,
    CATEGORY,
    TYPE,
    CONSTRUCT,
    REQUIRED,
    INHERITABLE,
    SYNC_CONTENT_SET,
    SYNC_CHANNEL_SET,
    SYNC_VARIANTS,
}

export const DEFAULT_CLICKABLE_OBJECT_PROPERTY_LIST_COLUMNS: ObjectPropertyListColumn[] = [
    ObjectPropertyListColumn.GLOBAL_ID,
    ObjectPropertyListColumn.KEYWORD,
    ObjectPropertyListColumn.NAME,
    ObjectPropertyListColumn.DESCRIPTION,
    ObjectPropertyListColumn.TYPE,
    ObjectPropertyListColumn.REQUIRED,
    ObjectPropertyListColumn.INHERITABLE,
    ObjectPropertyListColumn.SYNC_CONTENT_SET,
    ObjectPropertyListColumn.SYNC_CHANNEL_SET,
    ObjectPropertyListColumn.SYNC_VARIANTS,
];

export enum ConstructListColumn {
    ICON,
    NAME,
    KEYWORD,
    NEW_EDITOR,
    EXTERNAL_EDITOR_URL,
    MAY_BE_SUBTAG,
    MAY_CONTAIN_SUBTAGS,
    CONSTRUCT,
    DESCRIPTION,
    CREATOR,
    CREATION_DATE,
    EDITOR,
    EDIT_DATE,
    EDIT_DO,
    CATEGORY,
    CATEGORY_SORT_ORDER,
    VISIBLE_IN_MENU,
}

export const DEFAULT_CLICKABLE_CONSTRUCT_LIST_COLUMNS: ConstructListColumn[] = [
    ConstructListColumn.ICON,
    ConstructListColumn.NAME,
    ConstructListColumn.KEYWORD,
    ConstructListColumn.NEW_EDITOR,
    ConstructListColumn.EXTERNAL_EDITOR_URL,
    ConstructListColumn.MAY_BE_SUBTAG,
    ConstructListColumn.MAY_CONTAIN_SUBTAGS,
    ConstructListColumn.CONSTRUCT,
    ConstructListColumn.DESCRIPTION,
    ConstructListColumn.CREATION_DATE,
    ConstructListColumn.EDIT_DATE,
    ConstructListColumn.EDIT_DO,
    ConstructListColumn.CATEGORY_SORT_ORDER,
    ConstructListColumn.VISIBLE_IN_MENU,
];

export enum ScheduleListColumn {
    ID,
    NAME,
    DESCRIPTION,
    ACTIVE,
    TYPE,
    TASK,
    STATUS,
    EXECUTION_START,
    EXECUTION_RESULT,
    EXECUTION_DURATION,
}

export const DEFAULT_CLICKABLE_SCHEDULE_LIST_COLUMNS: ScheduleListColumn[] = [
    ScheduleListColumn.ID,
    ScheduleListColumn.NAME,
    ScheduleListColumn.DESCRIPTION,
    ScheduleListColumn.ACTIVE,
    ScheduleListColumn.TYPE,
    ScheduleListColumn.STATUS,
    ScheduleListColumn.EXECUTION_START,
    ScheduleListColumn.EXECUTION_DURATION,
];

export enum ContentPackageListColumn {
    NAME,
    DESCRIPTION,
    EDIT_DATE,
}

export const DEFAULT_CLICKABLE_CONTENT_PACKAGE_LIST_COLUMNS: ContentPackageListColumn[] = [
    ContentPackageListColumn.NAME,
    ContentPackageListColumn.DESCRIPTION,
    ContentPackageListColumn.EDIT_DATE,
];

export enum TemplateListColumn {
    GLOBAL_ID,
    NAME,
    DESCRIPTION,
    CREATOR,
    CREATION_DATE,
    EDITOR,
    EDIT_DATE,
    LOCKED,
    INHERITED,
    MASTER,
    MASTER_ID,
    FOLDER_ID,
    PATH,
    INHERITED_FROM,
    MASTER_NODE,
    SOURCE,
    CHANNEL_ID,
    CHANNEL_SET_ID,
}

export const DEFAULT_CLICKABLE_TEMPLATE_LIST_COLUMNS: TemplateListColumn[] = [
    TemplateListColumn.GLOBAL_ID,
    TemplateListColumn.NAME,
    TemplateListColumn.DESCRIPTION,
    TemplateListColumn.CREATOR,
    TemplateListColumn.CREATION_DATE,
    TemplateListColumn.EDITOR,
    TemplateListColumn.EDIT_DATE,
    TemplateListColumn.LOCKED,
    TemplateListColumn.INHERITED,
    TemplateListColumn.MASTER,
    TemplateListColumn.MASTER_ID,
    TemplateListColumn.FOLDER_ID,
    TemplateListColumn.PATH,
    TemplateListColumn.INHERITED_FROM,
    TemplateListColumn.MASTER_NODE,
    TemplateListColumn.SOURCE,
    TemplateListColumn.CHANNEL_ID,
    TemplateListColumn.CHANNEL_SET_ID,
];
