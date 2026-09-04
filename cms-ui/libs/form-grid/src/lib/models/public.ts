export enum FormGridEditMode {
    /** Allows the form to be fully edited without restrictions */
    FULL = 'full',
    /** Only a subset of properties/settings can be edited */
    RESTRICTED = 'restricted',
    /** Nothing of the form can be edited. Usually used as preview mode. */
    NONE = 'none',
}

export enum FormGridViewMode {
    EDITOR = 'editor',
    PREVIEW = 'preview',
}
