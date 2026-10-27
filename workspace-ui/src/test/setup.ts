import i18n from '@/i18n';

import '@testing-library/jest-dom/vitest';

// jsdom does no layout, but ProseMirror (the composer's Tiptap editor) measures ranges and hit-tests
// points. Empty measurements are enough for it outside a browser.
const noRects = (): DOMRectList => Object.assign([], { item: () => null });
const emptyRect = (): DOMRect => DOMRect.fromRect();

// jsdom has no media queries: none matches, as on a wide screen. A test of a phone stubs `matchMedia`.
const noMediaQuery = (query: string): MediaQueryList => ({
    matches: false,
    media: query,
    onchange: null,
    addEventListener: () => {},
    removeEventListener: () => {},
    addListener: () => {},
    removeListener: () => {},
    dispatchEvent: () => false,
});

// Only in jsdom; tests in the node environment have no DOM. Without layout there is nothing to
// scroll either (`scrollIntoView`, the @-menu following the keyboard).
if (typeof document !== 'undefined') {
    Element.prototype.scrollIntoView ??= () => {};
    Range.prototype.getClientRects ??= noRects;
    Range.prototype.getBoundingClientRect ??= emptyRect;
    document.elementFromPoint ??= () => null;
    window.matchMedia ??= noMediaQuery;
}

// The app starts in German (`src/i18n`); the tests expect its English texts unless they switch themselves.
await i18n.changeLanguage('en');
