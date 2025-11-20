import { ListResponse, Response } from './response';

/**
 * Response from endpoint `/admin/publishInfo`.
 */
export interface PublishInfo extends Response {
    /* Flag to mark failed publish process */
    failed: boolean;

    /* Failed flag for the previous publish process */
    lastFailed?: boolean;

    /* Progress in percent */
    progress: number;

    /* Estimated remaining duration in seconds */
    estimatedDuration: number;

    /* True when the publish process is currently running */
    running: boolean;

    /* Total number of work items of the current publish process */
    totalWork: number;

    /* Number of work items done */
    totalWorkDone: number;

    /* Name of the current publish process phase */
    phase: string;

    /* File counts */
    files: PublishObjectsCount;

    /* Folder counts */
    folders: PublishObjectsCount;

    /* Page counts */
    pages: PublishObjectsCount;

    /* Form counts */
    forms: PublishObjectsCount;
}

export interface PublishObjectsCount {
    /* Number of objects to publish */
    toPublish: number;

    /* Number of delayed objects */
    delayed: number;

    /* Number of objects, which are already published in the current publish process */
    published: number;

    /* Number of objects remaining to be published in the current publish process */
    remaining: number;
}

export interface Update {
    version: string;

    changelogUrl: string;
}

/**
 * Response from endpoint `/admin/updates`.
 */
export interface UpdatesInfo extends Response {
    /* List of available updates */
    available: Update[];
}

/**
 * Response from endpoint `/admin/features/{featureName}`.
 */
export interface CmsFeatureInfo extends Response {
    /* Name of the feature */
    name: string;

    /* True when the feature is activated, false if not */
    activated: boolean;
}

/**
 * Response from endpoint `/admin/content/publishqueue`.
 */
export interface PublishQueue extends Response {
    nodes: {
        /** property string */
        [nodeId: string]: PublishQueueNode;
    };
}

/**
 * Represents admin info per node in the CMS.
 */
export interface PublishQueueNode extends Response {
    /* File counts */
    files: PublishObjectsCount;
    /* Folder counts */
    folders: PublishObjectsCount;
    /* Page counts */
    pages: PublishObjectsCount;
    /* Form counts */
    forms: PublishObjectsCount;
}

/**
 * Response from endpoint `/admin/content/dirtqueue/summary`.
 */
export interface DirtQueueSummaryResponse extends Response {
    items: DirtQueueSummaryEntry[];
}

export interface DirtQueueSummaryEntry {
    label: string;
    count: number;
}

/**
 * Response from endpoint `/admin/content/dirtqueue`.
 */
export interface DirtQueueResponse extends ListResponse<DirtQueueEntry> {
}

export interface DirtQueueEntry {
    id: number;
    objType: number;
    objId: number;
    timestamp: number;
    label: string;
    failed: boolean;
    failReason: string;
}
export interface Jobs extends Response {
    /* True if more items are available to get (if paging was used) */
    hasMoreItems: boolean;

    /* Items in the list */
    items: [];

    /* Get total number of items available */
    numItems: number;
}
