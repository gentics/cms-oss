import { InheritableItem } from './item';
import { PermissionsMapCollection } from './permissions';
import { PrivilegeFlagName, PrivilegeMap } from './permissions/cms/privileges';
import { Tags } from './tag';
import { DefaultModelType, ModelType, Normalizable, Raw } from './type-util';

/**
 * These are the user-editable properties of a Folder object.
 */
export interface EditableFolderProps {
    name?: string;
    directory?: string;
    description?: string;
    descriptionI18n?: GtxI18nProperty;
    nameI18n?: GtxI18nProperty;
    publishDirI18n?: GtxI18nProperty;
}

/** A folder in the list of breadcrumbs of a folder. */
export interface FolderBreadcrumb {

    /** The ID of the folder */
    id: number;

    /** The name of the folder */
    name: string;

}

/**
 * A format for properties allowing for internationalization.
 */
export interface GtxI18nProperty { [key: string]: string }

/**
 * A Folder object as returned from the various folder endpoints:
 *
 * http://www.gentics.com/Content.Node/guides/restapi/resource_FolderResource.html
 * https://www.gentics.com/Content.Node/guides/restapi/json_Folder.html
 */
export interface Folder<T extends ModelType = DefaultModelType> extends InheritableItem<T> {
    type: 'folder';

    /** ID of the parent folder */
    motherId: number;

    /** Publish directory of the folder */
    publishDir: string;

    /** Description */
    description: string;

    /** ID of the startpage */
    startPageId?: number;

    /** Map of object tags of the folder */
    tags?: Tags;

    /** List of subfolders */
    subfolders?: Normalizable<T, Folder<Raw>, number>[];

    /** True if the folder has subfolders (regardless of whether they have been fetched), false if not */
    hasSubfolders: boolean;

    /** Node id */
    nodeId: number;

    /** Folder privileges */
    privileges?: PrivilegeFlagName[];

    /** Privilege bits */
    privilegeBits?: string;

    /** Map representation of all privileges */
    privilegeMap?: PrivilegeMap;

    /** Map representation of all permissions */
    permissionsMap?: PermissionsMapCollection;

    /**
     * Breadcrums of the folder.
     * The first item is the root folder and the last item the folder itself.
     */
    breadcrumbs: FolderBreadcrumb[];

    /**
     * Position of the folder in the folder tree.
     */
    atposidx: string;

    /** Folder path of this folder */
    path: string;

    /**
     * Channelset ID.
     *
     * All copies of the same folder in different channels share the same Channelset ID. Read only.
     */
    channelsetId: number;

    /**
     * Channel ID.
     *
     * It identifies different versions of the same folder in different channels.
     * Equals to the node id for which the folder is defined, or to 0 if it is defined
     * in the topost node of the channel hierarchy. Read only.
     */
    channelId: number;

    /**
     * True if the folder is a master, false otherwise.
     *
     * A folder is a master if it isn't a localized copy of another folder. Read only.
     */
    isMaster: boolean;

    /**
     * Master ID.
     *
     * The master is the next folder up in the channel hierarchy with the same Channelset ID.
     * The Master ID is 0 if there is no master. Read only.
     */
    masterId?: number;

    // Multilingual Properties -> https://jira.gentics.com/browse/GTXPE-994
    descriptionI18n?: GtxI18nProperty;
    nameI18n?: GtxI18nProperty;
    publishDirI18n?: GtxI18nProperty;
}
