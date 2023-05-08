/**
 * ## Elastic Search indexed properties
 * Since various entities indexed at GCMS Elastic Searche must be queried only by their indexed properties
 * the following index properties must be defined for each entity.
 */

/** Elastic Search indexed properties for entity `page`. */
export const ELASTICSEARCH_INDEXED_PROPERTIES_PAGE = Object.freeze([
    'id',
    'nodeId',
    'folderId',
    'name',
    'filename',
    'description',
    'content',
    'created',
    'creatorId',
    'edited',
    'editorId',
    'published',
    'publisherId',
    'templateId',
    'language',
    'languageCode',
    'customCreationDate',
    'customEditDate',
    'modified',
    'offlineAt',
    'online',
    'planned',
    'publishAt',
    'queued',
    'queuedOfflineAt',
    'queuedPublishAt',
    'systemCreationDate',
    'systemEditDate',
    'niceUrl',
    'path',
    'objecttype',
]);

/** Elastic Search indexed properties for entity `form`. */
export const ELASTICSEARCH_INDEXED_PROPERTIES_FORM = Object.freeze([
    'id',
    'nodeId',
    'folderId',
    'name',
    'description',
    'created',
    'creatorId',
    'edited',
    'editorId',
    'published',
    'publisherId',
    'language',
    'languageCode',
    'modified',
    'online',
    'objecttype',
]);

/** Elastic Search indexed properties for entity `folder`. */
export const ELASTICSEARCH_INDEXED_PROPERTIES_FOLDER = Object.freeze([
    'id',
    'nodeId',
    'folderId',
    'name',
    'description',
    'created',
    'creatorId',
    'edited',
    'editorId',
    'objecttype',
]);

/** Elastic Search indexed properties for entity `image`. */
export const ELASTICSEARCH_INDEXED_PROPERTIES_IMAGE = Object.freeze([
    'id',
    'nodeId',
    'folderId',
    'name',
    'filename',
    'description',
    'created',
    'creatorId',
    'edited',
    'editorId',
    'mimetype',
    'online',
    'niceUrl',
    'objecttype',
]);

/** Elastic Search indexed properties for entity `file`. */
export const ELASTICSEARCH_INDEXED_PROPERTIES_FILE = Object.freeze([
    'id',
    'nodeId',
    'folderId',
    'name',
    'filename',
    'description',
    'created',
    'creatorId',
    'edited',
    'editorId',
    'mimetype',
    'content',
    'binarycontent',
    'online',
    'niceUrl',
    'objecttype',
]);
