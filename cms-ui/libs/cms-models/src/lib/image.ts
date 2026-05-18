import { EditableFileProps, FileOrImage } from './file';
import { Folder } from './folder';
import { DefaultModelType, ModelType, Raw } from './type-util';

/**
 * These are the user-editable properties of a Image object.
 */
export interface EditableImageProps extends EditableFileProps {
    fpX?: number;
    fpY?: number;
}

/**
 * Associates an uploaded file with its destination folder.
 */
export interface FileUpload {

    /** The folder that the file was uploaded to. */
    destinationFolder: Folder;

    /** The file that has been uploaded. */
    file: FileOrImage<Raw>;
}

/**
 * Represents an image in the CMS.
 *
 * https://www.gentics.com/Content.Node/cmp8/guides/restapi/json_Image.html
 */
export interface Image<T extends ModelType = DefaultModelType> extends FileOrImage<T> {
    '@class'?: 'com.gentics.contentnode.rest.model.Image';
    iconCls: 'gtx_image';
    type: 'image';
    typeId: 10011;

    /** Image size in pixels (x-Dimension) */
    sizeX: number;

    /** Image size in pixels (y-Dimension) */
    sizeY: number;

    /** DPI (x-Dimension) */
    dpiX: number;

    /** DPI (y-Dimension) */
    dpiY: number;

    /** The focal point x-axis factor */
    fpX: number;

    /** The focal point y-axis factor */
    fpY: number;

    /** Whether the image is resizable by Gentics Image Store */
    gisResizable: boolean;

    /**
     * The info property only exists when getting properties from `gcnImagePlugin.imageProperties` in the
     * image editor. It provides the raw data that can be used to create a request to `image/resize`.
     */
    info?: {
        x: number;
        y: number;
        cw: number;
        ch: number;
        src: string;
        w: number;
        h: number;
        copyFile: boolean;
    };
}
