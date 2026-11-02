import type { CSSProperties, Ref } from 'react';

import type { ElementBox } from '@/hooks/usePreviewElements';

import styles from './PreviewOverlay.module.css';

// Where a box sits over the page, as inline style.
function boxStyle({ top, left, width, height }: ElementBox): CSSProperties {
    return { top, left, width, height };
}

// How far the outlines stand off a part (`outline-offset` in the CSS module); the focus mode leaves that much room.
const OUTLINE_OFFSET = 2;

// The four bands around `box` that the focus mode covers: above, below, left and right of it.
function dimBands({ top, left, width, height }: ElementBox): CSSProperties[] {
    const gap = OUTLINE_OFFSET;
    const bottom = top + height + gap;
    const right = left + width + gap;

    return [
        { top: 0, left: 0, right: 0, height: Math.max(0, top - gap) },
        { top: bottom, left: 0, right: 0, bottom: 0 },
        { top: top - gap, left: 0, width: Math.max(0, left - gap), height: height + 2 * gap },
        { top: top - gap, left: right, right: 0, height: height + 2 * gap },
    ];
}

/**
 * How the selection is shown: outlined; outlined as edited while one of its cards is open (image, link); or not at
 * all while an element in it is edited in place (it stays the cards' and the menu's anchor).
 */
export type SelectedMode = 'outline' | 'card' | 'anchor';

export interface PreviewOverlayProps {
    /** The box the page is dimmed around (focus mode, while the feedback card is open); `null` for none. */
    dimmed: ElementBox | null;
    /** The parts the last CMS write changed. */
    changed: { key: string; tagName: string; box: ElementBox }[];
    /** The selection and how it is shown; `null` for none. */
    selected: { box: ElementBox; mode: SelectedMode } | null;
    /** The selection's element, for what is anchored at it (menu, cards). */
    selectedRef: Ref<HTMLDivElement>;
    /** The item or cell "Write it yourself" would edit, when it is not the selection itself. */
    target: ElementBox | null;
    /** The element being edited in place. */
    editing: ElementBox | null;
    /** The part under the mouse with its name; `null` for none. */
    hovered: { box: ElementBox; label: string } | null;
}

/**
 * The outlines over the preview frame, the size of its page area (design.md §12 "Elemente in der Vorschau"): the
 * dimming of the focus mode, changed parts, the selection, the item or cell to be edited, the element being edited,
 * and the part under the mouse with its name. Hidden from assistive technology; lets the mouse through to the page.
 */
export function PreviewOverlay({ dimmed, changed, selected, selectedRef, target, editing, hovered }: PreviewOverlayProps) {
    return (
        <div className={styles.overlay} aria-hidden="true">
            {dimmed && dimBands(dimmed).map((band, index) => <div key={index} className={styles.dim} style={band} data-dim="" />)}
            {changed.map(({ key, tagName, box }) => <div key={key} className={styles.changed} style={boxStyle(box)} data-changed-part={tagName} />)}
            {selected && (
                <div
                    ref={selectedRef}
                    className={selected.mode === 'anchor' ? styles.anchor : `${styles.selected} ${selected.mode === 'card' ? styles.editing : ''}`}
                    style={boxStyle(selected.box)}
                />
            )}
            {target && <div className={styles.target} style={boxStyle(target)} data-edit-target="" />}
            {editing && <div className={`${styles.selected} ${styles.editing}`} style={boxStyle(editing)} data-editing="" />}
            {hovered && (
                <div className={styles.hovered} style={boxStyle(hovered.box)}>
                    <span className={styles.label}>{hovered.label}</span>
                </div>
            )}
        </div>
    );
}
