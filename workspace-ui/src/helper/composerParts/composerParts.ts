// A composer's field (dashboard and chat, `ComposerTextInput`) is a Tiptap editor. Its document
// holds text, line breaks, locked verbatim passages and tokens (references, session files,
// settings, parts of the previewed page). This file reads it into the message `parts` GenAIx expects (`readParts`) and builds
// content for it. Attached files are added as `file_ref` parts later, in `useStartSession` and
// `useSendTurn`.
import type { JSONContent } from '@tiptap/core';
import type { Node as ProseMirrorNode } from '@tiptap/pm/model';
import type { TFunction } from 'i18next';

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

/**
 * The references of `parts` (`reference` parts, e.g. an @-mention), in order and each object once:
 * what a new session's `SessionContext.references` holds, so the session keeps them for every run.
 */
export function partReferences(parts: UserMessagePart[]): ContextReference[] {
    const references = new Map<string, ContextReference>();

    for (const part of parts) {
        if (part.type !== 'reference') {
            continue;
        }

        const key = `${part.ref.type}:${part.ref.node_id ?? ''}:${part.ref.id}`;

        if (!references.has(key)) {
            references.set(key, part.ref);
        }
    }

    return [...references.values()];
}
/** The attribute that marks a passage's element in the field (its node view). */
export const VERBATIM_ATTRIBUTE = 'data-verbatim';
export const HARD_BREAK_NODE = 'hardBreak';

/** The token of a CMS object or other context: a `reference` part, its `ContextReference` in `ref`. */
export const REFERENCE_NODE = 'reference';
/** The token of a setting: a `setting` part, from the attributes `key`, `value` and `label`. */
export const SETTING_NODE = 'setting';
/**
 * The token of one part (tag) of a page, picked in the preview. `ContextReference` stops at the page,
 * so the part goes out as a `reference` part for the page, then `text`, which names the tag in the
 * user's language, in front of what follows.
 */
export const PAGE_ELEMENT_NODE = 'pageElement';
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

/**
 * One block inside a page part, e.g. the headline of an Aloha text that also holds a paragraph (the
 * preview's `PreviewBlock`). The API has no field for a block inside a tag, so the naming text of the
 * message says which block is meant: by position, kind and the start of its text.
 */
export interface PageElementBlock {
    /** Position among the part's blocks, starting at 0. */
    index: number;
    /** Total number of blocks in the part. */
    count: number;
    /** Its kind, already translated, e.g. "Heading 1". */
    label: string;
    /** The first characters of its text. */
    excerpt: string;
}

/** The attributes of a page part token. */
export interface PageElementAttrs {
    /** The page the part belongs to, as a `page` reference. */
    page: ContextReference;
    /** The tag name, e.g. `content_2`. */
    tagName: string;
    /** The name of its type, e.g. "Text"; empty when unknown. */
    constructName: string;
    /** The block of the part that is meant; `null` or absent means the whole part. */
    block?: PageElementBlock | null;
    /** What names the part in the message, e.g. ` – part "content_2" (Text): `. */
    text: string;
}

type PageElementName = Pick<PageElementAttrs, 'tagName' | 'constructName' | 'block'>;

/**
 * The label of a page part on its chat token and on the preview's cards, e.g. "Text · content_2". For a
 * block, its kind is appended: "Text · content_2 · Heading 1".
 */
export function pageElementLabel({ tagName, constructName, block }: PageElementName): string {
    return [constructName, tagName, block?.label].filter(Boolean).join(' · ');
}

// Translates the naming text of a part. The i18n key is `key`, plus `WithoutType` when the part has no
// construct name, plus `Block` when a block is meant (e.g. `replaceWithWithoutTypeBlock`).
function nameElement(t: TFunction, key: string, { tagName, constructName, block }: PageElementName): string {
    const name = `composer.pageElement.${key}${constructName ? '' : 'WithoutType'}${block ? 'Block' : ''}`;

    return block
        ? t(name, { tagName, constructName, position: block.index + 1, count: block.count, kind: block.label, excerpt: block.excerpt })
        : t(name, { tagName, constructName });
}

/**
 * The text that names a part of a page in a message (`PageElementAttrs.text`), in the user's language;
 * for a block, also its position, kind and the start of its text.
 */
export function pageElementNaming(t: TFunction, element: PageElementName): string {
    return nameElement(t, 'naming', element);
}

/**
 * The parts of a message about a part of a page, sent without the field (the preview's feedback
 * card): as a token in the field would be read (`readParts`), the page as a `reference` part, then
 * the naming text and `text` as one `text` part.
 */
export function pageElementParts({ page, text: naming }: PageElementAttrs, text: string): UserMessagePart[] {
    return [{ type: 'reference', ref: page }, { type: 'text', text: normalizeText(naming + text).trimEnd() }];
}

/**
 * The text that names a part of a page and asks to replace its text, in the user's language: what
 * precedes the new text of a hand edit (`pageElementEditParts`). For a block, also its position, kind
 * and the start of its text.
 */
export function pageElementReplaceNaming(t: TFunction, element: PageElementName): string {
    return nameElement(t, 'replaceWith', element);
}

/**
 * The parts of a hand edit of a part of a page (the preview's edit card): the page as a `reference`
 * part, the naming text (`pageElementReplaceNaming`) as a `text` part, then `text` as a `verbatim`
 * part the user wrote, unchanged, so that GenAIx writes it character-identical; its label names the part
 * as its token does (`pageElementLabel`, e.g. "Text · content_2"), for the check and the audit trail. The part is named
 * as for `PAGE_ELEMENT_NODE`; the request to replace its text is an interim wording, until GenAIx
 * defines how a hand edit is sent.
 */
export function pageElementEditParts({ page, text: naming, ...element }: PageElementAttrs, text: string): UserMessagePart[] {
    return [
        { type: 'reference', ref: page },
        { type: 'text', text: normalizeText(naming) },
        { type: 'verbatim', text, source: 'user', label: pageElementLabel(element) },
    ];
}

/**
 * The text that names a part of a page and asks to replace its image, in the user's language: what
 * precedes the chosen image (`pageElementImageParts`).
 */
export function pageElementReplaceImageNaming(t: TFunction, { tagName, constructName }: Pick<PageElementAttrs, 'tagName' | 'constructName'>): string {
    return constructName
        ? t('composer.pageElement.replaceImageWith', { tagName, constructName })
        : t('composer.pageElement.replaceImageWithWithoutType', { tagName });
}

/**
 * The parts of an image swap of a part of a page (the preview's image card): the page as a `reference`
 * part, the naming text (`pageElementReplaceImageNaming`) as a `text` part, then the chosen CMS image
 * as a `reference` part. Without `image` the parts end at the naming text: an uploaded image follows
 * as the turn's `file_ref` part. The part is named as for `PAGE_ELEMENT_NODE`; the request to replace
 * its image is an interim wording, until GenAIx defines how an image swap is sent.
 */
export function pageElementImageParts({ page, text: naming }: PageElementAttrs, image?: ContextReference): UserMessagePart[] {
    return [
        { type: 'reference', ref: page },
        { type: 'text', text: normalizeText(naming) },
        ...(image ? [{ type: 'reference' as const, ref: image }] : []),
    ];
}

/** One changed page setting: the field's label as the user saw it and its new value, the template as a `template` reference. */
export interface PageSettingsChange {
    label: string;
    value: string | ContextReference;
}

/**
 * The parts of a change of a page's settings (the preview's page settings): the page as a `reference`
 * part, a text asking to change its settings, then per change a text naming the field followed by the
 * new value, as a `verbatim` part the user wrote, unchanged and labelled with the field, or as the
 * `reference` part of a template. An emptied field is asked to be emptied. The wording is interim,
 * until GenAIx defines how page settings are sent.
 */
export function pageSettingsParts(t: TFunction, page: ContextReference, changes: PageSettingsChange[]): UserMessagePart[] {
    return [
        { type: 'reference', ref: page },
        { type: 'text', text: t('composer.pageSettings.change') },
        ...changes.flatMap(({ label, value }): UserMessagePart[] => {
            const naming = `\n${label}: `;

            if (typeof value !== 'string') {
                return [{ type: 'text', text: naming }, { type: 'reference', ref: value }];
            }

            return value === ''
                ? [{ type: 'text', text: naming + t('composer.pageSettings.empty') }]
                : [{ type: 'text', text: naming }, { type: 'verbatim', text: value, source: 'user', label }];
        }),
    ];
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
        case PAGE_ELEMENT_NODE:
            return pageElementLabel(node.attrs as PageElementAttrs);
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
 * `setting` and `file_ref` parts, a page part as a `reference` part for its page and its naming
 * text joined with the text that follows (`PAGE_ELEMENT_NODE`). Leading and trailing white space of the message is dropped; an
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

        if (node.type.name === PAGE_ELEMENT_NODE) {
            const { page, text: naming } = node.attrs as PageElementAttrs;

            flushText();
            parts.push({ type: 'reference', ref: page });
            text += naming;
        } else if (token) {
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
