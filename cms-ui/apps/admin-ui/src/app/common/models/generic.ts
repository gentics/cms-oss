export type BooleanFn = () => boolean;

/** Interface necessary to implement for the guarded component */
export interface OnDiscardChanges {
    /** Returns TRUE if user has changed something */
    userHasEdited: boolean | BooleanFn;
    /** Returns TRUE if the changes are valid */
    changesValid: boolean | BooleanFn;
    /** Update entity data of invoking component */
    updateEntity: () => Promise<void>;
    /** Reset entity data of invoking component */
    resetEntity: () => Promise<void>;
}
