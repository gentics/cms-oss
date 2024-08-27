import { DefaultModelType, ModelType, NormalizableEntity } from './type-util';

export interface ContentPackage<T extends ModelType = DefaultModelType> extends NormalizableEntity<T> {
    /** The global-id of the package (only available when imported) */
    globalId?: string;
    /** The timestamp when the package was imported the last time */
    timestamp?: number;

    /** The name/identifier of the package */
    name: string;
    /** The description of the package */
    description?: string;

    /** How many files are contained in the package */
    files?: number;
    /** How many folders are contained in the package */
    folders?: number;
    /** How many forms are contained in the package */
    forms?: number;
    /** How many images are contained in the package */
    images?: number;
    /** How many pages are contained in the package */
    pages?: number;
}

/**
 * @deprecated Create your own application specific type/business object instead.
 */
export interface ContentPackageBO<T extends ModelType = DefaultModelType> extends ContentPackage<T> {
    id: string;
}

export type StagableEntityType = 'file' | 'folder' | 'form' | 'image' | 'page';


export interface ContentPackageImportError {
    path: string;
    error: string;
    globalId: string;
    kind: string;
    recommendation: string;
}

