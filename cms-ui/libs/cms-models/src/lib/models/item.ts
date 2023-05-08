import { Form } from './cms-form';
import { File, File as FileModel } from './file';
import { Folder } from './folder';
import { Image } from './image';
import { Node } from './node';
import { Page } from './page';
import { ChannelSyncRequest } from './request';
import { Template } from './template';
import { DefaultModelType, ModelType, Normalizable, NormalizableEntity, Normalized, Raw } from './type-util';
import { User } from './user';

export interface DeleteInfo {
    /** Date when the object was deleted. */
    at: number;
    /** User who deleted the object. */
    by: User<Raw>;
}

/**
 * This property is not part of the response from the API, but is created only when the "usage" displayField is
 * enabled for a particular itemType. The "total" will be populated when viewing items in the FolderContents list,
 * and the specific types will be populated when viewing the detailed usage for a particular item.
 */
export interface Usage {
    total: number;
    files?: number;
    folders?: number;
    images?: number;
    pages?: number;
    templates?: number;
}

export const folderItemTypes: FolderItemType[] = ['file', 'folder', 'image', 'page'];

/**
 * Types of items that are located in a folder.
 */
export type FolderItemType = 'file' | 'folder' | 'form' | 'image' | 'page';

export type DependencyItemType = 'file' | 'image' | 'page' | 'form';

/** Maps FolderItemType literals to their interfaces for use with TypeScript generics. */
export interface FolderItemTypeMap<T extends ModelType = DefaultModelType> {
    file: File<T>;
    folder: Folder<T>;
    form: Form<T>;
    image: Image<T>;
    page: Page<T>;
}

export type FolderItemOrNodeType = FolderItemType | 'node';
/** Maps FolderItemOrNodeType literals to their interfaces for use with TypeScript generics. */
export interface FolderItemOrNodeTypeMap<T extends ModelType = DefaultModelType> extends FolderItemTypeMap<T> {
    node: Node<T>;
}

export const folderItemTypePlurals: FolderItemTypePlural[] = ['files', 'folders', 'forms', 'images', 'pages'];
export type FolderItemTypePlural = 'files' | 'folders' | 'forms' | 'images' | 'pages';
export type DependencyItemTypePlural = 'files' | 'images' | 'pages' | 'forms';

export type FolderItemOrTemplateType = FolderItemType | 'template';
/** Maps FolderItemOrTemplateType literals to their interfaces for use with TypeScript generics. */
export interface FolderItemOrTemplateTypeMap<T extends ModelType = DefaultModelType> extends FolderItemTypeMap<T> {
    template: Template<T>;
}

export type ItemType = FolderItemOrNodeType | FolderItemOrTemplateType | 'channel';
/** Maps ItemType literals to their interfaces for use with TypeScript generics. */
export interface ItemTypeMap<T extends ModelType = DefaultModelType> extends FolderItemOrTemplateTypeMap<T>, FolderItemOrNodeTypeMap<T> {
    channel: Node<T>;
}

export type UsageType = ItemType | 'variant' | 'tag' | 'linkedPage' | 'linkedFile' | 'linkedImage';

export type ItemNormalized = Page<Normalized> | FileModel<Normalized> | Folder<Normalized> | Form<Normalized> | Image<Normalized> | Node<Normalized>;

/**
 * The base of the various content types.
 *
 * Corresponds to the REST API type `ContentNodeItem`:
 * https://www.gentics.com/Content.Node/guides/restapi/json_ContentNodeItem.html
 */
export interface Item<T extends ModelType = DefaultModelType> extends NormalizableEntity<T> {

    /** ID of the item */
    id: number;

    /** Global ID of the item */
    globalId: string;

    /** Name of the item */
    name: string;

    masterId?: number;

    masterNodeId?: number;

    inheritedFromId?: number;

    /** Creator of the item */
    creator: Normalizable<T, User<Raw>, number>;

    /** Creation date of the item as a Unix timestamp */
    cdate: number;

    /** Last editor of the item */
    editor: Normalizable<T, User<Raw>, number>;

    /** Last Edit Date of the item as a Unix timestamp  */
    edate: number;

    /** Item type */
    type: ItemType;

    /** Deletion information, if object was deleted */
    deleted?: DeleteInfo;

    /** Deletion information about the master (if the object is not a master itself) */
    masterDeleted?: DeleteInfo;

    /** Deletion information about the containing folder */
    folderDeleted?: DeleteInfo;

    /** Usage information about the item */
    usage?: Usage;
}

/**
 * Items which can be inherited, such as folders, pages, files...
 */
export interface InheritableItem<T extends ModelType = DefaultModelType> extends Item<T> {

    type: FolderItemType;

    /** Whether this item is excluded from multichannelling */
    excluded: boolean;

    /** True if the item was inherited */
    inherited: boolean;

    /** Name of the node, this item is inherited from. */
    inheritedFrom: string;

    /** ID of the node this item is inherited from. */
    inheritedFromId: number;

    /** Name of the node, the master object belongs to */
    masterNode: string;

    /** ID of the node, the master object belongs to. */
    masterNodeId: number;

    /** Whether this item is disinherited by default in new channels. */
    disinheritDefault: boolean;

    /** True if the item is disinherited in some channels */
    disinherited: boolean;

    /**
     * IDs of nodes/channels, in which the object will not be inherited.
     * This will be ignored, if the object is excluded from multichannelling.
     *
     * NOTE: This property does not exist by default, but is created
     * after calling the `<type>/disinherit/<id>` endpoint, at which
     * point it is merged into the item.
     */
    disinherit?: number[];


    /**
     * IDs of nodes/channels, where this object (actually its master) can be inherited
     *
     * NOTE: This property does not exist by default, but is created
     * after calling the `<type>/disinherit/<id>` endpoint, at which
     * point it is merged into the item.
     */
    inheritable?: number[];

}

export interface ItemsGroupedByChannelId {
    pages: ChannelSyncRequest[];
    files: ChannelSyncRequest[];
    images: ChannelSyncRequest[];
}

/**
 * Item types that have a `tags` property that contains object properties.
 *
 * Note that `tags` may also contain non-object tags (always check if `tag.type === 'OBJECTTAG'`).
 */
export type ItemWithObjectTags<T extends ModelType = DefaultModelType> = File<T> | Folder<T> | Image<T> | Page<T>;

/**
 * Item types that have a `tags` property that contains content tags.
 *
 * Note that `tags` may also contain object tags (always check if `tag.type === 'CONTENTTAG'`).
 */
export type ItemWithContentTags<T extends ModelType = DefaultModelType> = Page<T>;

/**
 * In many situations, we want to treat a folder and the root folder of a node the same way.
 * Since the server returns a "type" of "folder" for subfolders and "node" or "channel"
 * for the root folder, comparing the type alone is not sufficient.
 */
export function isFolderOrNode(input: any): input is Folder {
    return (typeof input === 'object' && input &&
        (input.type === 'folder' || input.type === 'node' || input.type === 'channel'));
}
