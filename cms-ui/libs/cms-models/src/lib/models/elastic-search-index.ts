import { DefaultModelType, ModelType, Normalizable, NormalizableEntity, Raw } from './type-util';

/**
 * A GCMS Elastic Search index.
 */
export interface ElasticSearchIndex<T extends ModelType = DefaultModelType> extends NormalizableEntity<T> {
    /** Same as `name`. @Note This is actually not part of the real REST API response, but added client-side to enforce data conformity. */
    id: string;
    /** Index name. */
    name: string;
    /** True when the index exists. */
    found: boolean;
    /** True when the index settings are valid. */
    settingsValid: boolean;
    /** True when the index mapping is valid. */
    mappingValid: boolean;
    /** Count of indexed objects. */
    indexed: number;
    /** Count of objects that must be indexed. */
    objects: number;
    /** Number of documents queued to be indexed. */
    queued: number;
}
