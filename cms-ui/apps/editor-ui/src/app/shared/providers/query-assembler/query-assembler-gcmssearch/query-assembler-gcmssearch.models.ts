import { FileListOptions, FolderListOptions, FormListOptions, PageListOptions } from '@gentics/cms-models';
/**
 * # GCMS Search indexed properties
 * Since various entities indexed at GCMS Search must be queried only by their defined properties
 * the following index properties must be defined for each entity.
 */

/** Common keys */
type GCMSSearchAvailableFiltersShared = 'all' | 'created' | 'edited' | 'objecttype';

/** Elastic Search indexed properties for entity `page`. */
export const GCMSSEARCH_AVAILABLE_FILTERS_PAGE: readonly ('published' | GCMSSearchAvailableFiltersShared | keyof PageListOptions)[] = Object.freeze([
    'all',
    'objecttype',
    'nodeId',
    'filename',
    'language',
    'modified',
    'niceurl',
    'online',
    'planned',
    'published',
    'queued',
    'template_id',
    'created',
    'edited',
]);

/** Elastic Search indexed properties for entity `form`. */
export const GCMSSEARCH_AVAILABLE_FILTERS_FORM: readonly ('nodeId' | GCMSSearchAvailableFiltersShared | keyof FormListOptions)[] = Object.freeze([
    'all',
    'objecttype',
    'nodeId',
    'modified',
    'online',
    'created',
    'edited',
]);

/** Elastic Search indexed properties for entity `folder`. */
export const GCMSSEARCH_AVAILABLE_FILTERS_FOLDER: readonly (GCMSSearchAvailableFiltersShared | keyof FolderListOptions)[] = Object.freeze([
    'all',
    'objecttype',
    'nodeId',
    'created',
    'edited',
]);

/** Elastic Search indexed properties for entity `image`. */
export const GCMSSEARCH_AVAILABLE_FILTERS_IMAGE: readonly (GCMSSearchAvailableFiltersShared | keyof FileListOptions)[] = Object.freeze([
    'all',
    'objecttype',
    'nodeId',
    'broken',
    'niceurl',
    'online',
    'created',
    'edited',
]);

/** Elastic Search indexed properties for entity `file`. */
export const GCMSSEARCH_AVAILABLE_FILTERS_FILE: readonly (GCMSSearchAvailableFiltersShared | keyof FileListOptions)[] = Object.freeze([
    'all',
    'objecttype',
    'nodeId',
    'broken',
    'niceurl',
    'online',
    'created',
    'edited',
]);
