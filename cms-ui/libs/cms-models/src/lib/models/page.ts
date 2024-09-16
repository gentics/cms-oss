import { ExternalLink } from './external-link';
import { Folder } from './folder';
import { Group } from './group';
import { InheritableItem } from './item';
import { Tags } from './tag';
import { Template } from './template';
import { DefaultModelType, IndexById, ModelType, Normalizable, Raw } from './type-util';
import { User } from './user';

export type PageStatus = 'published' | 'edited' | 'offline' | 'queue' | 'timeframe' | 'publishat';

/** Time Management of pages / forms */
export interface TimeManagement {

    /** Unix timestamp at which the page / the form will be published */
    at: number;

    /** Unix timestamp at which the page / the form will be taken offline  */
    offlineAt: number;

    /** Queued time management for publishing the page / the form */
    queuedPublish?: QueuedActionPublish;

    /** Queued time management for taking the page / the form offline */
    queuedOffline?: QueuedActionTakeOffline;

    /** Page / Form Version that will be published at the timestamp */
    version?: PageVersion;

}

/** Superinterface for queued TimeManagement actions/ */
export interface QueuedAction {

    /** Unix timestamp at which the page shall be published/taken offline */
    at: number;

    /** User who put the page into the queue */
    user: User<Raw>;

}

/** Queued time management for publishing a page */
export interface QueuedActionPublish extends QueuedActionTakeOffline {

    /** Page Version that will be published at the timestamp */
    version: PageVersion;

}

/** Queued time management for taking a page offline */
export interface QueuedActionTakeOffline extends QueuedAction { }


export interface QueuedActionRequestPublishAt {
    at: number;
    alllang: boolean;
    keepVersion: boolean;
}

export interface QueuedActionRequestTakeOfflineAt {
    at: number;
    alllang: boolean;
}

export interface QueuedActionRequestClear {
    page: {
        id: number;
    };
    unlock: boolean;
    clearPublishAt?: boolean;
    clearOfflineAt?: boolean;
}

/**
 * Represents a page version in the CMS
 * https://www.gentics.com/Content.Node/cmp8/guides/restapi/json_PageVersion.html
 */
export interface PageVersion {

    /** Version number */
    // eslint-disable-next-line id-blacklist
    number: string;

    /** Version timestamp */
    timestamp: number;

    /** Editor of the version */
    editor: User<Raw>;

}

/** Information about the translation status of a page. */
export interface PageTranslationStatus {

    /** Page id of the page with which the given page is in sync */
    pageId: number;

    /** Page name of the page with which the given page is in sync */
    name: string;

    /** Version timestamp of the synchronized version */
    versionTimestamp: number;

    /** Language of the synchronized version */
    language: string;

    /** True when the page is in sync with the latest version of the other language variant, false if not */
    inSync: boolean;

    /** Version number of the page version, with which this page is in sync */
    version: string;

    /** Latest version information */
    latestVersion: {
        /** Version timestamp */
        versionTimestamp: number;
        /** Version number */
        version: string;
    };

}

/** Workflow that can be attached to a page. */
export interface PageWorkflow {
    groups: Group<Raw>[];
    message: string;
    user: User<Raw>;
    modified: boolean;
    timestamp: number;
}

/**
 * The user-editable properties of a Page object.
 */
export type EditablePageProps = Partial<Pick<Page, 'name' | 'fileName' | 'description' | 'niceUrl'
| 'alternateUrls' |'language' | 'templateId' | 'priority' | 'customCdate' | 'customEdate'>>;

/**
 * External Link Checker page list item, contains the page and the external links
 */
export interface PageWithExternalLinks<T extends ModelType = DefaultModelType> {
    editable: boolean;
    page: Page<T>;
    links: ExternalLink[];
}

/**
 * Represents a page in the CMS.
 *
 * https://www.gentics.com/Content.Node/cmp8/guides/restapi/json_Page.html
 */
export interface Page<T extends ModelType = DefaultModelType> extends InheritableItem<T> {

    type: 'page';

    /** Filename */
    fileName: string;

    /** Description */
    description: string;

    /** Folder path to the page */
    path: string;

    /** Template ID */
    templateId: number;

    /** ID of the folder this page is located in */
    folderId: number;

    /** Used to group language variants */
    contentSetId: number;

    /** Corresponds to the language id */
    contentGroupId: number;

    /**
     * Whether the page is modified (the last version of the page is not the currently published one)
     */
    modified: boolean;

    /** Priority */
    priority: number;

    /** True if the page was fetched readonly, false if fetched in edit mode */
    readOnly: boolean;

    /** ID of the publisher */
    publisher?: Normalizable<T, User<Raw>, number>;

    /** Publish Date as a Unix timestamp */
    pdate?: number;

    /**
     * Custom creation date of the page as a Unix timestamp.
     *
     * Set to 0 for clearing custom creation date and falling back to the real creation date.
     */
    customCdate?: number;

    /**
     * Custom edit date of the page as a Unix timestmap.
     *
     * Set to 0 for clearing custom edit date and falling back to the real edit date.
     */
    customEdate?: number;

    /** Language Code (if page has a language) */
    language?: string;

    /** Language Name (if page has a language) */
    languageName?: string;

    /** Nice URL */
    niceUrl?: string;

    /** Additional/Alternative Nice URLs */
    alternateUrls?: string[];

    /** Tags of the page */
    tags?: Tags;

    /** Time management */
    timeManagement: TimeManagement;

    /** Whether the page has time management set or not. */
    planned: boolean;

    /** Workflow attached to the page */
    workflow?: PageWorkflow;

    /** Whether the page is in queue for being published or taken offline */
    queued: boolean;

    /** Page variants of the page */
    pageVariants?: Normalizable<T, Page<Raw>, number>[];

    /** Publish path */
    publishPath: string;

    /** Language variants */
    languageVariants?: IndexById<Normalizable<T, Page<Raw>, number>>;

    /** URL to the page */
    url?: string;

    /** Live URL to the page */
    liveUrl?: string;

    /** Whether the page is currently online. */
    online: boolean;

    /** Template */
    template?: Normalizable<T, Template<Raw>, number>;

    /** The folder that this page is located in */
    folder?: Normalizable<T, Folder<Raw>, number>;

    /** Whether this page is a master page */
    master: boolean;

    /** True if the page is locked */
    locked: boolean;

    /** Unix timestamp, since when the page is locked, or -1 if it is not locked */
    lockedSince: number;

    /** User, who locked the page */
    lockedBy?: Normalizable<T, User<Raw>, number>;

    /** Translation status information */
    translationStatus?: PageTranslationStatus;

    /** Page versions */
    versions?: PageVersion[];

    /** Current version of the page */
    currentVersion?: PageVersion;

    /** Published version of the page */
    publishedVersion?: PageVersion;

    /** Content id */
    contentId: number;

    /** Channel id */
    channelId?: number;

    /** Channelset id */
    channelSetId: number;

}
