import {
    ActionLogEntry,
    ConstructCategory,
    ContentPackage,
    ContentRepository,
    ContentRepositoryFragment,
    DataSource,
    DataSourceEntry,
    DirtQueueEntry,
    ElasticSearchIndex,
    File,
    Folder,
    Form,
    GcmsPermission,
    Group,
    Image,
    IndexByKey,
    Language,
    Node,
    ObjectProperty,
    ObjectPropertyCategory,
    Package,
    Page,
    PermissionInfo,
    PermissionsSet,
    Raw,
    Role,
    Schedule,
    ScheduleExecution,
    ScheduleTask,
    TagStatus,
    TagType,
    TagmapEntry,
    Template,
    TemplateTag,
    User,
    PackageDependencyEntity,
    ContentPackageImportError,
    ContentRepositoryLicense,

} from '@gentics/cms-models';
import { Permission } from '@gentics/mesh-models';

/** Symbol to access the permissions of an object */
export const BO_PERMISSIONS = Symbol('bo-permissions');
/** Symbol to access the internally used id of the object */
export const BO_ID = Symbol('bo-id');
/** Sombol to access the display name of the object. */
export const BO_DISPLAY_NAME = Symbol('bo-display-name');

export const BO_ORIGINAL_SORT_ORDER = Symbol('bo-original-order');
export const BO_NEW_SORT_ORDER = Symbol('bo-new-order');

export const BO_NODE_ID = Symbol('bo-node-id');

/**
 * A Business-Object is a regular CMS object with additional properties applied.
 * These properties are only relevant in the context of the application and may
 * not be posted to any CMS Endpoints.
 * Which is why properties are defined with symbols to exclude them from the json
 * encoding in the first place.
 *
 * Currently BOs are defined in the `cms-models` package, where they don't belong,
 * as BOs are application dependent and should not be in the package to begin with.
 * Cleanup has to be postponed due to the amount of changes required.
 */
export interface BusinessObject {
    /**
     * The internally used ID for this object.
     * Usually the id (string | number), or another identifier if nothing else is applicable.
     */
    [BO_ID]: string;
    /** The permissions the user has to this object */
    [BO_PERMISSIONS]: (GcmsPermission | Permission)[];
    /** The display name to use for this object */
    [BO_DISPLAY_NAME]: string;
}

export interface SortableBusinessObject extends BusinessObject {
    [BO_ORIGINAL_SORT_ORDER]: number;
    [BO_NEW_SORT_ORDER]: number;
}

export type UserBO = User<Raw> & BusinessObject;
export type GroupBO = Group<Raw> & BusinessObject & {
    permissionSet?: PermissionsSet;
    categorizedPerms?: IndexByKey<PermissionInfo[]>;
};
export type DataSourceBO = DataSource<Raw> & BusinessObject;
export type DataSourceEntryBO = DataSourceEntry & SortableBusinessObject;
export type RoleBO = Role<Raw> & BusinessObject;
export type LanguageBO = Language & SortableBusinessObject;
export type NodeBO = Node<Raw> & BusinessObject;
export type ContentRepositoryBO = ContentRepository & BusinessObject;
export type TagMapEntryBO = TagmapEntry & BusinessObject;
export type ContentRepositoryFragmentBO = ContentRepositoryFragment & BusinessObject;
export type ConstructBO = TagType & BusinessObject;
export type ConstructCategoryBO = ConstructCategory & SortableBusinessObject;
export type FolderBO = Folder & BusinessObject;
export type PermissionsSetBO = PermissionsSet & BusinessObject & {
    group: Group,
    categorized: IndexByKey<PermissionInfo[]>;
};
export type ContentPackageBO = ContentPackage & BusinessObject;
export type ImportErrorBO = ContentPackageImportError & BusinessObject;
export type DirtQueueItemBO = DirtQueueEntry & BusinessObject;
export type ElasticSearchIndexBO = ElasticSearchIndex<Raw> & BusinessObject;
export type ActionLogEntryBO = ActionLogEntry & BusinessObject;
export type ObjectPropertyBO = ObjectProperty & BusinessObject;
export type ObjectPropertyCategoryBO = ObjectPropertyCategory & BusinessObject;
export type DevToolPackageBO = Package & BusinessObject;
export type TemplateBO = Template & BusinessObject;
export type TemplateTagBO = TemplateTag & BusinessObject;
export type TagStatusBO = TagStatus & BusinessObject;
export type ScheduleExecutionBO = ScheduleExecution & BusinessObject;
export type ScheduleBO = Schedule & BusinessObject;
export type ScheduleTaskBO = ScheduleTask & BusinessObject;
export type ContentRepositoryLicenseBO = ContentRepositoryLicense & BusinessObject;

export type PageBO = Page & BusinessObject;
export type FileBO = File & BusinessObject;
export type ImageBO = Image & BusinessObject;
export type FormBO = Form & BusinessObject;

export type ContentItem = Node<Raw> | Folder | Page | File | Image | Form | Template;
export type ContentItemBO = (NodeBO | FolderBO | PageBO | FileBO | ImageBO | FormBO | TemplateBO) & {
    [BO_NODE_ID]: number;
};
export type ContentItemTypes = (Pick<ContentItemBO, 'type'>['type']);
export const CONTENT_ITEM_TYPES: ContentItemTypes[] = [
    'node',
    'channel',
    'folder',
    'page',
    'file',
    'image',
    'form',
    'template',
];

export type PackageDependencyEntityBO = PackageDependencyEntity & BusinessObject;
