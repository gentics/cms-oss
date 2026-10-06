// A composer's field (dashboard and chat, `ComposerTextInput`) is a Tiptap editor. Its document
// holds text, line breaks and locked verbatim passages. This file reads it into the message `parts`
// GenAIx expects (`readParts`) and builds content for it. Attached files are added as `file_ref`
// parts later, in `useStartSession` and `useSendTurn`.
import type { JSONContent } from '@tiptap/core';
import type { Node as ProseMirrorNode } from '@tiptap/pm/model';

import type { UserMessagePart } from '@/services/apiService/genaix/types';

/** The node of a locked verbatim passage: not editable, its text in the attribute `text`. */
export const VERBATIM_NODE = 'verbatim';
/** The attribute that marks a passage's element in the field (its node view). */
export const VERBATIM_ATTRIBUTE = 'data-verbatim';
const HARD_BREAK_NODE = 'hardBreak';

// Typed text is free text: runs of spaces collapse, line breaks stay.
function normalizeText(text: string): string {
    return text.replace(/[^\S\n]+/g, ' ');
}

/**
 * The message parts of the field's document, in order: typed text as `text` parts, locked passages
 * as `verbatim` parts with `source: 'user'` and their text unchanged. Leading and trailing white
 * space of the message is dropped; an empty field gives no parts.
 */
export function readParts(doc: ProseMirrorNode): UserMessagePart[] {
    const parts: UserMessagePart[] = [];
    let text = '';

    // Ends the text collected so far as one `text` part.
    function flushText() {
        if (text) {
            parts.push({ type: 'text', text: normalizeText(text) });
            text = '';
        }
    }

    // The document is inline content only: text, line breaks and passages.
    doc.forEach((node) => {
        if (node.type.name === VERBATIM_NODE) {
            flushText();
            parts.push({ type: 'verbatim', text: String(node.attrs.text), source: 'user' });
        } else if (node.type.name === HARD_BREAK_NODE) {
            text += '\n';
        } else {
            text += node.text ?? '';
        }
    });
    flushText();

    // White space at the very start and end of the message is not part of it.
    const first = parts[0];
    const last = parts[parts.length - 1];

    if (first?.type === 'text') {
        first.text = first.text.trimStart();
    }

    if (last?.type === 'text') {
        last.text = last.text.trimEnd();
    }

    return parts.filter((part) => part.type !== 'text' || part.text !== '');
}

/** `text` as content for the field, taken literally (no HTML); a `\n` becomes a line break. */
export function textContent(text: string): JSONContent[] {
    return text.split('\n').flatMap((line, index): JSONContent[] => [
        ...(index > 0 ? [{ type: HARD_BREAK_NODE }] : []),
        // A document holds no empty text nodes.
        ...(line ? [{ type: 'text', text: line }] : []),
    ]);
}

/**
 * The selection, if it is a non-empty range of text inside `field`: what the Verbatim button would
 * lock, and whether it shows as pressed.
 */
export function selectedRange(field: HTMLElement): Range | null {
    const selection = document.getSelection();

    if (!selection || selection.rangeCount === 0 || selection.isCollapsed) {
        return null;
    }

    const range = selection.getRangeAt(0);

    if (!field.contains(range.commonAncestorContainer) || !range.toString().trim()) {
        return null;
    }

    return range;
}

/** Whether `range` holds a locked passage or lies in one; a passage is never nested. */
export function touchesVerbatim(range: Range): boolean {
    const selector = `[${VERBATIM_ATTRIBUTE}]`;

    return range.cloneContents().querySelector(selector) !== null
        || (range.commonAncestorContainer.parentElement?.closest(selector) ?? null) !== null;
}
