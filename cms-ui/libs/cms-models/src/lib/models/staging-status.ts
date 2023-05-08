export interface StagingStatus {
    /** If the item is included in the package */
    included: boolean;
    /** The package this status belongs to */
    packageName: string;
    /** If this has been recently done */
    recent: boolean;
}

export interface StagedItemsMap {
    [key: string]: StagingStatus;
}
