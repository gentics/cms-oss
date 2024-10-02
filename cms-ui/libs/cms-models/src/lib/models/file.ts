import { Folder } from './folder';
import { InheritableItem } from './item';
import { Tags } from './tag';
import { DefaultModelType, ModelType, Normalizable, Raw } from './type-util';

/**
 * These are the user-editable properties of a File object.
 */
export type EditableFileProps = Partial<Pick<FileOrImage, 'name' | 'description' | 'forceOnline'
| 'niceUrl' | 'alternateUrls' | 'customCdate' | 'customEdate'>>;

/**
 * Superinterface for CMS files and images.
 *
 * Contains the common properties of the REST API types `File` and `Image`:
 * https://www.gentics.com/Content.Node/cmp8/guides/restapi/json_File.html
 * https://www.gentics.com/Content.Node/cmp8/guides/restapi/json_Image.html
 */
export interface FileOrImage<T extends ModelType = DefaultModelType> extends InheritableItem<T> {

    /** Type ID */
    typeId: number;

    /** File type */
    fileType: string;

    /** Description */
    description: string;

    /** The `Folder`, in which this item is located. */
    folder?: Normalizable<T, Folder<Raw>, number>;

    /** The ID of the folder, in which this item is located. */
    folderId: number;

    /** The name of the folder, in which this item is located. */
    folderName: string;

    /** File size in bytes */
    fileSize: number;

    /** Channel ID */
    channelId: number;

    /** Map of object tags of this item */
    tags?: Tags;

    /** URL to the item */
    url?: string;

    /** Nice URL */
    niceUrl?: string;

    /** Additional/Alternative Nice URLs */
    alternateUrls?: string[];

    /** Live URL to the item */
    liveUrl: string;

    /** Folder path of this item */
    path: string;

    /**
     * True if the item shall be force to go online, even if nothing depends on it
     * (may be null if status is undetermined)
     */
    forceOnline: boolean;

    /** Customizable creation date to display in public/frontend */
    customCdate: number;

    /** Customizable edit date to display in public/frontend */
    customEdate: number;

    /** Publish path */
    publishPath: string;

    /** True if the item is online, false if it is offline */
    online: boolean;

    /** True for broken items */
    broken: boolean;

    /** This is a file or an image, so leaf is true */
    leaf: boolean;

    cls: 'file';

    /** Name of the file */
    text: string;
}

/**
 * Describes a file in the CMS.
 *
 * https://www.gentics.com/Content.Node/cmp8/guides/restapi/json_File.html
 */
export interface File<T extends ModelType = DefaultModelType> extends FileOrImage<T> {
    '@class'?: 'com.gentics.contentnode.rest.model.File';
    iconCls: 'gtx_file';
    type: 'file';
    typeId: 10008;
}
