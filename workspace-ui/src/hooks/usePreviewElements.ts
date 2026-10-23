import { type RefObject, useCallback, useEffect, useRef, useState } from 'react';

/** The selector of the parts of a CMS page: the CMS marks them with `tagmarkers=true` (edit mode without Aloha Editor, `AlohaPageServlet`). */
const MARKED = '[data-gcn-tagid]';

/** What kind of block it is, from its HTML element (`h1`–`h6`, `p`, `blockquote`, `pre`, `ul`/`ol`, `table`). */
export type BlockKind = 'heading1' | 'heading2' | 'heading3' | 'heading4' | 'heading5' | 'heading6' | 'paragraph' | 'quote' | 'code' | 'list' | 'table';

/**
 * A block inside a text part: one top-level heading, paragraph, list, etc. The CMS often stores several
 * of them in one tag (e.g. a new page's headline and paragraph in one Aloha text); blocks let the user
 * select and edit each one separately.
 */
export interface PreviewBlock {
    /** Position among the part's blocks, starting at 0. */
    index: number;
    /** Total number of blocks in the part. */
    count: number;
    kind: BlockKind;
    /**
     * The first characters of the block's text (white space collapsed, at most `EXCERPT_LENGTH`, cut off
     * with `…`). Sent in the message so that GenAIx can tell which block is meant.
     */
    excerpt: string;
}

/** A part of the previewed page (a CMS tag, marked with `data-gcn-tagid`), or one block inside it. */
export interface PreviewElement {
    /** The tag name, e.g. `content_3` (`data-gcn-tagname`). */
    tagName: string;
    /** The name of its type (construct), e.g. "Text" (`data-gcn-i18n-constructname`); empty when missing. */
    constructName: string;
    /** Set when one block of the part is meant instead of the whole part (see `blocksOf`). */
    block?: PreviewBlock;
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

/**
 * Reads the tag name and construct name of a marked part. With `block` (one of `blocksOf(element)`) it
 * describes that block instead of the whole part.
 */
export function previewElementOf(element: Element, block?: Element | null): PreviewElement {
    const annotations = {
        tagName: element.getAttribute('data-gcn-tagname') ?? '',
        constructName: element.getAttribute('data-gcn-i18n-constructname') ?? '',
    };
    const blocks = block ? blocksOf(element) : [];
    const index = block ? blocks.indexOf(block) : -1;

    return index < 0 ? annotations : { ...annotations, block: { index, count: blocks.length, kind: blockKindOf(block!)!, excerpt: excerptOf(block!) } };
}

/**
 * The text of a marked part, trimmed: as the browser renders it (`innerText`, so line breaks and
 * blocks become `\n`) where it has that, its `textContent` otherwise.
 */
export function partTextOf(element: Element): string {
    const rendered = (element as HTMLElement).innerText as string | undefined;

    return (rendered ?? element.textContent ?? '').trim();
}

/** Whether a marked part shows an image: it is an `<img>` or holds one that no marked part inside it holds. */
export function isImagePart(element: Element): boolean {
    return element.matches('img') || Array.from(element.querySelectorAll('img')).some((image) => image.closest(MARKED) === element);
}

// Which HTML elements count as blocks, and their kind.
const BLOCK_KINDS: Record<string, BlockKind> = {
    h1: 'heading1',
    h2: 'heading2',
    h3: 'heading3',
    h4: 'heading4',
    h5: 'heading5',
    h6: 'heading6',
    p: 'paragraph',
    blockquote: 'quote',
    pre: 'code',
    ul: 'list',
    ol: 'list',
    table: 'table',
};

/** Maximum length of `PreviewBlock.excerpt`, in characters. */
export const EXCERPT_LENGTH = 40;

function blockKindOf(element: Element): BlockKind | undefined {
    return BLOCK_KINDS[element.localName];
}

function excerptOf(block: Element): string {
    const text = partTextOf(block).replace(/\s+/g, ' ');

    return text.length > EXCERPT_LENGTH ? `${text.slice(0, EXCERPT_LENGTH - 1).trimEnd()}…` : text;
}

/**
 * The blocks of a text part that can be selected one by one.
 *
 * The CMS often stores several blocks in one tag, e.g. the headline and the paragraphs of a new page
 * in one Aloha text (`html_long`). So that the user can select and edit them separately, the part's
 * direct children that are a heading, paragraph, quote, code block, list or table are used as blocks.
 *
 * Skipped: empty elements (like the `<p></p>` the editor leaves around an image), and elements that
 * are or contain another marked part (like an image block). Those are selected as parts of their own.
 *
 * Returns `[]`, so the part is selected as a whole, if fewer than two blocks remain or the part
 * itself shows an image (`isImagePart`).
 */
export function blocksOf(part: Element): Element[] {
    if (isImagePart(part)) {
        return [];
    }

    const blocks = Array.from(part.children).filter((child) => blockKindOf(child) !== undefined
        && !child.matches(MARKED)
        && !child.querySelector(MARKED)
        && partTextOf(child) !== '');

    return blocks.length >= 2 ? blocks : [];
}

/** The block of `part` (`blocksOf`) that contains `target` (e.g. the clicked node), or `null` if `target` is outside every block. */
export function blockOf(target: EventTarget | null, part: Element): Element | null {
    const node = target as Node | null;

    return node ? blocksOf(part).find((block) => block.contains(node)) ?? null : null;
}

// What the mouse is on: the innermost marked part, or the block inside it. `node` is the element to outline, `element` describes it.
function targetOf(target: EventTarget | null): { node: Element; element: PreviewElement } | null {
    const part = markedElementOf(target);

    if (!part) {
        return null;
    }

    const block = blockOf(target, part);

    return { node: block ?? part, element: previewElementOf(part, block) };
}

function boxOf(element: Element): ElementBox {
    const { top, left, width, height } = element.getBoundingClientRect();

    return { top, left, width, height };
}

function findPart(doc: Document, tagName: string): Element | null {
    return doc.querySelector(`${MARKED}[data-gcn-tagname="${CSS.escape(tagName)}"]`);
}

// Finds `element` on the current page: its part, or the block at `block.index` in it. `null` if the part
// is gone, or if after a reload that position no longer holds a block of the same kind (e.g. the
// headline was removed).
function findTarget(doc: Document, { tagName, block }: PreviewElement): Element | null {
    const part = findPart(doc, tagName);

    if (!part || !block) {
        return part;
    }

    const node = blocksOf(part)[block.index];

    return node && blockKindOf(node) === block.kind ? node : null;
}

/**
 * The marked parts of the page in the preview frame `frameRef`: which one the mouse is over and where
 * the selected one (`selected`) sits, both as boxes for an overlay above the frame. When a part is
 * split into blocks (`blocksOf`), these are the boxes of the single block. Call
 * `onFrameLoad` from the frame's `load`. A click on a part selects it through `onSelect` instead of
 * following a link in it; a click into the page outside the parts and Escape in the frame clear the
 * selection. The boxes follow scrolling,
 * resizing and changes to the page. `changed` (tag names, from `changed_elements`) gives the boxes of
 * the parts the last CMS write changed; a name not on the page has none. `textOf` reads the current
 * text of a part or block (`partTextOf`), `hasImage` whether it shows an image (`isImagePart`; a
 * block never does), `currentOf` returns a part or block as it is on the page now (see there).
 *
 * Reading the frame needs the page to come from the Workspace's own origin; for any other page there
 * is no document to read, and nothing is hovered or selected. `onSelectedGone` is called when a loaded
 * page lacks the selected part, or no longer has the selected block (`findTarget`). New callbacks on
 * each render do not re-attach anything.
 */
export function usePreviewElements(
    frameRef: RefObject<HTMLIFrameElement | null>,
    selected: PreviewElement | null,
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
    const [hoveredTarget, setHoveredTarget] = useState<{ node: Element; element: PreviewElement } | null>(null);

    const onFrameLoad = useCallback(() => {
        let loaded: Document | null;

        try {
            // `null`, or a SecurityError in some browsers, for a page from another origin.
            loaded = frameRef.current?.contentDocument ?? null;
        } catch {
            loaded = null;
        }

        setHoveredTarget(null);
        setDoc(loaded);
    }, [frameRef]);

    // Listens in the frame's document: hover, click to select, Escape to clear.
    useEffect(() => {
        if (!doc) {
            return;
        }

        function handleMouseOver(event: MouseEvent) {
            const target = targetOf(event.target);

            // Same part or block as before: keep the old state, so a mouse move inside it doesn't re-render and re-measure.
            setHoveredTarget((current) => (current?.node === target?.node ? current : target));
        }

        function handleMouseLeave() {
            setHoveredTarget(null);
        }

        function handleClick(event: MouseEvent) {
            const target = targetOf(event.target);

            if (target) {
                event.preventDefault();
                callbacks.current.onSelect(target.element);
            } else {
                callbacks.current.onSelect(null);
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
            const selectedNode = selected ? findTarget(doc!, selected) : null;

            setHovered(hoveredTarget?.node.isConnected ? { element: hoveredTarget.element, box: boxOf(hoveredTarget.node) } : null);
            setSelectedBox(selectedNode ? boxOf(selectedNode) : null);
            setChangedBoxes(changedKey.split('\n').flatMap((tagName) => {
                const node = tagName ? findPart(doc!, tagName) : null;

                return node ? [{ tagName, box: boxOf(node) }] : [];
            }));

            if (selected && !selectedNode) {
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
    }, [doc, frameRef, hoveredTarget, selected, changedKey]);

    // The current text of a part or block, read from the page; empty if the page no longer has it.
    const textOf = useCallback((element: PreviewElement) => {
        const node = doc ? findTarget(doc, element) : null;

        return node ? partTextOf(node) : '';
    }, [doc]);

    // Whether a part shows an image (`isImagePart`). Always false for a block: `blocksOf` never splits a part that shows an image.
    const hasImage = useCallback((element: PreviewElement) => {
        const node = doc && !element.block ? findPart(doc, element.tagName) : null;

        return node ? isImagePart(node) : false;
    }, [doc]);

    // A part or block as it is on the page now. The selection is stored at the moment of the click, and
    // after a reload a block's text may have changed, so it is re-read before a message is sent.
    // Returns `element` unchanged if the page no longer has it.
    const currentOf = useCallback((element: PreviewElement) => {
        const part = doc && element.block ? findPart(doc, element.tagName) : null;
        const node = part ? findTarget(doc!, element) : null;

        return part && node ? previewElementOf(part, node) : element;
    }, [doc]);

    return { onFrameLoad, isFrameReadable: Boolean(doc), hovered, selectedBox, changedBoxes, textOf, hasImage, currentOf };
}
