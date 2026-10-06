import { TextSelection } from '@tiptap/pm/state';
import { useEditor } from '@tiptap/react';
import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { composerExtensions } from '@/components/ComposerTextInput/composerExtensions';
import { useToast } from '@/components/ui/use-toast';
import { readParts, selectedRange, textContent, touchesVerbatim, VERBATIM_NODE } from '@/helper/composerParts/composerParts';
import type { StartFile, StartSessionInput } from '@/hooks/useGenaixQueries';

/** A file attached in a composer, before it is uploaded. */
export interface Attachment extends StartFile {
    id: string;
}

/**
 * The state of the `Composer` (dashboard and session chat): the field, a Tiptap editor with its
 * verbatim passages (`composerParts`), and the attached files. The caller decides what a submit does.
 */
export function useComposer() {
    const { t } = useTranslation();
    const toast = useToast();
    const editor = useEditor({ extensions: composerExtensions });
    const fileInputRef = useRef<HTMLInputElement>(null);
    const [isEmpty, setIsEmpty] = useState(true);
    const [hasSelection, setHasSelection] = useState(false);
    const [attachments, setAttachments] = useState<Attachment[]>([]);

    // Typing, dictation, a passage locked or unlocked: every change of the text.
    useEffect(() => {
        function onUpdate() {
            setIsEmpty(readParts(editor.state.doc).length === 0);
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

    /** The message parts and files to send, or `null` while there is nothing to send. */
    function readInput(): StartSessionInput | null {
        const parts = readParts(editor.state.doc);

        if (parts.length === 0 && attachments.length === 0) {
            return null;
        }

        return { parts, files: attachments.map(({ file, mode }) => ({ file, mode })) };
    }

    /** Empties the field and drops the attachments, after a message was sent. */
    function reset() {
        editor.commands.clearContent();
        setIsEmpty(true);
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

        // The selection as the user sees it, in document positions.
        const from = view.posAtDOM(range.startContainer, range.startOffset);
        const to = view.posAtDOM(range.endContainer, range.endOffset);
        // The caret needs text behind the passage to land in.
        const tail = editor.state.doc.resolve(to).nodeAfter?.isText ? [] : [{ type: 'text', text: ' ' }];

        editor.chain().insertContentAt({ from, to }, [{ type: VERBATIM_NODE, attrs: { text: range.toString() } }, ...tail]).run();
    }

    /** Opens the browser's file picker (the hidden `<input type="file">` behind `fileInputRef`). */
    function openFilePicker() {
        fileInputRef.current!.click();
    }

    /** The file input's change handler: new files are attached as a source. */
    function addFiles(files: FileList | null) {
        const added = Array.from(files ?? [], (file): Attachment => ({ id: crypto.randomUUID(), file, mode: 'source' }));

        setAttachments((current) => [...current, ...added]);
        // The same file can be picked again after it was removed.
        fileInputRef.current!.value = '';
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
        setText,
        readInput,
        reset,
        markSelectionVerbatim,
        openFilePicker,
        addFiles,
        toggleMode,
        removeAttachment,
    };
}
