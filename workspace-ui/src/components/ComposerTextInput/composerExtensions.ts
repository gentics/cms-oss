import { mergeAttributes, Node } from '@tiptap/core';
import { Document } from '@tiptap/extension-document';
import { HardBreak } from '@tiptap/extension-hard-break';
import { Text } from '@tiptap/extension-text';
import { UndoRedo } from '@tiptap/extensions';
import { ReactNodeViewRenderer } from '@tiptap/react';

import { VERBATIM_NODE } from '@/helper/composerParts/composerParts';

import { VerbatimPassage } from './VerbatimPassage';

const Verbatim = Node.create({
    name: VERBATIM_NODE,
    group: 'inline',
    inline: true,
    // Not editable as a whole, and not selectable on its own.
    atom: true,
    selectable: false,

    addAttributes() {
        return { text: { default: '', rendered: false } };
    },

    parseHTML() {
        return [{
            tag: 'span[data-verbatim]',
            getAttrs: (element) => ({ text: element.querySelector('[data-verbatim-text]')?.textContent ?? '' }),
        }];
    },

    renderHTML({ node, HTMLAttributes }) {
        return ['span', mergeAttributes(HTMLAttributes, { 'data-verbatim': '' }), ['span', { 'data-verbatim-text': '' }, String(node.attrs.text)]];
    },

    renderText({ node }) {
        return String(node.attrs.text);
    },

    addNodeView() {
        return ReactNodeViewRenderer(VerbatimPassage, { as: 'span' });
    },
});

/**
 * The extensions of the field: inline content only (text sits in the field itself, no paragraphs),
 * line breaks, undo, and locked verbatim passages. One array for every editor, so `useEditor` sees
 * the same extensions on each render.
 */
export const composerExtensions = [Document.extend({ content: 'inline*' }), Text, HardBreak, UndoRedo, Verbatim];
