import { type RefObject, useCallback, useEffect, useRef, useState } from 'react';

/**
 * The selector of the parts of a CMS page: the CMS marks them in read-only mode with `tagmarkers=true` (`AlohaRenderer`).
 * A part may be several sibling elements, each marked with the same tag.
 */
const MARKED = '[data-gcn-tagid]';

/** A marked part of the previewed page, as the CMS annotated it. */
export interface PreviewElement {
    /** The tag name, e.g. `content_3` (`data-gcn-tagname`). */
    tagName: string;
    /** The name of its type (construct), e.g. "Text" (`data-gcn-i18n-constructname`); empty when missing. */
    constructName: string;
}

/** A rectangle in px, relative to the frame's visible page area. */
export interface ElementBox {
    top: number;
    left: number;
    width: number;
    height: number;
}

/** The innermost marked part `target` belongs to, or `null`. */
export function markedElementOf(target: EventTarget | null): Element | null {
    // An `instanceof Element` check would fail for nodes of the frame's own window.
    const node = target as Node | null;
    const element = node?.nodeType === 1 ? (node as Element) : node?.parentElement;

    return element?.closest(MARKED) ?? null;
}

/** The CMS annotations of a marked part. */
export function previewElementOf(element: Element): PreviewElement {
    return {
        tagName: element.getAttribute('data-gcn-tagname') ?? '',
        constructName: element.getAttribute('data-gcn-i18n-constructname') ?? '',
    };
}

/**
 * The text of a marked part, trimmed: as the browser renders it (`innerText`, so line breaks and
 * blocks become `\n`) where it has that, its `textContent` otherwise.
 */
export function partTextOf(element: Element): string {
    const rendered = (element as HTMLElement).innerText as string | undefined;

    return (rendered ?? element.textContent ?? '').trim();
}

/** Whether a marked part shows an image: it is an `<img>` or holds one. */
export function isImagePart(element: Element): boolean {
    return element.matches('img') || element.querySelector('img') !== null;
}

/** The box around all elements of a part. */
function boxOf(elements: Element[]): ElementBox {
    const rects = elements.map((element) => element.getBoundingClientRect());
    const top = Math.min(...rects.map((rect) => rect.top));
    const left = Math.min(...rects.map((rect) => rect.left));

    return {
        top,
        left,
        width: Math.max(...rects.map((rect) => rect.right)) - left,
        height: Math.max(...rects.map((rect) => rect.bottom)) - top,
    };
}

/** The elements of the part `tagName`; none when the page lacks it. */
function findParts(doc: Document, tagName: string): Element[] {
    return [...doc.querySelectorAll(`${MARKED}[data-gcn-tagname="${CSS.escape(tagName)}"]`)];
}

/**
 * The marked parts of the page in the preview frame `frameRef`: which one the mouse is over and where
 * the selected one (`selected`, a tag name) sits, both as boxes for an overlay above the frame. Call
 * `onFrameLoad` from the frame's `load`. A click on a part selects it through `onSelect` instead of
 * following a link in it; Escape in the frame clears the selection. The boxes follow scrolling,
 * resizing and changes to the page. `changed` (tag names, from `changed_elements`) gives the boxes of
 * the parts the last CMS write changed; a name not on the page has none. `textOf` reads a part's
 * current text (`partTextOf`), `hasImage` whether it shows an image (`isImagePart`).
 *
 * Reading the frame needs the page to come from the Workspace's own origin; for any other page there
 * is no document to read, and nothing is hovered or selected. `onSelectedGone` is called when a loaded
 * page lacks the selected part. New callbacks on each render do not re-attach anything.
 */
export function usePreviewElements(
    frameRef: RefObject<HTMLIFrameElement | null>,
    selected: string | null,
    onSelect: (element: PreviewElement | null) => void,
    onSelectedGone: () => void,
    changed: readonly string[] = [],
) {
    const callbacks = useRef({ onSelect, onSelectedGone });

    useEffect(() => {
        callbacks.current = { onSelect, onSelectedGone };
    });

    const [doc, setDoc] = useState<Document | null>(null);
    const [hovered, setHovered] = useState<{ element: PreviewElement; box: ElementBox } | null>(null);
    const [selectedBox, setSelectedBox] = useState<ElementBox | null>(null);
    const [changedBoxes, setChangedBoxes] = useState<{ tagName: string; box: ElementBox }[]>([]);
    // One string, so a new array with the same names does not measure again.
    const changedKey = changed.join('\n');
    const [hoveredNode, setHoveredNode] = useState<Element | null>(null);

    const onFrameLoad = useCallback(() => {
        let loaded: Document | null;

        try {
            // `null`, or a SecurityError in some browsers, for a page from another origin.
            loaded = frameRef.current?.contentDocument ?? null;
        } catch {
            loaded = null;
        }

        setHoveredNode(null);
        setDoc(loaded);
    }, [frameRef]);

    // Listens in the frame's document: hover, click to select, Escape to clear.
    useEffect(() => {
        if (!doc) {
            return;
        }

        function handleMouseOver(event: MouseEvent) {
            setHoveredNode(markedElementOf(event.target));
        }

        function handleMouseLeave() {
            setHoveredNode(null);
        }

        function handleClick(event: MouseEvent) {
            const element = markedElementOf(event.target);

            if (element) {
                event.preventDefault();
                callbacks.current.onSelect(previewElementOf(element));
            }
        }

        function handleKeyDown(event: KeyboardEvent) {
            if (event.key === 'Escape') {
                callbacks.current.onSelect(null);
            }
        }

        doc.addEventListener('mouseover', handleMouseOver);
        doc.documentElement.addEventListener('mouseleave', handleMouseLeave);
        doc.addEventListener('click', handleClick);
        doc.addEventListener('keydown', handleKeyDown);

        return () => {
            doc.removeEventListener('mouseover', handleMouseOver);
            doc.documentElement.removeEventListener('mouseleave', handleMouseLeave);
            doc.removeEventListener('click', handleClick);
            doc.removeEventListener('keydown', handleKeyDown);
        };
    }, [doc]);

    // Measures the hovered and the selected part, and again whenever the page scrolls, resizes or changes.
    useEffect(() => {
        const frame = frameRef.current;
        const view = doc?.defaultView;

        if (!doc || !frame || !view) {
            setHovered(null);
            setSelectedBox(null);
            setChangedBoxes([]);

            return;
        }

        let frameRequest = 0;

        function measure() {
            const selectedParts = selected ? findParts(doc!, selected) : [];

            if (hoveredNode?.isConnected) {
                const element = previewElementOf(hoveredNode);
                const parts = element.tagName ? findParts(doc!, element.tagName) : [];

                setHovered({ element, box: boxOf(parts.length ? parts : [hoveredNode]) });
            } else {
                setHovered(null);
            }
            setSelectedBox(selectedParts.length ? boxOf(selectedParts) : null);
            setChangedBoxes(changedKey.split('\n').flatMap((tagName) => {
                const parts = tagName ? findParts(doc!, tagName) : [];

                return parts.length ? [{ tagName, box: boxOf(parts) }] : [];
            }));

            if (selected && !selectedParts.length) {
                callbacks.current.onSelectedGone();
            }
        }

        function scheduleMeasure() {
            cancelAnimationFrame(frameRequest);
            frameRequest = requestAnimationFrame(measure);
        }

        measure();
        view.addEventListener('scroll', scheduleMeasure, true);
        view.addEventListener('resize', scheduleMeasure);

        const mutations = new MutationObserver(scheduleMeasure);
        const sizes = new ResizeObserver(scheduleMeasure);

        mutations.observe(doc.documentElement, { subtree: true, childList: true, attributes: true, characterData: true });
        sizes.observe(frame);

        return () => {
            cancelAnimationFrame(frameRequest);
            view.removeEventListener('scroll', scheduleMeasure, true);
            view.removeEventListener('resize', scheduleMeasure);
            mutations.disconnect();
            sizes.disconnect();
        };
    }, [doc, frameRef, hoveredNode, selected, changedKey]);

    // The current text of a part, read from the page as it is now (the texts of its elements, one per line); empty when
    // the page lacks it.
    const textOf = useCallback((tagName: string) => {
        const parts = doc ? findParts(doc, tagName) : [];

        return parts.map(partTextOf).filter(Boolean).join('\n');
    }, [doc]);

    // Whether a part shows an image (`isImagePart`) in any of its elements, on the page as it is now.
    const hasImage = useCallback((tagName: string) => {
        const parts = doc ? findParts(doc, tagName) : [];

        return parts.some(isImagePart);
    }, [doc]);

    return { onFrameLoad, isFrameReadable: Boolean(doc), hovered, selectedBox, changedBoxes, textOf, hasImage };
}
