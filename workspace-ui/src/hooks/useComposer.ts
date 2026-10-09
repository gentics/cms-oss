import { TextSelection } from '@tiptap/pm/state';
import { useEditor } from '@tiptap/react';
import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { composerExtensions } from '@/components/ComposerTextInput/composerExtensions';
import { useToast } from '@/components/ui/use-toast';
import { holdsToken, pendingFileSource, readParts, REFERENCE_NODE, type ReferenceAttrs, referenceContent, referencedObjects, referenceKey, selectedRange, sourceAttachmentId, textContent, touchesVerbatim, VERBATIM_NODE } from '@/helper/composerParts/composerParts';
import { checkUploadFile, formatMegabytes, UPLOAD_MAX_BYTES } from '@/helper/uploadLimits/uploadLimits';
import type { StartFile, StartSessionInput } from '@/hooks/useGenaixQueries';
import type { ContextReference } from '@/services/apiService/genaix/types';

/** A file attached in a composer, before it is uploaded. */
export interface Attachment extends StartFile {
    id: string;
}

/**
 * The state of the `Composer` (dashboard and session chat): the field, a Tiptap editor with its
 * verbatim passages (`composerParts`), and the attached files within the upload limits
 * (`uploadLimits`). The caller decides what a submit does. `references` are the objects the field's
 * tokens point at.
 */
export function useComposer() {
    const { t, i18n } = useTranslation();
    const toast = useToast();
    const editor = useEditor({ extensions: composerExtensions });
    const fileInputRef = useRef<HTMLInputElement>(null);
    const [isEmpty, setIsEmpty] = useState(true);
    const [hasSelection, setHasSelection] = useState(false);
    const [attachments, setAttachments] = useState<Attachment[]>([]);
    const [references, setReferences] = useState<ContextReference[]>([]);

    // Typing, dictation, a passage locked or unlocked, a token added or removed: every change.
    useEffect(() => {
        function onUpdate() {
            const parts = readParts(editor.state.doc);
            const next = referencedObjects(parts);

            setIsEmpty(parts.length === 0);
            // A new array only when the objects changed, so typing does not count as a change.
            setReferences((current) => (current.map(referenceKey).join() === next.map(referenceKey).join() ? current : next));
        }

        editor.on('update', onUpdate);

        return () => {
            editor.off('update', onUpdate);
        };
    }, [editor]);

    /** Replaces the field's content with `text`, caret at the end so it can be continued. */
    function setText(text: string) {
        editor.chain()
            .setContent({ type: 'doc', content: textContent(text) })
            .command(({ tr }) => {
                tr.setSelection(TextSelection.atEnd(tr.doc));

                return true;
            })
            .run();
    }

    /**
     * The message parts and files to send, or `null` while there is nothing to send. A passage
     * quoting an attachment names it by its place in `files` (`pendingFileSource`), filled in with
     * the file's id after the upload; one whose attachment was removed counts as typed.
     */
    function readInput(): StartSessionInput | null {
        const parts = readParts(editor.state.doc).map((part) => {
            const attachmentId = part.type === 'verbatim' ? sourceAttachmentId(part.source) : null;

            if (part.type !== 'verbatim' || attachmentId === null) {
                return part;
            }

            const index = attachments.findIndex((attachment) => attachment.id === attachmentId);

            return { ...part, source: index === -1 ? 'user' : pendingFileSource(index) };
        });

        if (parts.length === 0 && attachments.length === 0) {
            return null;
        }

        return { parts, files: attachments.map(({ file, mode }) => ({ file, mode })) };
    }

    /**
     * Puts a reference token for each of `refs` at the start of the field, before what is typed
     * there, and the caret at the end, so the instruction can be typed behind them.
     */
    function addReferences(refs: ContextReference[]) {
        // A browser selection left in the field from before (it lost the focus, its content changed)
        // can stand for the same place as the new caret, so ProseMirror keeps it, while the browser
        // types at the start of the field. Without it, focusing sets a fresh one.
        if (!editor.isFocused) {
            window.getSelection()?.removeAllRanges();
        }

        // `focus` with a position: without one it would put back the selection from before the insert.
        editor.chain().insertContentAt(0, referenceContent(refs)).focus('end').run();
    }

    /** Removes the reference tokens of `refs` from the field, and the space each was followed by. */
    function removeReferences(refs: ContextReference[]) {
        const keys = new Set(refs.map(referenceKey));

        editor.chain().command(({ tr }) => {
            const ranges: { from: number; to: number }[] = [];

            tr.doc.descendants((node, pos) => {
                if (node.type.name === REFERENCE_NODE && keys.has(referenceKey((node.attrs as ReferenceAttrs).ref))) {
                    const after = tr.doc.textBetween(pos + node.nodeSize, Math.min(pos + node.nodeSize + 1, tr.doc.content.size));

                    ranges.push({ from: pos, to: pos + node.nodeSize + (after === ' ' ? 1 : 0) });
                }
            });
            // From the end, so the earlier positions stay valid.
            ranges.reverse().forEach(({ from, to }) => tr.delete(from, to));

            return true;
        }).run();
    }

    /** Empties the field and drops the attachments, after a message was sent. */
    function reset() {
        editor.commands.clearContent();
        setIsEmpty(true);
        setReferences([]);
        setAttachments([]);
    }

    // The verbatim button is pressed while there is a selection in the field it would lock.
    useEffect(() => {
        function onSelectionChange() {
            setHasSelection(selectedRange(editor.view.dom) !== null);
        }

        document.addEventListener('selectionchange', onSelectionChange);

        return () => document.removeEventListener('selectionchange', onSelectionChange);
    }, [editor]);

    /** Locks the selected text as verbatim; without a selection, or inside a passage, says why not. */
    function markSelectionVerbatim() {
        const { view } = editor;
        const range = selectedRange(view.dom);

        view.dom.focus();

        if (!range) {
            toast.add({ title: t('composer.verbatimHint') });

            return;
        }

        if (touchesVerbatim(range)) {
            toast.add({ title: t('composer.verbatimNested') });

            return;
        }

        // A passage holds text only; locking a token into it would lose the token.
        if (holdsToken(range)) {
            toast.add({ title: t('composer.verbatimHasToken') });

            return;
        }

        // The selection as the user sees it, in document positions.
        const from = view.posAtDOM(range.startContainer, range.startOffset);
        const to = view.posAtDOM(range.endContainer, range.endOffset);
        // The caret needs text behind the passage to land in.
        const tail = editor.state.doc.resolve(to).nodeAfter?.isText ? [] : [{ type: 'text', text: ' ' }];

        editor.chain().insertContentAt({ from, to }, [{ type: VERBATIM_NODE, attrs: { text: range.toString() } }, ...tail]).run();
    }

    /**
     * Opens the @-menu at the caret: types `@`, after a space where the caret follows a word (the
     * menu opens only for an `@` that starts a word, `composerExtensions`).
     */
    function openReferenceMenu() {
        const before = editor.state.selection.$from.nodeBefore;
        const needsSpace = before?.isText === true && !/\s$/.test(before.text ?? '');

        editor.chain().focus().insertContent(needsSpace ? ' @' : '@').run();
    }

    /** Opens the browser's file picker (the hidden `<input type="file">` behind `fileInputRef`). */
    function openFilePicker() {
        fileInputRef.current!.click();
    }

    // An error toast for a file that cannot be attached, naming it and the limit it breaks. Stays
    // until it is closed, like the app's other error toasts (`ErrorNotifications`).
    function rejectFile(file: File, problem: NonNullable<ReturnType<typeof checkUploadFile>>) {
        toast.add({
            type: 'error',
            priority: 'low',
            timeout: 0,
            title: t('composer.attachment.notAdded', { name: file.name }),
            description: problem.reason === 'tooLarge'
                ? t('composer.attachment.tooLarge', {
                    size: formatMegabytes(file.size, i18n.language),
                    limit: formatMegabytes(UPLOAD_MAX_BYTES, i18n.language),
                })
                : t('composer.attachment.typeNotAccepted', { type: problem.type }),
        });
    }

    /**
     * Attaches picked or dropped files as a source; one over the limits is not attached, and an
     * error toast says why.
     */
    function addFiles(files: FileList | null) {
        const added: Attachment[] = [];

        for (const file of Array.from(files ?? [])) {
            const problem = checkUploadFile(file);

            if (problem) {
                rejectFile(file, problem);
            } else {
                added.push({ id: crypto.randomUUID(), file, mode: 'source' });
            }
        }

        setAttachments((current) => [...current, ...added]);

        // The same file can be picked again after it was removed.
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    }

    function toggleMode(id: string) {
        setAttachments((current) => current.map((attachment) => (attachment.id === id
            ? { ...attachment, mode: attachment.mode === 'source' ? 'verbatim' : 'source' }
            : attachment)));
    }

    function removeAttachment(id: string) {
        setAttachments((current) => current.filter((attachment) => attachment.id !== id));
    }

    return {
        editor,
        fileInputRef,
        isEmpty,
        hasSelection,
        attachments,
        references,
        setText,
        addReferences,
        removeReferences,
        readInput,
        reset,
        markSelectionVerbatim,
        openReferenceMenu,
        openFilePicker,
        addFiles,
        toggleMode,
        removeAttachment,
    };
}
