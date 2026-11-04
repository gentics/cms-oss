import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { Editor } from '@tiptap/core';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { UiProvider } from '@/components/ui/provider';

import { InlineEditorToolbar } from './InlineEditorToolbar';
import { mountInlineEditor } from './useInlineEditor';

import '@/i18n';

let unmountEditor: (() => void) | null = null;
let leaf: HTMLElement | null = null;

// An editor on a paragraph with `html`, and its toolbar.
function setup(html: string): Editor {
    leaf = document.createElement('p');
    leaf.innerHTML = html;
    document.body.append(leaf);

    const { inlineEditor, unmount } = mountInlineEditor(leaf, { marks: true, fixedSelector: '', onApply: vi.fn(), onCancel: vi.fn() });

    unmountEditor = unmount;
    render(<InlineEditorToolbar editor={inlineEditor.editor} />, { wrapper: UiProvider });

    return inlineEditor.editor;
}

// Places the selection (the document holds inline content only, so its first position is 0), as React sees it.
function select(editor: Editor, position: number | { from: number; to: number }) {
    act(() => {
        editor.commands.setTextSelection(position);
    });
}

afterEach(() => {
    unmountEditor?.();
    unmountEditor = null;
    leaf?.remove();
});

describe('InlineEditorToolbar', () => {
    it('sets and removes a mark on the selection, and shows it as pressed there', async () => {
        const user = userEvent.setup();
        const editor = setup('Ab dem 1. Juli gilt');
        const bold = screen.getByRole('button', { name: 'Bold' });

        select(editor, { from: 0, to: 6 });
        expect(bold).toHaveAttribute('aria-pressed', 'false');

        await user.click(bold);
        expect(editor.getHTML()).toBe('<b>Ab dem</b> 1. Juli gilt');
        expect(bold).toHaveAttribute('aria-pressed', 'true');

        await user.click(screen.getByRole('button', { name: 'Superscript' }));
        expect(editor.getHTML()).toBe('<b><sup>Ab dem</sup></b> 1. Juli gilt');

        await user.click(bold);
        expect(editor.getHTML()).toBe('<sup>Ab dem</sup> 1. Juli gilt');
    });

    it('offers a link only for selected text or inside a link', () => {
        const editor = setup('Ab dem <a href="/x">1. Juli</a> gilt');
        const link = screen.getByRole('button', { name: 'Link' });

        select(editor, 2);
        expect(link).toBeDisabled();

        select(editor, 10);
        expect(link).toBeEnabled();
        expect(link).toHaveAttribute('aria-pressed', 'true');
    });

    it('links the selected text to a URL', async () => {
        const user = userEvent.setup();
        const editor = setup('Ab dem 1. Juli gilt');

        select(editor, { from: 7, to: 14 });
        await user.click(screen.getByRole('button', { name: 'Link' }));
        await user.type(screen.getByLabelText('URL'), 'https://example.com{Enter}');

        expect(editor.getHTML()).toBe('Ab dem <a href="https://example.com">1. Juli</a> gilt');
        expect(screen.queryByLabelText('URL')).not.toBeInTheDocument();
    });

    it('changes the URL of the link the caret is in, keeping its other attributes, and removes a link', async () => {
        const user = userEvent.setup();
        const editor = setup('Ab dem <a class="ext" href="/alt" target="_blank">1. Juli</a> gilt');

        select(editor, 10);
        await user.click(screen.getByRole('button', { name: 'Link' }));

        const url = screen.getByLabelText('URL');

        expect(url).toHaveValue('/alt');
        await user.clear(url);
        await user.type(url, '/neu');
        await user.click(screen.getByRole('button', { name: 'Apply' }));
        expect(editor.getHTML()).toBe('Ab dem <a class="ext" href="/neu" target="_blank">1. Juli</a> gilt');

        select(editor, 10);
        await user.click(screen.getByRole('button', { name: 'Link' }));
        await user.click(screen.getByRole('button', { name: 'Remove link' }));
        expect(editor.getHTML()).toBe('Ab dem 1. Juli gilt');
    });
});
