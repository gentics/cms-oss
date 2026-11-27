import { Editor } from '@tiptap/core';
import { useEffect, useRef, useState } from 'react';

import { collapseWhiteSpace } from '@/helper/collapseWhiteSpace/collapseWhiteSpace';

import { inlineEditorExtensions, type InlineEditorSchemaOptions } from './inlineEditorSchema';

export interface InlineEditorOptions extends InlineEditorSchemaOptions {
    /** Ctrl/Cmd+Enter in the editor. */
    onApply: () => void;
    /** Escape in the editor. */
    onCancel: () => void;
}

/** The editor on an element, and what it holds now. */
export interface InlineEditor {
    editor: Editor;
    /** The element's content as HTML, as the editor writes it. */
    html: () => string;
    /** The element's content as plain text, a line break as `\n`. */
    text: () => string;
    /** Whether the content differs from what the editor read at the start, beyond white space (as the page shows it). */
    isChanged: () => boolean;
    /** The element's attributes before the editor added its own (class, `contenteditable`, …), in their order. */
    attributes: [string, string][];
    /**
     * When the editing ends, the element keeps what the editor holds then (its attributes still come back), instead
     * of its content from before, e.g. when the editing moves on to another element and this one is saved later.
     */
    keepContent: () => void;
}

/**
 * Puts the focus back into the editor, e.g. after a button outside it. Its window first: focusing an element of
 * another document (a frame) alone leaves the focus where it is.
 */
export function focusEditor(editor: Editor, position: 'end' | null = null) {
    editor.view.dom.ownerDocument.defaultView?.focus();
    editor.commands.focus(position);
}

/**
 * Makes `element` editable in place with the inline editor (`inlineEditorSchema`): the element itself becomes the
 * editor, wherever it is (also in another document, such as a frame), so it keeps its look. Focuses it with the
 * caret at the end. `unmount` removes the editor and gives the element its attributes and content back; the caller
 * decides what is kept. Without React; `useInlineEditor` ties it to a component.
 */
export function mountInlineEditor(element: HTMLElement, options: InlineEditorOptions): { inlineEditor: InlineEditor; unmount: () => void } {
    const originalHtml = element.innerHTML;
    const originalStyle = element.style.whiteSpace;
    const attributes: [string, string][] = Array.from(element.attributes, ({ name, value }) => [name, value]);

    // Ctrl/Cmd+Enter applies, Escape cancels. Both end here: a listener further up (e.g. one that clears a selection
    // on Escape) would otherwise see them after the caller has already ended the editing.
    function handleKeyDown(_view: unknown, event: KeyboardEvent): boolean {
        const isApply = event.key === 'Enter' && (event.ctrlKey || event.metaKey);

        if (!isApply && event.key !== 'Escape') {
            return false;
        }

        event.preventDefault();
        event.stopPropagation();

        if (isApply) {
            options.onApply();
        } else {
            options.onCancel();
        }

        return true;
    }
    // A link in the element is not followed while it is edited: a click there places the caret (set on mousedown, so
    // this does not stop it). The browser leaves an editable link alone, but not one kept as it is
    // (`contenteditable="false"`), nor a click with Ctrl or Cmd.
    function handleClick(event: MouseEvent) {
        const target = event.target as Node | null;
        const clicked = target?.nodeType === 1 ? (target as Element) : target?.parentElement;

        if (clicked?.closest('a[href]') && element.contains(clicked)) {
            event.preventDefault();
        }
    }

    // Text that keeps its white space (a code block) is read with it.
    const keepsWhiteSpace = element.ownerDocument.defaultView?.getComputedStyle(element).whiteSpace.startsWith('pre') ?? false;

    element.innerHTML = '';

    const editor = new Editor({
        element: { mount: element },
        extensions: inlineEditorExtensions(options),
        editorProps: { handleKeyDown },
        content: originalHtml,
        parseOptions: { preserveWhitespace: keepsWhiteSpace ? 'full' : false },
        // The editor's own CSS would go into this document's head, not the element's.
        injectCSS: false,
    });
    const startHtml = collapseWhiteSpace(editor.getHTML());
    // Set by `keepContent`.
    let keep = false;

    // What ProseMirror needs, as its CSS is not in the element's document: white space kept as typed.
    if (!keepsWhiteSpace) {
        element.style.whiteSpace = 'pre-wrap';
    }

    element.addEventListener('click', handleClick);

    focusEditor(editor, 'end');

    return {
        inlineEditor: {
            editor,
            html: () => editor.getHTML(),
            // A line break as `\n`; there is only one block, so nothing between blocks.
            text: () => editor.getText({ blockSeparator: '' }),
            isChanged: () => collapseWhiteSpace(editor.getHTML()) !== startHtml,
            attributes,
            keepContent: () => {
                keep = true;
            },
        },
        unmount: () => {
            element.removeEventListener('click', handleClick);

            // What the editor holds now, if it is to be kept; read before the editor is gone.
            const html = keep ? editor.getHTML() : originalHtml;

            editor.destroy();
            element.style.whiteSpace = originalStyle;

            for (const name of element.getAttributeNames()) {
                element.removeAttribute(name);
            }

            for (const [name, value] of attributes) {
                element.setAttribute(name, value);
            }

            element.innerHTML = html;
        },
    };
}

/**
 * The inline editor on `element` while it is given (`mountInlineEditor`); `null` ends the editing. Returns the
 * editor while it is on the element, else `null`. `options` is read when the editing starts, except `onApply` and
 * `onCancel`: the latest are called.
 */
export function useInlineEditor(element: HTMLElement | null, options: InlineEditorOptions): InlineEditor | null {
    const [mounted, setMounted] = useState<{ element: HTMLElement; inlineEditor: InlineEditor } | null>(null);
    const optionsRef = useRef(options);

    useEffect(() => {
        optionsRef.current = options;
    });

    useEffect(() => {
        if (!element) {
            return;
        }

        const { inlineEditor, unmount } = mountInlineEditor(element, {
            ...optionsRef.current,
            onApply: () => optionsRef.current.onApply(),
            onCancel: () => optionsRef.current.onCancel(),
        });

        setMounted({ element, inlineEditor });

        return unmount;
    }, [element]);

    // Only the editor of the current element: right after a change, the one of the element before is gone.
    return mounted?.element === element ? mounted.inlineEditor : null;
}
