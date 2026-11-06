/**
 * Normalizes the white space in `text`, so that two texts can be compared by what they read: every run of white
 * space (spaces, tabs, line breaks) becomes a single space, and white space at the start and end is removed.
 * E.g. `"  Ab dem \n 1. Juli "` becomes `"Ab dem 1. Juli"`.
 */
export function collapseWhiteSpace(text: string): string {
    return text.replace(/\s+/g, ' ').trim();
}
