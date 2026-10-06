// A composer's field (dashboard and chat, `ComposerTextInput`) is a `contenteditable` element, not React
// state. This file reads it into the message `parts` GenAIx expects (`readParts`) and edits the
// verbatim passages in it (the other functions). Attached files are added as `file_ref` parts later,
// in `useStartSession` and `useSendTurn`.
import type { UserMessagePart } from '@/services/apiService/genaix/types';

// A passage locked as verbatim is a non-editable span in the field; its text sits in a child of its
// own, next to the lock icon and the remove button.
const VERBATIM_ATTRIBUTE = 'data-verbatim';
const VERBATIM_TEXT_ATTRIBUTE = 'data-verbatim-text';

// Whether `node` is a locked verbatim passage.
function isVerbatim(node: Node): node is HTMLElement {
    return node instanceof HTMLElement && node.hasAttribute(VERBATIM_ATTRIBUTE);
}

/** The verbatim span `node` sits in, if any; used to tell a click on a passage's remove button. */
export function closestVerbatim(node: EventTarget | null): HTMLElement | null {
    return node instanceof Element ? node.closest<HTMLElement>(`[${VERBATIM_ATTRIBUTE}]`) : null;
}

// Typed text is free text: runs of spaces collapse, line breaks stay.
function normalizeText(text: string): string {
    return text.replace(/[^\S\n]+/g, ' ');
}

/**
 * The message parts of the field, in order: typed text as `text` parts, locked passages as
 * `verbatim` parts with `source: 'user'` and their text unchanged. Leading and trailing white space
 * of the message is dropped; an empty field gives no parts.
 */
export function readParts(field: HTMLElement): UserMessagePart[] {
    const parts: UserMessagePart[] = [];
    let text = '';

    // Ends the text collected so far as one `text` part.
    function flushText() {
        if (text) {
            parts.push({ type: 'text', text: normalizeText(text) });
            text = '';
        }
    }

    // Goes through the field in document order: text is collected, a passage becomes its own part.
    function walk(node: Node) {
        if (isVerbatim(node)) {
            flushText();
            parts.push({ type: 'verbatim', text: node.querySelector(`[${VERBATIM_TEXT_ATTRIBUTE}]`)?.textContent ?? '', source: 'user' });
        } else if (node.nodeType === Node.TEXT_NODE) {
            text += node.textContent ?? '';
        } else if (node.nodeName === 'BR') {
            text += '\n';
        } else {
            // A browser wraps a new line into a block of its own.
            if (node.nodeName === 'DIV' && node.previousSibling) {
                text += '\n';
            }

            node.childNodes.forEach(walk);
        }
    }

    field.childNodes.forEach(walk);
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

// The rendered icon and button of a passage. Its look comes from the field's CSS Module, which
// styles `[data-verbatim]` and `[data-verbatim-text]`.
interface VerbatimSpanOptions {
    /** Markup of the lock icon in front of the text. */
    iconMarkup: string;
    /** Markup of the remove button behind the text; a click on it is handled by the field. */
    removeButtonMarkup: string;
}

/**
 * Locks the text of `range` as a verbatim passage. Refused (`false`) when the range already holds
 * one, so a passage is never nested. The caret ends up behind the passage.
 */
export function markVerbatim(range: Range, options: VerbatimSpanOptions): boolean {
    if (range.cloneContents().querySelector(`[${VERBATIM_ATTRIBUTE}]`) || closestVerbatim(range.commonAncestorContainer.parentElement)) {
        return false;
    }

    // Not editable as a whole: lock icon, the text in its own element, remove button.
    const span = document.createElement('span');
    const text = document.createElement('span');

    span.setAttribute(VERBATIM_ATTRIBUTE, '');
    span.setAttribute('contenteditable', 'false');
    text.setAttribute(VERBATIM_TEXT_ATTRIBUTE, '');
    text.textContent = range.toString();
    span.innerHTML = options.iconMarkup;
    span.append(text);
    span.insertAdjacentHTML('beforeend', options.removeButtonMarkup);

    // The passage takes the place of the selected text.
    range.deleteContents();
    range.insertNode(span);

    // The caret needs a text node behind the passage to land in.
    let tail = span.nextSibling;
    let offset = 0;

    if (!tail || tail.nodeType !== Node.TEXT_NODE) {
        tail = document.createTextNode(' ');
        span.after(tail);
        offset = 1;
    }

    const caret = document.createRange();

    caret.setStart(tail, offset);
    caret.collapse(true);
    document.getSelection()?.removeAllRanges();
    document.getSelection()?.addRange(caret);

    return true;
}

/** Turns a verbatim passage back into plain text: the × in the passage. */
export function unmarkVerbatim(span: HTMLElement): void {
    span.replaceWith(document.createTextNode(span.querySelector(`[${VERBATIM_TEXT_ATTRIBUTE}]`)?.textContent ?? ''));
}
