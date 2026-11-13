import { DefaultModelType, ModelType, Raw } from './type-util';
import { User } from './user';

/** Type of a Portal Implementation */
export enum PortalImplementationType {
    PHP = 'PHP',
    JAVA = 'Java',
}

/** Portal Implementation (REST resource `/portalimplementations`) */
export interface PortalImplementationBase<T extends ModelType> {
    /** Global ID */
    globalId: string;
    /** Keyword (unique) */
    keyword: string;
    /** Name (unique) */
    name: string;
    /** Description */
    description?: string;
    /** Type */
    type: PortalImplementationType;
    /** Creator */
    creator?: User<Raw>;
    /** Creation timestamp */
    cdate?: number;
    /** Last editor */
    editor?: User<Raw>;
    /** Timestamp of last editing */
    edate?: number;
}

/** Data model as defined by backend. */
export interface PortalImplementation<T extends ModelType = DefaultModelType> extends PortalImplementationBase<T> {
    /** Internal ID of the Portal Implementation */
    id: number;
}

/**
 * Data model as defined by frontend.
 * @deprecated Create your own application specific type/business object instead.
 */
export interface PortalImplementationBO<T extends ModelType = DefaultModelType> extends PortalImplementationBase<T> {
    /** Internal ID of the Portal Implementation */
    id: string;
}

/** File contained in a Portal Implementation (REST resource `/portalimplementations/{id}/files`) */
export interface PortalImplementationFile {
    /** Internal ID of the file */
    id: number;
    /** Global ID */
    globalId: string;
    /** Path of the file (including the filename) */
    path: string;
    /** Filename */
    name: string;
    /** Path of the folder containing the file (empty for the root folder) */
    folder: string;
    /** Mimetype */
    mimetype: string;
    /** Filesize in bytes */
    filesize: number;
    /** Creator */
    creator?: User<Raw>;
    /** Creation timestamp */
    cdate?: number;
    /** Last editor */
    editor?: User<Raw>;
    /** Timestamp of last editing */
    edate?: number;
}
