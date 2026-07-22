/** Describes an embedded tool that can be added to the GCMS UI. */
export interface EmbeddedTool {
    /** Numeric internal ID of the tool used for permission handling. */
    id: number;

    /** Unique url-safe id of the tool. */
    key: string;

    /**
     * User-facing name (or names) of the tool.
     * @example
     *   { de: "Aufgabenplanung", en: "Task planner" }
     */
    name: string | { [languageName: string]: string };

    /**
     * The URL, where the tool is hosted.
     *
     * Since the custom tool API uses post messages for communication,
     * the tool may also be hosted on a different domain than the CMS.
     */
    toolUrl: string;

    /** The URL of the icon that should be displayed for the tool in the custom tools menu. */
    iconUrl?: string;

    /** If true, the tool opens in a new tab, otherwise as an overlay in the tab of the GCMS UI. */
    newtab: boolean;
}
