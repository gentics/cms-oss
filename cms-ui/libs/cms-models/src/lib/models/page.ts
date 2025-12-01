import { LocalizationType } from './common';
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

    /* The user that planned to publish */
    futurePublisher?: User;

    /* The user that planned to upublish */
    futureUnpublisher?: User;
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
  | 'alternateUrls' | 'language' | 'templateId' | 'priority' | 'customCdate' | 'customEdate' | 'tags'>>;

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

    /* BASICS
     * ---------------------------------------------------------------------- */

    /** Filename */
    fileName: string;

    /** Description */
    description: string;

    /** Template ID */
    templateId: number;

    /**
     * Language Code (if page has a language).
     * Should always be present, but optional for legacy reasons.
     */
    language?: string;

    /** Priority */
    priority: number;

    /** Nice URL */
    niceUrl?: string;

    /** Additional/Alternative Nice URLs */
    alternateUrls?: string[];

    /** Tags of the page */
    tags?: Tags;

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

    /* COMMON META-DATA
     * ---------------------------------------------------------------------- */

    /** Folder path to the page */
    readonly path: string;

    /** ID of the folder this page is located in. */
    readonly folderId: number;

    /** Language Name (if page has a language) */
    readonly languageName?: string;

    /** True if the page is locked */
    readonly locked: boolean;

    /** Unix timestamp, since when the page is locked, or -1 if it is not locked */
    readonly lockedSince: number;

    /** User, who locked the page */
    readonly lockedBy?: Normalizable<T, User<Raw>, number>;

    /**
     * Whether the page is modified (the last version of the page is not the currently published one)
     */
    readonly modified: boolean;

    /** True if the page was fetched readonly, false if fetched in edit mode */
    readonly readOnly: boolean;

    /**
     * ID of this pages content.
     * Can be used to determine if this is a variant or not.
     * If it's the same as `id`, then it is not a variant.'
     */
    readonly contentId: number;

    /** Channelset id */
    readonly channelSetId: number;

    /* MULTICHANNELLING
     * ---------------------------------------------------------------------- */

    /** Whether this page is a master page */
    readonly master: boolean;

    /** Channel id */
    readonly channelId?: number;

    /** If the page is partially localized (only certain tags). */
    readonly localizationType?: LocalizationType;

    /* PUBLISH META-DATA
     * ---------------------------------------------------------------------- */

    /** Whether the page is currently online. */
    readonly online: boolean;

    /** Whether the page is in queue for being published or taken offline */
    readonly queued: boolean;

    /** Whether the page has time management set or not. */
    readonly planned: boolean;

    /** ID of the publisher */
    readonly publisher?: Normalizable<T, User<Raw>, number>;

    /** Publish Date as a Unix timestamp */
    readonly pdate?: number;

    /** Publish path */
    readonly publishPath: string;

    /** URL to the page */
    readonly url?: string;

    /** Live URL to the page */
    readonly liveUrl?: string;

    /** Time management */
    readonly timeManagement: TimeManagement;

    /* VARIANTS
     * ---------------------------------------------------------------------- */

    /** This page's ID in the {@link pageVariants} */
    readonly contentSetId: number;

    /**
     * Page variants of the page.
     * Only included if the page is loaded with `pagevars`.
     */
    readonly pageVariants?: Normalizable<T, Page<Raw>, number>[];

    /** Corresponds to the language id. May be used with the {@link languageVariants} */
    readonly contentGroupId: number;

    /** Language variants */
    readonly languageVariants?: IndexById<Normalizable<T, Page<Raw>, number>>;

    /* INLINE REFERENCES
     * ---------------------------------------------------------------------- */

    /**
     * Template of the Page.
     * Only included if the page is loaded with `template`.
     */
    readonly template?: Normalizable<T, Template<Raw>, number>;

    /**
     * The folder that this page is located in.
     * Only included if the page is loaded with `folder`.
     */
    readonly folder?: Normalizable<T, Folder<Raw>, number>;

    /**
     * Translation status information.
     * Only included if the page is loaded with `translationstatus`.
     */
    readonly translationStatus?: PageTranslationStatus;

    /**
     * Page versions.
     * Only included if the page is loaded with `versioninfo`.
     */
    readonly versions?: PageVersion[];

    /**
     * Current version of the page.
     * Only included if the page is loaded with `versioninfo`.
     */
    readonly currentVersion?: PageVersion;

    /**
     * Published version of the page.
     * Only included if the page is loaded with `versioninfo`.
     */
    readonly publishedVersion?: PageVersion;

    /**
     * Workflow attached to the page.
     * Only included if the page is loaded with `workflow`.
     */
    readonly workflow?: PageWorkflow;

}
