/**
 * Models for the Device-Preview feature, which lets the user constrain the
 * content-frame iframe to a specific viewport size (mobile / tablet / desktop)
 * for responsive previews.
 *
 * @see DevicePreviewService
 */

/**
 * A configurable viewport-size preset that the user can select to preview the
 * current page at a specific resolution.
 */
export interface DevicePreset {
    /** Stable identifier, e.g. `mobile`, `tablet`, `desktop-1200` */
    id: string;
    /** i18n-key for the human-readable label */
    labelKey: string;
    /** Material-icon name displayed in the menu */
    icon: string;
    /** Logical viewport width in CSS pixels */
    width: number;
    /** Logical viewport height in CSS pixels */
    height: number;
    /** True for entries created by the user at runtime (not part of defaults) */
    isCustom?: boolean;
}

/** Current state of the device-preview feature. */
export interface DevicePreviewState {
    /** Whether device-preview is currently active. */
    active: boolean;
    /** Id of the currently selected preset, or null if none. */
    presetId: string | null;
}
