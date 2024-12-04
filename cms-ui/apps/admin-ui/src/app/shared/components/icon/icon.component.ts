import { ChangeDetectionStrategy, Component, Input, OnChanges } from '@angular/core';
import { GcmsPermission } from '@gentics/cms-models';
import { ChangesOf } from '@gentics/ui-core';

const PERM_PREFIX = 'perm.';

export type GtxIcon =
    'content-node' |
    'node-management' |
    'node' |
    'channel' |
    'inbox' |
    'queue' |
    'administration' |
    'page' |
    'form' |
    'pagecontent' |
    'user-management' |
    'user' |
    'folder-management' |
    'group-management' |
    'group' |
    'role-management' |
    'role' |
    'operations' |
    'auto-update' |
    'licensing' |
    'system-maintenance' |
    'error' |
    'import-export' |
    'import' |
    'export' |
    'search-index-maintenance' |
    'logs' |
    'datasource' |
    'scheduler' |
    'content-admin' |
    'object-properties' |
    'object-properties-folder' |
    'object-properties-templates' |
    'object-properties-pages' |
    'object-properties-images' |
    'object-properties-files' |
    'object-properties-categories' |
    'object-properties-maintenance' |
    'tagtypes' |
    'tagtype-definitions' |
    'nodes' |
    'tagtype-categories' |
    'definitions-categories' |
    'datasources' |
    'languages' |
    'maintenance' |
    'content-repositories' |
    'packages' |
    'cr-fragments' |
    'crfragment' |
    'template-management' |
    'tools' |
    'boolean-true' |
    'boolean-false' |
    'unspecified' |
    'publish_done' |
    'publish_start' |
    string;

interface IconDefinition {
    matIconPrimary: string;
    matIconSecondary: string;
}

// tslint:disable: object-literal-key-quotes
export const ICON_DEFINITIONS: Record<GtxIcon, IconDefinition> = {
    'content-node': {
        matIconPrimary: 'device_hub',
        matIconSecondary: 'settings',
    },
    'node-management': {
        matIconPrimary: 'device_hub',
        matIconSecondary: null,
    },
    node: {
        matIconPrimary: 'device_hub',
        matIconSecondary: null,
    },
    channel: {
        matIconPrimary: 'input',
        matIconSecondary: null,
    },
    inbox: {
        matIconPrimary: 'mail',
        matIconSecondary: null,
    },
    queue: {
        matIconPrimary: 'playlist_add_check',
        matIconSecondary: null,
    },
    administration: {
        matIconPrimary: 'tune',
        matIconSecondary: null,
    },
    page: {
        matIconPrimary: 'subject',
        matIconSecondary: null,
    },
    file: {
        matIconPrimary: 'insert_drive_file',
        matIconSecondary: null,
    },
    image: {
        matIconPrimary: 'photo',
        matIconSecondary: null,
    },
    form: {
        matIconPrimary: 'list_alt',
        matIconSecondary: null,
    },
    template: {
        matIconPrimary: 'view_quilt',
        matIconSecondary: null,
    },
    pagecontent: {
        matIconPrimary: 'newspaper',
        matIconSecondary: null,
    },
    'user-management': {
        matIconPrimary: 'person',
        matIconSecondary: 'settings',
    },
    user: {
        matIconPrimary: 'person',
        matIconSecondary: null,
    },
    'folder-management': {
        matIconPrimary: 'folder',
        matIconSecondary: 'settings',
    },
    'group-management': {
        matIconPrimary: 'group',
        matIconSecondary: 'settings',
    },
    group: {
        matIconPrimary: 'group',
        matIconSecondary: null,
    },
    'role-management': {
        matIconPrimary: 'person',
        matIconSecondary: 'local_offer',
    },
    role: {
        matIconPrimary: 'local_offer',
        matIconSecondary: 'settings',
    },
    operations: {
        matIconPrimary: 'assessment',
        matIconSecondary: 'settings',
    },
    'auto-update': {
        matIconPrimary: 'autorenew',
        matIconSecondary: null,
    },
    'licensing': {
        matIconPrimary: 'license',
        matIconSecondary: null,
    },
    'system-maintenance': {
        matIconPrimary: 'folder',
        matIconSecondary: 'build',
    },
    error: {
        matIconPrimary: 'error',
        matIconSecondary: null,
    },
    'import-export': {
        matIconPrimary: 'swap_vertical_circle',
        matIconSecondary: null,
    },
    import: {
        matIconPrimary: 'archive',
        matIconSecondary: null,
    },
    'search-index-maintenance': {
        matIconPrimary: 'search',
        matIconSecondary: 'settings',
    },
    export: {
        matIconPrimary: 'unarchive',
        matIconSecondary: null,
    },
    logs: {
        matIconPrimary: 'featured_play_list',
        matIconSecondary: null,
    },
    scheduler: {
        matIconPrimary: 'calendar_today',
        matIconSecondary: null,
    },
    schedulerschedule: {
        matIconPrimary: 'event',
        matIconSecondary: null,
    },
    schedulerscheduleadmin: {
        matIconPrimary: 'calendar_today',
        matIconSecondary: null,
    },
    'content-admin': {
        matIconPrimary: 'content',
        matIconSecondary: 'settings',
    },
    'object-properties': {
        matIconPrimary: 'vertical_split',
        matIconSecondary: null,
    },
    'object-properties-folder': {
        matIconPrimary: 'vertical_split',
        matIconSecondary: 'folder',
    },
    'object-properties-templates': {
        matIconPrimary: 'vertical_split',
        matIconSecondary: 'view_quilt',
    },
    'object-properties-pages': {
        matIconPrimary: 'vertical_split',
        matIconSecondary: 'subject',
    },
    'object-properties-images': {
        matIconPrimary: 'vertical_split',
        matIconSecondary: 'insert-Photo',
    },
    'object-properties-files': {
        matIconPrimary: 'vertical_split',
        matIconSecondary: 'insert_drive_file',
    },
    'object-properties-categories': {
        matIconPrimary: 'vertical_split',
        matIconSecondary: 'category',
    },
    'object-properties-maintenance': {
        matIconPrimary: 'vertical_split',
        matIconSecondary: 'build',
    },
    tagtypes: {
        matIconPrimary: 'web',
        matIconSecondary: null,
    },
    construct: {
        matIconPrimary: 'web',
        matIconSecondary: null,
    },
    'tagtype-definitions': {
        matIconPrimary: 'web',
        matIconSecondary: 'edit',
    },
    nodes: {
        matIconPrimary: 'device_hub',
        matIconSecondary: null,
    },
    'tagtype-categories': {
        matIconPrimary: 'web',
        matIconSecondary: 'category',
    },
    'definitions-categories': {
        matIconPrimary: 'category',
        matIconSecondary: null,
    },
    datasources: {
        matIconPrimary: 'view_list',
        matIconSecondary: null,
    },
    languages: {
        matIconPrimary: 'language',
        matIconSecondary: null,
    },
    maintenance: {
        matIconPrimary: 'folder',
        matIconSecondary: 'settings',
    },
    'content-repositories': {
        matIconPrimary: 'storage',
        matIconSecondary: null,
    },
    packages: {
        matIconPrimary: 'developer_board',
        matIconSecondary: null,
    },
    'cr-fragments': {
        matIconPrimary: 'dns',
        matIconSecondary: null,
    },
    'template-management': {
        matIconPrimary: 'view_quilt',
        matIconSecondary: null,
    },
    tools: {
        matIconPrimary: 'extension',
        matIconSecondary: null,
    },
    setting: {
        matIconPrimary: 'settings_applications',
        matIconSecondary: null,
    },
    pubqueue: {
        matIconPrimary: 'playlist_add_check',
        matIconSecondary: null,
    },
    admin: {
        matIconPrimary: 'tune',
        matIconSecondary: null,
    },
    content: {
        matIconPrimary: 'device_hub',
        matIconSecondary: 'settings',
    },
    useradmin: {
        matIconPrimary: 'person',
        matIconSecondary: null,
    },
    groupadmin: {
        matIconPrimary: 'people',
        matIconSecondary: null,
    },
    autoupdate: {
        matIconPrimary: 'loop',
        matIconSecondary: null,
    },
    contentadmin: {
        matIconPrimary: 'folder',
        matIconSecondary: 'settings',
    },
    folder: {
        matIconPrimary: 'folder',
        matIconSecondary: null,
    },
    systemmaintenance: {
        matIconPrimary: 'block',
        matIconSecondary: null,
    },
    bundleadmin: {
        matIconPrimary: 'swap_vertical_circle',
        matIconSecondary: null,
    },
    errorlog: {
        matIconPrimary: 'error',
        matIconSecondary: null,
    },
    actionlog: {
        matIconPrimary: 'featured_play_list',
        matIconSecondary: null,
    },
    customtooladmin: {
        matIconPrimary: 'extension',
        matIconSecondary: null,
    },
    customtool: {
        matIconPrimary: 'extension',
        matIconSecondary: null,
    },
    workflowadmin: {
        matIconPrimary: 'assessment',
        matIconSecondary: 'settings',
    },
    objpropadmin: {
        matIconPrimary: 'vertical_split',
        matIconSecondary: null,
    },
    objproptype: {
        matIconPrimary: 'vertical_split',
        matIconSecondary: null,
    },
    objprop: {
        matIconPrimary: 'vertical_split',
        matIconSecondary: null,
    },
    objpropcategory: {
        matIconPrimary: 'vertical_split',
        matIconSecondary: 'category',
    },
    objpropmaintenance: {
        matIconPrimary: 'vertical_split',
        matIconSecondary: 'build',
    },
    constructadmin: {
        matIconPrimary: 'web',
        matIconSecondary: null,
    },
    datasourceadmin: {
        matIconPrimary: 'view_list',
        matIconSecondary: null,
    },
    languageadmin: {
        matIconPrimary: 'language',
        matIconSecondary: null,
    },
    contentrepositoryadmin: {
        matIconPrimary: 'storage',
        matIconSecondary: null,
    },
    contentrepository: {
        matIconPrimary: 'storage',
        matIconSecondary: null,
    },
    contentmapbrowser: {
        matIconPrimary: 'storage',
        matIconSecondary: null,
    },
    devtooladmin: {
        matIconPrimary: 'developer_board',
        matIconSecondary: null,
    },
    contentrepositoryfragmentadmin: {
        matIconPrimary: 'dns',
        matIconSecondary: null,
    },
    tasktemplateadmin: {
        matIconPrimary: 'assignment',
        matIconSecondary: null,
    },
    taskadmin: {
        matIconPrimary: 'library_books',
        matIconSecondary: null,
    },
    jobadmin: {
        matIconPrimary: 'event',
        matIconSecondary: null,
    },
    tasktemplate: {
        matIconPrimary: 'assignment',
        matIconSecondary: null,
    },
    task: {
        matIconPrimary: 'library_books',
        matIconSecondary: null,
    },
    schedulertask: {
        matIconPrimary: 'library_books',
        matIconSecondary: null,
    },
    schedulertaskadmin: {
        matIconPrimary: 'library_books',
        matIconSecondary: null,
    },
    job: {
        matIconPrimary: 'event',
        matIconSecondary: null,
    },
    constructcategoryadmin: {
        matIconPrimary: 'web',
        matIconSecondary: 'category',
    },
    constructcategory: {
        matIconPrimary: 'web',
        matIconSecondary: 'category',
    },
    usersnap: {
        matIconPrimary: 'extension',
        matIconSecondary: null,
    },
    bundleexport: {
        matIconPrimary: 'unarchive',
        matIconSecondary: null,
    },
    bundleimport: {
        matIconPrimary: 'archive',
        matIconSecondary: null,
    },
    'boolean-true': {
        matIconPrimary: 'done',
        matIconSecondary: null,
    },
    'boolean-false': {
        matIconPrimary: 'clear',
        matIconSecondary: null,
    },
    // eslint-disable-next-line @typescript-eslint/naming-convention
    publish_done: {
        matIconPrimary: 'calendar_today',
        matIconSecondary: 'check_circle',
    },
    // eslint-disable-next-line @typescript-eslint/naming-convention
    publish_start: {
        matIconPrimary: 'calendar_today',
        matIconSecondary: 'play_circle',
    },
    datasource: {
        matIconPrimary: 'view_list',
        matIconSecondary: null,
    },
    crfragment: {
        matIconPrimary: 'dns',
        matIconSecondary: null,
    },
    mesh: {
        matIconPrimary: 'hub',
        matIconSecondary: null,
    },
    license: {
        matIconPrimary: 'license',
        matIconSecondary: null,
    },

    [PERM_PREFIX + GcmsPermission.READ]: {
        matIconPrimary: 'remove_red_eye',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.CREATE]: {
        matIconPrimary: 'add',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.DELETE]: {
        matIconPrimary: 'delete',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.UPDATE]: {
        matIconPrimary: 'edit',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.SET_PERMISSION]: {
        matIconPrimary: 'person',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.READ_ITEMS]: {
        matIconPrimary: 'remove_red_eye',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.CREATE_ITEMS]: {
        matIconPrimary: 'add',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.CREATE_USER]: {
        matIconPrimary: 'add',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.UPDATE_USER]: {
        matIconPrimary: 'edit',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.DELETE_USER]: {
        matIconPrimary: 'delete',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.CREATE_GROUP]: {
        matIconPrimary: 'add',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.UPDATE_GROUP]: {
        matIconPrimary: 'edit',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.DELETE_GROUP]: {
        matIconPrimary: 'delete',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.CREATE_IMPORT]: {
        matIconPrimary: 'archive',
        matIconSecondary: 'add',
    },
    [PERM_PREFIX + GcmsPermission.CREATE_EXPORT]: {
        matIconPrimary: 'unarchive',
        matIconSecondary: 'add',
    },
    [PERM_PREFIX + GcmsPermission.UPDATE_ITEMS]: {
        matIconPrimary: 'loop',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.DELETE_ITEMS]: {
        matIconPrimary: 'delete',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.IMPORT_ITEMS]: {
        matIconPrimary: 'archive',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.PUBLISH_PAGES]: {
        matIconPrimary: 'cloud_upload',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.UPDATE_FOLDER]: {
        matIconPrimary: 'edit',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.DELETE_FOLDER]: {
        matIconPrimary: 'delete',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.READ_TEMPLATES]: {
        matIconPrimary: 'remove_red_eye',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.CREATE_TEMPLATES]: {
        matIconPrimary: 'add',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.UPDATE_TEMPLATES]: {
        matIconPrimary: 'edit',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.DELETE_TEMPLATES]: {
        matIconPrimary: 'delete',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.LINK_TEMPLATES]: {
        matIconPrimary: 'link',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.VIEW_FORM]: {
        matIconPrimary: 'remove_red_eye',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.CREATE_FORM]: {
        matIconPrimary: 'add',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.UPDATE_FORM]: {
        matIconPrimary: 'edit',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.DELETE_FORM]: {
        matIconPrimary: 'delete',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.PUBLISH_FORM]: {
        matIconPrimary: 'cloud_upload',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.FORM_REPORTS]: {
        matIconPrimary: 'list_alt',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.UPDATE_CONSTRUCTS]: {
        matIconPrimary: 'web',
        matIconSecondary: 'loop',
    },
    [PERM_PREFIX + GcmsPermission.CHANNEL_SYNC]: {
        matIconPrimary: 'file_upload',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.UPDATE_INHERITANCE]: {
        matIconPrimary: 'input',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.WASTE_BIN]: {
        matIconPrimary: 'delete_sweep',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.INSTANT_MESSAGES]: {
        matIconPrimary: 'mail',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.ASSIGN_ROLES]: {
        matIconPrimary: 'people',
        matIconSecondary: 'insert_link',
    },
    [PERM_PREFIX + GcmsPermission.UPDATE_GROUP_USER]: {
        matIconPrimary: 'person',
        matIconSecondary: 'loop',
    },
    [PERM_PREFIX + GcmsPermission.DELETE_ERROR_LOG]: {
        matIconPrimary: 'delete',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.UPDATE_IMPORT]: {
        matIconPrimary: 'archive',
        matIconSecondary: 'loop',
    },
    [PERM_PREFIX + GcmsPermission.EDIT_IMPORT]: {
        matIconPrimary: 'archive',
        matIconSecondary: 'edit',
    },
    [PERM_PREFIX + GcmsPermission.UPDATE_EXPORT]: {
        matIconPrimary: 'unarchive',
        matIconSecondary: 'loop',
    },
    [PERM_PREFIX + GcmsPermission.SET_BUNDLE_PERMISSIONS]: {
        matIconPrimary: 'swap_vertical_circle',
        matIconSecondary: 'vpn_key',
    },
    [PERM_PREFIX + GcmsPermission.SYSTEM_INFORMATION]: {
        matIconPrimary: 'remove_red_eye',
        matIconSecondary: null,
    },
    [PERM_PREFIX + GcmsPermission.SUSPEND_SCHEDULER]: {
        matIconPrimary: 'calendar_today',
        matIconSecondary: 'pause',
    },
    [PERM_PREFIX + GcmsPermission.USER_ASSIGNMENT]: {
        matIconPrimary: 'person',
        matIconSecondary: 'insert_link',
    },
    [PERM_PREFIX + GcmsPermission.SET_USER_PERMISSIONS]: {
        matIconPrimary: 'people',
        matIconSecondary: 'vpn_key',
    },
    [PERM_PREFIX + GcmsPermission.BUILD_EXPORT]: {
        matIconPrimary: 'unarchive',
        matIconSecondary: 'insert_drive_file',
    },
    [PERM_PREFIX + GcmsPermission.UPDATE_BUNDLE]: {
        matIconPrimary: 'swap_vertical_circle',
        matIconSecondary: 'loop',
    },
    [PERM_PREFIX + GcmsPermission.DELETE_BUNDLE]: {
        matIconPrimary: 'swap_vertical_circle',
        matIconSecondary: 'delete',
    },
    [PERM_PREFIX + GcmsPermission.READ_TASKS]: {
        matIconPrimary: 'library_books',
        matIconSecondary: 'remove_red_eye',
    },
    [PERM_PREFIX + GcmsPermission.UPDATE_TASKS]: {
        matIconPrimary: 'library_books',
        matIconSecondary: 'loop',
    },
    [PERM_PREFIX + GcmsPermission.READ_JOBS]: {
        matIconPrimary: 'event',
        matIconSecondary: 'remove_red_eye',
    },
    [PERM_PREFIX + GcmsPermission.UPDATE_JOBS]: {
        matIconPrimary: 'event',
        matIconSecondary: 'loop',
    },
    [PERM_PREFIX + GcmsPermission.READ_TASK_TEMPLATE]: {
        matIconPrimary: 'assignment',
        matIconSecondary: 'remove_red_eye',
    },
    [PERM_PREFIX + GcmsPermission.UPDATE_TASK_TEMPLATES]: {
        matIconPrimary: 'assignment',
        matIconSecondary: 'loop',
    },
    [PERM_PREFIX + GcmsPermission.READ_SCHEDULES]: {
        matIconPrimary: 'calendar_today',
        matIconSecondary: 'remove_red_eye',
    },
    [PERM_PREFIX + GcmsPermission.UPDATE_SCHEDULES]: {
        matIconPrimary: 'calendar_today',
        matIconSecondary: 'loop',
    },

    unspecified: {
        matIconPrimary: 'crop_square',
        matIconSecondary: null,
    },
};

@Component({
    selector: 'gtx-icon',
    templateUrl: './icon.component.html',
    styleUrls: ['./icon.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IconComponent implements OnChanges {

    @Input() name: GtxIcon;

    matIconPrimary: string;
    matIconSecondary: string;

    ngOnChanges(changes: ChangesOf<this>): void {
        if (changes.name) {
            const iconDef = ICON_DEFINITIONS[this.name] || ICON_DEFINITIONS.unspecified;
            this.matIconPrimary = iconDef.matIconPrimary;
            this.matIconSecondary = iconDef.matIconSecondary;
        }
    }

}
