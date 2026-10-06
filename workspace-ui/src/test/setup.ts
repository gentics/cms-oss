import '@testing-library/jest-dom/vitest';

// jsdom does no layout, but ProseMirror (the composer's Tiptap editor) measures ranges and hit-tests
// points. Empty measurements are enough for it outside a browser.
const noRects = (): DOMRectList => Object.assign([], { item: () => null });
const emptyRect = (): DOMRect => DOMRect.fromRect();

// Only in jsdom; tests in the node environment have no DOM.
if (typeof document !== 'undefined') {
    Range.prototype.getClientRects ??= noRects;
    Range.prototype.getBoundingClientRect ??= emptyRect;
    document.elementFromPoint ??= () => null;
}
