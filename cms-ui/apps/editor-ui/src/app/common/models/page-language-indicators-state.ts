/**
 * Exposes the state of the page language icon bar in editor list
 */
export interface PageLanguageIndicatorState {
    /** If TRUE display all languages including untranslated */
    expanded: boolean;
    /** If TRUE display all status symbols */
    displayStatusInfos: boolean;
}

export interface LanguageState {
    available: boolean;
    published: boolean;
    modified: boolean;
    deleted: boolean;
    queued: boolean;
    planned: boolean;
    inherited: boolean;
    localized: boolean;
    staged: boolean;
}

export interface ItemState {
    inQueue: boolean;
    planned: boolean;
    plannedOnline: boolean;
    plannedOffline: boolean;
    modified: boolean;
    published: boolean;
    offline: boolean;
    locked: boolean;
    deleted: boolean;
    inherited: boolean;
    localized: boolean;
}
