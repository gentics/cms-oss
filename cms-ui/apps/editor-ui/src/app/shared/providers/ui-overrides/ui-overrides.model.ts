/**
 * A customer configuration that allows overwriting behavior of the UI.
 * It is loaded from /.Node/customer-config/config/ui-overrides.json and ignored if missing.
 */
export interface UIOverrides {
    changePasswordOption?: UIButtonOverride;
    composeMessageButton?: UIButtonOverride;
    fileDragAndDrop?: UIDisableOverride;
    newFileButton?: UIButtonOverride;
    newImageButton?: UIButtonOverride;
    replaceFileButton?: UIButtonOverride;
    replaceImageButton?: UIButtonOverride;
}

export type UIOverrideSlot = keyof UIOverrides;

export type UIButtonOverride = UIToolOverride | UIDisableOverride | UIHideOverride;

export interface UIToolOverride {
    openTool: string;
    toolPath?: string;
    restartTool?: boolean;
}

export interface UIDisableOverride {
    disable: boolean;
}

export interface UIHideOverride {
    hide: boolean;
}

/** A hash of values that can be used in strings in ui-overrides.json, e.g. "my-url/{{VARIABLE}}". */
export type UIOverrideParameters = { [key: string]: string | number | boolean };
