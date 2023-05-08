
/**
 * This file contains definitions for part of the UI state, which are exposed over the `window.GCMSUI` object.
 */

/**
 * The user interface languages available in the GCMS UI.
 * Since 5.40.x generic.
 */
export type GcmsUiLanguage = string;

/** The possible edit modes of the content-frame. */
export type EditMode = 'preview' | 'edit' | 'editProperties' | 'previewVersion' |
                       'compareVersionContents' | 'compareVersionSources';
