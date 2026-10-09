// A composer's field (dashboard and chat, `ComposerTextInput`) is a Tiptap editor. Its document
// holds text, line breaks, locked verbatim passages and tokens (references, session files,
// settings). This file reads it into the message `parts` GenAIx expects (`readParts`) and builds
// content for it. Attached files are added as `file_ref` parts later, in `useStartSession` and
// `useSendTurn`.
import type { JSONContent } from '@tiptap/core';
import type { Node as ProseMirrorNode } from '@tiptap/pm/model';

import type { ContextReference, FileMode, UserMessagePart } from '@/services/apiService/genaix/types';

/**
 * The node of a locked verbatim passage: not editable, its text in the attribute `text`, where it
 * comes from in `source`: `'user'`, a session file's id, or `attachmentSource(id)` for a file
 * attached in the composer and uploaded only on sending.
 */
export const VERBATIM_NODE = 'verbatim';

/** The `source` of a passage quoted from the composer's attachment `attachmentId`. */
export function attachmentSource(attachmentId: string): string {
    return `attachment:${attachmentId}`;
}

/** The attachment id in a passage's `source`, if it names an attachment. */
export function sourceAttachmentId(source: string): string | null {
    return source.startsWith('attachment:') ? source.slice('attachment:'.length) : null;
}

const PENDING_FILE = 'pending-file:';

/**
 * The `source` of a sent passage quoting the file at `index` of the submitted files, until that
 * file is uploaded and has an id (`resolvePendingFileSources`).
 */
export function pendingFileSource(index: number): string {
    return `${PENDING_FILE}${index}`;
}

/**
 * `parts` with each passage quoting a submitted file (`pendingFileSource`) pointing at that file's
 * id after the upload, `fileIds` in the order of the submitted files.
 */
export function resolvePendingFileSources(parts: UserMessagePart[], fileIds: string[]): UserMessagePart[] {
    return parts.map((part) => {
        if (part.type !== 'verbatim' || !part.source.startsWith(PENDING_FILE)) {
            return part;
        }

        const fileId = fileIds[Number(part.source.slice(PENDING_FILE.length))];

        return { ...part, source: fileId ?? 'user' };
    });
}
/** The attribute that marks a passage's element in the field (its node view). */
export const VERBATIM_ATTRIBUTE = 'data-verbatim';
const HARD_BREAK_NODE = 'hardBreak';

/** The token of a CMS object or other context: a `reference` part, its `ContextReference` in `ref`. */
export const REFERENCE_NODE = 'reference';
/** The token of a setting: a `setting` part, from the attributes `key`, `value` and `label`. */
export const SETTING_NODE = 'setting';
/**
 * The token of an uploaded session file: a `file_ref` part, from the attributes `fileId` and `mode`;
 * `name` is only shown.
 */
export const FILE_REF_NODE = 'fileRef';
/** The attribute that marks a token's element in the field (its node view), valued by its node name. */
export const TOKEN_ATTRIBUTE = 'data-token';

/** The attributes of a reference token. */
export interface ReferenceAttrs {
    ref: ContextReference;
}

/** The attributes of a setting token; an empty `label` is not sent. */
export interface SettingAttrs {
    key: string;
    value: string;
    label: string;
}

/** The attributes of a session file token. */
export interface FileRefAttrs {
    fileId: string;
    mode: FileMode;
    name: string;
}

/** What a token shows, and its text when the field is copied as plain text. */
export function tokenLabel(node: ProseMirrorNode): string {
    switch (node.type.name) {
        case REFERENCE_NODE: {
            const { ref } = node.attrs as ReferenceAttrs;

            return ref.label ?? ref.id;
        }
        case SETTING_NODE: {
            const { value, label } = node.attrs as SettingAttrs;

            return label || value;
        }
        case FILE_REF_NODE:
            return (node.attrs as FileRefAttrs).name;
        default:
            return '';
    }
}

// The part of a token node, or `null` for any other node.
function tokenPart(node: ProseMirrorNode): UserMessagePart | null {
    switch (node.type.name) {
        case REFERENCE_NODE: {
            const { ref } = node.attrs as ReferenceAttrs;

            return { type: 'reference', ref };
        }
        case SETTING_NODE: {
            const { key, value, label } = node.attrs as SettingAttrs;

            return { type: 'setting', key, value, ...(label ? { label } : {}) };
        }
        case FILE_REF_NODE: {
            const { fileId, mode } = node.attrs as FileRefAttrs;

            return { type: 'file_ref', file_id: fileId, mode };
        }
        default:
            return null;
    }
}

// Typed text is free text: runs of spaces collapse, line breaks stay.
function normalizeText(text: string): string {
    return text.replace(/[^\S\n]+/g, ' ');
}

/**
 * The message parts of the field's document, in order: typed text as `text` parts, locked passages
 * as `verbatim` parts with their text unchanged and their `source` as stored, tokens as `reference`,
 * `setting` and `file_ref` parts. Leading and trailing white space of the message is dropped; an
 * empty field gives no parts.
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

    // The document is inline content only: text, line breaks, passages and tokens.
    doc.forEach((node) => {
        const token = tokenPart(node);

        if (token) {
            flushText();
            parts.push(token);
        } else if (node.type.name === VERBATIM_NODE) {
            flushText();
            parts.push({ type: 'verbatim', text: String(node.attrs.text), source: String(node.attrs.source ?? 'user') });
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

/** What tells two references apart: the same object is the same type, node and id. */
export function referenceKey(ref: ContextReference): string {
    return `${ref.type}:${ref.node_id ?? ''}:${ref.id}`;
}

/** The objects the `reference` parts among `parts` point at, each once, in their order. */
export function referencedObjects(parts: UserMessagePart[]): ContextReference[] {
    const refs = new Map<string, ContextReference>();

    for (const part of parts) {
        if (part.type === 'reference') {
            refs.set(referenceKey(part.ref), part.ref);
        }
    }

    return [...refs.values()];
}

/**
 * `refs` as content for the field: a reference token each, a space after each one, so the caret
 * lands behind them and the user can go on typing.
 */
export function referenceContent(refs: ContextReference[]): JSONContent[] {
    return refs.flatMap((ref): JSONContent[] => [{ type: REFERENCE_NODE, attrs: { ref } }, { type: 'text', text: ' ' }]);
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

/** Whether `range` holds a token, which a verbatim passage must not swallow. */
export function holdsToken(range: Range): boolean {
    return range.cloneContents().querySelector(`[${TOKEN_ATTRIBUTE}]`) !== null;
}
