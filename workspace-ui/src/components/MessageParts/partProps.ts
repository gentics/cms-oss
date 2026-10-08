/** What every part renderer gets: its part and where it is shown. */
export interface PartViewProps<Part> {
    part: Part;
    /** The session the message belongs to, for links into it (`file_ref`). */
    sessionId: string;
}
