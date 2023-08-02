import { NormalizableEntityTypesMap } from './gcms-normalizer/gcms-normalizer-types';
import { DefaultModelType, ModelType } from './type-util';

export type TagmapEntryParentType = keyof Pick<NormalizableEntityTypesMap<DefaultModelType>, 'contentRepository' | 'contentRepositoryFragment'> ;

/** @see https://www.gentics.com/Content.Node/guides/restapi/json_TagmapEntryModel.html */
export interface TagmapEntryBase<T extends ModelType> {
    /** Global ID */
    globalId: string;
    /** Tag name (property to resolve for the object) */
    tagname: string;
    /** Map name (name of the attribute in the ContentRepository) */
    mapname: string;
    /** Type of the object */
    object?: number;
    /** Type of the object */
    objType?: number;
    /** Attribute Type */
    attributeType: number;
    /** Type of the target object for link attributes */
    targetType: number;
    /** Multivalue flag */
    multivalue: boolean;
    /** Optimized flag */
    optimized: boolean;
    /** Reserved flag */
    reserved: boolean;
    /** Filesystem flag */
    filesystem: boolean;
    /** Name of the foreign attribute for foreignlink attributes */
    foreignlinkAttribute: string;
    /** Rule for restricting foreign linked objects */
    foreignlinkAttributeRule: string;
    /** Entry category */
    category: string;
    /** True when the entry is a segmentfield (of a Mesh ContentRepository) */
    segmentfield: boolean;
    /** True when the entry is a displayfield (of a Mesh ContentRepository) */
    displayfield: boolean;
    /** True when the entry is a urlfield (of a Mesh ContentRepository) */
    urlfield: boolean;
    /** Get the elasticsearch specific configuration of a Mesh CR */
    elasticsearch: object;
    /** Get the micronode filter (for entries of type "micronode") */
    micronodeFilter: string;
    /** Name of the CR Fragment, this entry belongs to. Null, if the entry directly belongs to the ContentRepository. */
    fragmentName: string;
}

/** Data model as defined by backend. */
export interface TagmapEntry<T extends ModelType = DefaultModelType> extends TagmapEntryBase<T> {
    /** Internal ID of the object property definition */
    id: number;
}

/**
 * Data model as defined by frontend.
 * @deprecated Create your own application specific type/business object instead.
 */
export interface TagmapEntryBO<T extends ModelType = DefaultModelType> extends TagmapEntryBase<T> {
    /** Internal ID of the object property definition */
    id: string;
}

export enum TagmapEntryPropertiesObjectType {
    FOLDER = 10002,
    PAGE = 10007,
    FILE = 10008,
};

export enum MeshTagmapEntryAttributeTypes {
    TEXT = 1,
    REFERENCE = 2,
    INTEGER = 3,
    BINARY = 6,
    DATE = 10,
    BOOLEAN = 11,
    MICRONODE = 12,
};

export enum SQLTagmapEntryAttributeTypes {
    TEXT = 1,
    REFERENCE = 2,
    INTEGER = 3,
    TEXT_LONG = 5,
    BINARY = 6,
    FOREIGN_LINK = 7,
};

export enum TagmapEntryAttributeTypes {
    TEXT = 1,
    REFERENCE = 2,
    INTEGER = 3,
    TEXT_LONG = 5,
    BINARY = 6,
    FOREIGN_LINK = 7,
    DATE = 10,
    BOOLEAN = 11,
    MICRONODE = 12,
};
