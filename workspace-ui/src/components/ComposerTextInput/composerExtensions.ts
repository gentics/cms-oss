import { type Editor, Extension, mergeAttributes, Node, type Range } from '@tiptap/core';
import { Document } from '@tiptap/extension-document';
import { HardBreak } from '@tiptap/extension-hard-break';
import { Text } from '@tiptap/extension-text';
import { UndoRedo } from '@tiptap/extensions';
import { type EditorState, PluginKey } from '@tiptap/pm/state';
import { ReactNodeViewRenderer } from '@tiptap/react';
import { Suggestion, type SuggestionProps } from '@tiptap/suggestion';

import { FILE_REF_NODE, REFERENCE_NODE, SETTING_NODE, TOKEN_ATTRIBUTE, tokenLabel, VERBATIM_NODE } from '@/helper/composerParts/composerParts';

import { TokenChip } from './TokenChip';
import { VerbatimPassage } from './VerbatimPassage';

const Verbatim = Node.create({
    name: VERBATIM_NODE,
    group: 'inline',
    inline: true,
    // Not editable as a whole, and not selectable on its own.
    atom: true,
    selectable: false,

    addAttributes() {
        return {
            text: { default: '', rendered: false },
            source: { default: 'user', rendered: false },
            // The name of the file in `source`, as the user picked it.
            sourceLabel: { default: '', rendered: false },
        };
    },

    parseHTML() {
        return [{
            tag: 'span[data-verbatim]',
            getAttrs: (element) => ({
                text: element.querySelector('[data-verbatim-text]')?.textContent ?? '',
                source: element.getAttribute('data-source') ?? 'user',
                sourceLabel: element.getAttribute('data-source-label') ?? '',
            }),
        }];
    },

    renderHTML({ node, HTMLAttributes }) {
        return [
            'span',
            mergeAttributes(HTMLAttributes, { 'data-verbatim': '', 'data-source': String(node.attrs.source), 'data-source-label': String(node.attrs.sourceLabel) }),
            ['span', { 'data-verbatim-text': '' }, String(node.attrs.text)],
        ];
    },

    renderText({ node }) {
        return String(node.attrs.text);
    },

    addNodeView() {
        return ReactNodeViewRenderer(VerbatimPassage, { as: 'span' });
    },
});

// The attributes of a token as HTML (copy and paste inside the field), as JSON.
const ATTRS_ATTRIBUTE = 'data-attrs';

/**
 * A token: an inline chip like a verbatim passage, not editable, its part in its attributes
 * (`composerParts`). In HTML its attributes travel as JSON; an element whose JSON does not parse is
 * not taken as a token.
 */
function token(name: string, defaults: Record<string, unknown>) {
    return Node.create({
        name,
        group: 'inline',
        inline: true,
        atom: true,
        selectable: false,

        addAttributes() {
            return Object.fromEntries(Object.entries(defaults).map(([key, value]) => [key, { default: value, rendered: false }]));
        },

        parseHTML() {
            return [{
                tag: `span[${TOKEN_ATTRIBUTE}="${name}"]`,
                getAttrs: (element) => {
                    try {
                        const attrs: unknown = JSON.parse(element.getAttribute(ATTRS_ATTRIBUTE) ?? '');

                        return typeof attrs === 'object' && attrs !== null ? attrs : false;
                    } catch {
                        return false;
                    }
                },
            }];
        },

        renderHTML({ node, HTMLAttributes }) {
            return ['span', mergeAttributes(HTMLAttributes, { [TOKEN_ATTRIBUTE]: name, [ATTRS_ATTRIBUTE]: JSON.stringify(node.attrs) }), tokenLabel(node)];
        },

        renderText({ node }) {
            return tokenLabel(node);
        },

        addNodeView() {
            return ReactNodeViewRenderer(TokenChip, { as: 'span' });
        },
    });
}

/** The plugin of the @-menu's trigger; `exitSuggestion(view, referenceTriggerKey)` closes the menu. */
export const referenceTriggerKey = new PluginKey<{ active: boolean }>('referenceTrigger');

/** Whether the @-menu is open in the editor with `state`; it then takes Enter and the arrow keys. */
export function isReferenceMenuOpen(state: EditorState): boolean {
    return referenceTriggerKey.getState(state)?.active === true;
}

/** An open @-menu: what follows the `@`, where `@query` sits, and the element the menu renders into. */
export interface ReferenceTrigger {
    query: string;
    range: Range;
    host: HTMLElement;
}

/** Who follows the @-menu of one editor (`ReferenceMenu`). */
export interface ReferenceTriggerListener {
    /** The menu opened or its query changed; `null` once it closed. */
    onChange: (trigger: ReferenceTrigger | null) => void;
    /** A key pressed while the menu is open; `true` if the menu took it. */
    onKeyDown: (event: KeyboardEvent) => boolean;
}

declare module '@tiptap/core' {
    interface Storage {
        referenceTrigger: { listener: ReferenceTriggerListener | null };
    }
}

/**
 * Makes `listener` the one that follows the @-menu of `editor`. Returns a
 * function that undoes it, unless another listener took over meanwhile.
 */
export function followReferenceTrigger(editor: Editor, listener: ReferenceTriggerListener): () => void {
    const storage = editor.storage.referenceTrigger;

    storage.listener = listener;

    return () => {
        if (storage.listener === listener) {
            storage.listener = null;
        }
    };
}

/**
 * Opens the @-menu: `@` at the start, after a space, a line break or a token, and the text up to the
 * next space as its query (`@tiptap/suggestion`). The plugin places the menu's element at the `@`
 * and closes it on Esc or a click elsewhere; what the menu shows is React's (`ReferenceMenu`).
 */
const ReferenceTriggerExtension = Extension.create<object, { listener: ReferenceTriggerListener | null }>({
    name: 'referenceTrigger',

    addStorage() {
        return { listener: null };
    },

    addProseMirrorPlugins() {
        const { storage } = this;

        return [Suggestion({
            editor: this.editor,
            pluginKey: referenceTriggerKey,
            char: '@',
            placement: 'top-start',
            render: () => {
                let host: HTMLElement | null = null;
                let unmount: (() => void) | null = null;

                function change({ query, range }: SuggestionProps) {
                    if (host) {
                        storage.listener?.onChange({ query, range, host });
                    }
                }

                return {
                    onStart: (props) => {
                        host = document.createElement('div');
                        unmount = props.mount(host);
                        change(props);
                    },
                    onUpdate: change,
                    onExit: () => {
                        storage.listener?.onChange(null);
                        unmount?.();
                        host = null;
                        unmount = null;
                    },
                    onKeyDown: ({ event }) => storage.listener?.onKeyDown(event) ?? false,
                };
            },
        })];
    },
});

const Reference = token(REFERENCE_NODE, { ref: null });
const Setting = token(SETTING_NODE, { key: '', value: '', label: '' });
const FileRef = token(FILE_REF_NODE, { fileId: '', mode: 'source', name: '' });

/**
 * The extensions of the field: inline content only (text sits in the field itself, no paragraphs),
 * line breaks, undo, locked verbatim passages, tokens and the @-menu's trigger. One array for every
 * editor, so `useEditor` sees the same extensions on each render.
 */
export const composerExtensions = [Document.extend({ content: 'inline*' }), Text, HardBreak, UndoRedo, Verbatim, Reference, Setting, FileRef, ReferenceTriggerExtension];
