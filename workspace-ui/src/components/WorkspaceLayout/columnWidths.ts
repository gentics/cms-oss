/** Smallest width of the left column in px. */
export const MIN_LEFT_WIDTH = 210;
/** Smallest width of the right column in px. */
export const MIN_RIGHT_WIDTH = 320;
/** Width the center column always keeps free in px (design.md §11; the draft's script used 300). */
export const MIN_CENTER_WIDTH = 320;
/** Width of one splitter track in px (design.md §6.2). */
export const SPLITTER_WIDTH = 9;

/** Width of the left and right column in px; `null` keeps the default width from the CSS. */
export interface ColumnWidths {
    left: number | null;
    right: number | null;
}

export interface ColumnWidthInput {
    /** Width of the whole workspace grid in px. */
    total: number;
    isLeftVisible: boolean;
    /** Widths asked for by dragging or the keyboard. */
    requested: ColumnWidths;
    /** Widths the columns are rendered with right now. */
    rendered: { left: number; right: number };
}

export interface ClampedColumnWidths extends ColumnWidths {
    maxLeft: number;
    maxRight: number;
}

/**
 * Fits the requested widths into the grid: each side column keeps its minimum, and together they
 * leave the center column at least `MIN_CENTER_WIDTH`. A column without a requested width counts
 * with its rendered width.
 */
export function clampColumnWidths({ total, isLeftVisible, requested, rendered }: ColumnWidthInput): ClampedColumnWidths {
    const splitters = (isLeftVisible ? SPLITTER_WIDTH : 0) + SPLITTER_WIDTH;
    const free = total - splitters - MIN_CENTER_WIDTH;
    const maxLeft = Math.max(MIN_LEFT_WIDTH, free - (requested.right ?? rendered.right));
    const maxRight = Math.max(MIN_RIGHT_WIDTH, free - (isLeftVisible ? (requested.left ?? rendered.left) : 0));

    return {
        left: requested.left === null ? null : Math.round(clamp(requested.left, MIN_LEFT_WIDTH, maxLeft)),
        right: requested.right === null ? null : Math.round(clamp(requested.right, MIN_RIGHT_WIDTH, maxRight)),
        maxLeft: Math.round(maxLeft),
        maxRight: Math.round(maxRight),
    };
}

function clamp(value: number, min: number, max: number): number {
    return Math.max(min, Math.min(value, max));
}
