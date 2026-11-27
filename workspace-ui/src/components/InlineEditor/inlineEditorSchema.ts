import { type AnyExtension, Mark, Node } from '@tiptap/core';
import { Document } from '@tiptap/extension-document';
import { HardBreak } from '@tiptap/extension-hard-break';
import { Text } from '@tiptap/extension-text';
import { UndoRedo } from '@tiptap/extensions';

/*
 * The schema of the inline editor: the inline content of one element (text, line breaks, a few marks, links, and
 * elements kept as they are). It knows nothing about where the element comes from; whoever uses it says which
 * elements are kept unchanged (`fixedSelector`).
 *
 * The editor writes back what it read: a mark keeps the tag it was read from (`strong` stays `strong`), a link all
 * its attributes in their order, a fixed element its whole HTML. Content it could not write back that way is not
 * edited at all (`isSupportedContent`).
 */

/** The marks, each with the tags it is read from; the first is written for a mark set in the editor. */
export const MARKS = {
    bold: ['b', 'strong'],
    italic: ['i', 'em'],
    underline: ['u'],
    strike: ['s', 'strike', 'del'],
    subscript: ['sub'],
    superscript: ['sup'],
} as const;

export type MarkName = keyof typeof MARKS;

// Shortcuts of the marks that have one (Mod = Ctrl, or Cmd on a Mac).
const SHORTCUTS: Partial<Record<MarkName, string>> = { bold: 'Mod-b', italic: 'Mod-i', underline: 'Mod-u' };

function tagMark(name: MarkName) {
    const tags = MARKS[name];

    return Mark.create({
        name,

        addAttributes() {
            return { tag: { default: tags[0], parseHTML: (element) => element.localName, rendered: false } };
        },

        parseHTML() {
            return tags.map((tag) => ({ tag }));
        },

        renderHTML({ mark }) {
            return [String(mark.attrs.tag), 0];
        },

        addKeyboardShortcuts() {
            const shortcut = SHORTCUTS[name];

            return shortcut ? { [shortcut]: () => this.editor.commands.toggleMark(name) } : {};
        },
    });
}

/** A link's attributes, in their order (`href` among them), as the editor keeps them. */
export type LinkAttributes = [string, string][];

// A link, keeping all its attributes; an element kept as it is (`fixedSelector`) is no link here.
const Link = Mark.create<{ fixedSelector: string }>({
    name: 'link',
    inclusive: false,

    addOptions() {
        return { fixedSelector: '' };
    },

    addAttributes() {
        return {
            attributes: {
                default: [],
                parseHTML: (element): LinkAttributes => Array.from(element.attributes, ({ name, value }) => [name, value]),
                rendered: false,
            },
        };
    },

    parseHTML() {
        const { fixedSelector } = this.options;

        return [{ tag: 'a[href]', getAttrs: (element) => (fixedSelector && element.matches(fixedSelector) ? false : null) }];
    },

    renderHTML({ mark }) {
        return ['a', Object.fromEntries(mark.attrs.attributes as LinkAttributes), 0];
    },
});

/** The `href` of a link's attributes; empty without one. */
export function hrefOf(attributes: LinkAttributes): string {
    return attributes.find(([name]) => name === 'href')?.[1] ?? '';
}

/** A link's attributes with `href` set to `href`, in place if it had one. */
export function withHref(attributes: LinkAttributes, href: string): LinkAttributes {
    return attributes.some(([name]) => name === 'href')
        ? attributes.map(([name, value]) => [name, name === 'href' ? href : value])
        : [...attributes, ['href', href]];
}

// The element in `html` (the first one), created in `doc`.
function elementFrom(html: string, doc: globalThis.Document): HTMLElement | null {
    const template = doc.createElement('template');

    template.innerHTML = html;

    return template.content.firstElementChild as HTMLElement | null;
}

/** The name of the node of an element kept as it is. */
export const FIXED_NODE = 'fixed';

// An element kept as it is (`fixedSelector`): shown unchanged, not editable, written back as it was read.
const Fixed = Node.create<{ fixedSelector: string }>({
    name: FIXED_NODE,
    group: 'inline',
    inline: true,
    atom: true,
    selectable: false,

    addOptions() {
        return { fixedSelector: '' };
    },

    addAttributes() {
        return { html: { default: '', rendered: false } };
    },

    parseHTML() {
        const { fixedSelector } = this.options;

        // Before the link mark, which would take a fixed link for a plain one.
        return fixedSelector ? [{ tag: fixedSelector, priority: 100, getAttrs: (element) => ({ html: element.outerHTML }) }] : [];
    },

    renderHTML({ node }) {
        return elementFrom(String(node.attrs.html), document) ?? ['span'];
    },

    addNodeView() {
        return ({ node }) => {
            const dom = elementFrom(String(node.attrs.html), document) ?? document.createElement('span');

            dom.setAttribute('contenteditable', 'false');

            return { dom };
        };
    },
});

/** What the editor offers: `marks` turns the marks and links on; without, only text and line breaks. */
export interface InlineEditorSchemaOptions {
    marks: boolean;
    /** Selector of the elements kept as they are, e.g. embedded objects; empty for none. */
    fixedSelector: string;
}

/**
 * The editor's extensions: the content of one element as inline content, Enter and Shift+Enter for a line break
 * (Ctrl/Cmd+Enter is left to the caller), undo and redo.
 */
export function inlineEditorExtensions({ marks, fixedSelector }: InlineEditorSchemaOptions): AnyExtension[] {
    return [
        Document.extend({ content: 'inline*' }),
        Text,
        HardBreak.extend({
            addKeyboardShortcuts() {
                return { 'Enter': () => this.editor.commands.setHardBreak(), 'Shift-Enter': () => this.editor.commands.setHardBreak() };
            },
        }),
        UndoRedo,
        Fixed.configure({ fixedSelector }),
        ...(marks ? [...(Object.keys(MARKS) as MarkName[]).map(tagMark), Link.configure({ fixedSelector })] : []),
    ];
}

/**
 * Whether the editor can edit `element`'s content without losing anything: only text, line breaks, the marks
 * (without attributes, `MARKS`) and links (`a[href]`) when `marks` is on, and elements kept as they are
 * (`fixedSelector`, with all they hold). Anything else (a `span`, an image, a list, a mark with a class) is not.
 */
export function isSupportedContent(element: Element, { marks, fixedSelector }: InlineEditorSchemaOptions): boolean {
    const markTags: readonly string[] = Object.values(MARKS).flat();

    return Array.from(element.children).every((child) => {
        if (fixedSelector && child.matches(fixedSelector)) {
            return true;
        }

        if (child.localName === 'br') {
            return true;
        }

        const isMark = marks && markTags.includes(child.localName) && child.attributes.length === 0;
        const isLink = marks && child.matches('a[href]');

        return (isMark || isLink) && isSupportedContent(child, { marks, fixedSelector });
    });
}
