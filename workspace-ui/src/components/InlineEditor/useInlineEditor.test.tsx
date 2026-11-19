import { act, renderHook } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { mountInlineEditor, useInlineEditor } from './useInlineEditor';

const OPTIONS = { marks: true, fixedSelector: '[data-fixed]' };

// A paragraph on the page, as the editor finds it.
function paragraph(html = 'Ab dem <b>1. Juli</b> gilt'): HTMLElement {
    document.body.innerHTML = `<p id="leaf" class="lead" data-x="1">${html}</p><p id="other">Andere</p>`;

    return document.getElementById('leaf')!;
}

function press(element: HTMLElement, key: string, init: KeyboardEventInit = {}) {
    const event = new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true, ...init });

    element.dispatchEvent(event);

    return event;
}

afterEach(() => {
    document.body.innerHTML = '';
});

describe('mountInlineEditor', () => {
    it('makes the element itself the editor, with its content', () => {
        const element = paragraph();
        const { inlineEditor, unmount } = mountInlineEditor(element, { ...OPTIONS, onApply: vi.fn(), onCancel: vi.fn() });

        expect(element.getAttribute('contenteditable')).toBe('true');
        expect(inlineEditor.html()).toBe('Ab dem <b>1. Juli</b> gilt');
        expect(inlineEditor.text()).toBe('Ab dem 1. Juli gilt');
        expect(inlineEditor.attributes).toEqual([['id', 'leaf'], ['class', 'lead'], ['data-x', '1']]);
        unmount();
    });

    it('tells a change from white space only', () => {
        const element = paragraph();
        const { inlineEditor, unmount } = mountInlineEditor(element, { ...OPTIONS, onApply: vi.fn(), onCancel: vi.fn() });

        inlineEditor.editor.commands.insertContentAt(inlineEditor.editor.state.doc.content.size, '  ');
        expect(inlineEditor.isChanged()).toBe(false);

        inlineEditor.editor.commands.insertContentAt(inlineEditor.editor.state.doc.content.size, 'sofort');
        expect(inlineEditor.isChanged()).toBe(true);
        expect(inlineEditor.html()).toBe('Ab dem <b>1. Juli</b> gilt  sofort');
        unmount();
    });

    it('applies on Ctrl/Cmd+Enter and cancels on Escape, and keeps both keys to itself', () => {
        const element = paragraph();
        const onApply = vi.fn();
        const onCancel = vi.fn();
        const onDocumentKey = vi.fn();
        const { unmount } = mountInlineEditor(element, { ...OPTIONS, onApply, onCancel });

        document.addEventListener('keydown', onDocumentKey);

        expect(press(element, 'Enter', { ctrlKey: true }).defaultPrevented).toBe(true);
        expect(press(element, 'Enter', { metaKey: true }).defaultPrevented).toBe(true);
        expect(press(element, 'Escape').defaultPrevented).toBe(true);

        expect(onApply).toHaveBeenCalledTimes(2);
        expect(onCancel).toHaveBeenCalledTimes(1);
        expect(onDocumentKey).not.toHaveBeenCalled();

        document.removeEventListener('keydown', onDocumentKey);
        unmount();
    });

    it('does not follow a link in it, neither an edited one nor a fixed one', () => {
        const element = paragraph('Siehe <a href="/dort">dort</a> und <a data-fixed="" href="/kontakt"><b>Kontakt</b></a>');
        const { unmount } = mountInlineEditor(element, { ...OPTIONS, onApply: vi.fn(), onCancel: vi.fn() });
        const click = (target: Element) => {
            const event = new MouseEvent('click', { bubbles: true, cancelable: true });

            target.dispatchEvent(event);

            return event.defaultPrevented;
        };

        expect(click(element.querySelector('a[href="/dort"]')!)).toBe(true);
        expect(click(element.querySelector('a[href="/kontakt"] b')!)).toBe(true);
        expect(click(element)).toBe(false);
        unmount();

        // Once the editing has ended, links are links again.
        expect(click(element.querySelector('a[href="/dort"]')!)).toBe(false);
    });

    it('leaves the element what the editor holds when it ends, after keepContent, with its attributes back', () => {
        const element = paragraph();
        const { inlineEditor, unmount } = mountInlineEditor(element, { ...OPTIONS, onApply: vi.fn(), onCancel: vi.fn() });

        inlineEditor.keepContent();
        // What it holds at the end counts, also after keepContent.
        inlineEditor.editor.commands.setContent('Ab dem <i>1. August</i> gilt');
        unmount();

        expect(element.outerHTML).toBe('<p id="leaf" class="lead" data-x="1">Ab dem <i>1. August</i> gilt</p>');
    });

    it('gives the element its attributes and content back when it ends, whatever was edited', () => {
        const element = paragraph();
        const { inlineEditor, unmount } = mountInlineEditor(element, { ...OPTIONS, onApply: vi.fn(), onCancel: vi.fn() });

        inlineEditor.editor.commands.setContent('Etwas anderes');
        unmount();

        expect(element.outerHTML).toBe('<p id="leaf" class="lead" data-x="1">Ab dem <b>1. Juli</b> gilt</p>');
        expect(inlineEditor.editor.isDestroyed).toBe(true);
    });
});

describe('useInlineEditor', () => {
    it('puts the editor on the element while it is given, moves it with the element, and ends it with null', () => {
        paragraph();

        const leaf = document.getElementById('leaf')!;
        const other = document.getElementById('other')!;
        const { result, rerender } = renderHook(({ element }) => useInlineEditor(element, { ...OPTIONS, onApply: vi.fn(), onCancel: vi.fn() }), {
            initialProps: { element: leaf as HTMLElement | null },
        });

        expect(result.current?.editor.view.dom).toBe(leaf);

        rerender({ element: other });
        expect(result.current?.editor.view.dom).toBe(other);
        expect(leaf.hasAttribute('contenteditable')).toBe(false);

        rerender({ element: null });
        expect(result.current).toBeNull();
        expect(other.outerHTML).toBe('<p id="other">Andere</p>');
    });

    it('calls the latest onApply', () => {
        paragraph();

        const first = vi.fn();
        const latest = vi.fn();
        const leaf = document.getElementById('leaf')!;
        const { rerender, unmount } = renderHook(({ onApply }) => useInlineEditor(leaf, { ...OPTIONS, onApply, onCancel: vi.fn() }), {
            initialProps: { onApply: first },
        });

        rerender({ onApply: latest });
        act(() => {
            press(leaf, 'Enter', { ctrlKey: true });
        });

        expect(first).not.toHaveBeenCalled();
        expect(latest).toHaveBeenCalledTimes(1);
        unmount();
    });
});
