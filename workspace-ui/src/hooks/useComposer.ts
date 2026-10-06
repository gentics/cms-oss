import { LockIcon, XIcon } from 'lucide-react';
import { createElement, type MouseEvent, useEffect, useRef, useState } from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { useToast } from '@/components/ui/use-toast';
import { closestVerbatim, markVerbatim, readParts, selectedRange, unmarkVerbatim } from '@/helper/composerParts/composerParts';
import type { StartFile, StartSessionInput } from '@/hooks/useGenaixQueries';

/** A file attached in a composer, before it is uploaded. */
export interface Attachment extends StartFile {
    id: string;
}

/**
 * The state of the `Composer` (dashboard and session chat): the field with its verbatim passages
 * (`composerParts`), and the attached files. The caller decides what a submit does.
 */
export function useComposer() {
    const { t } = useTranslation();
    const toast = useToast();
    const textInputRef = useRef<HTMLDivElement>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);
    const [isEmpty, setIsEmpty] = useState(true);
    const [hasSelection, setHasSelection] = useState(false);
    const [attachments, setAttachments] = useState<Attachment[]>([]);

    function textInput() {
        return textInputRef.current!;
    }

    function syncEmpty() {
        setIsEmpty(readParts(textInput()).length === 0);
    }

    /** Replaces the field's content with `text`, caret at the end so it can be continued. */
    function setText(text: string) {
        const element = textInput();

        element.textContent = text;
        syncEmpty();

        const caret = document.createRange();

        caret.selectNodeContents(element);
        caret.collapse(false);
        document.getSelection()?.removeAllRanges();
        document.getSelection()?.addRange(caret);
    }

    /** The message parts and files to send, or `null` while there is nothing to send. */
    function readInput(): StartSessionInput | null {
        const parts = readParts(textInput());

        if (parts.length === 0 && attachments.length === 0) {
            return null;
        }

        return { parts, files: attachments.map(({ file, mode }) => ({ file, mode })) };
    }

    /** Empties the field and drops the attachments, after a message was sent. */
    function reset() {
        textInput().replaceChildren();
        setIsEmpty(true);
        setAttachments([]);
    }

    // The verbatim button is pressed while there is a selection in the field it would lock.
    useEffect(() => {
        function onSelectionChange() {
            setHasSelection(textInputRef.current !== null && selectedRange(textInputRef.current) !== null);
        }

        document.addEventListener('selectionchange', onSelectionChange);

        return () => document.removeEventListener('selectionchange', onSelectionChange);
    }, []);

    /** Locks the selected text as verbatim; without a selection, or inside a passage, says why not. */
    function markSelectionVerbatim() {
        const range = selectedRange(textInput());

        textInput().focus();

        if (!range) {
            toast.add({ title: t('composer.verbatimHint') });

            return;
        }

        const removeLabel = t('composer.removeVerbatim');
        // The passage is DOM inside the field, not React: its icon and button are rendered to markup.
        const marked = markVerbatim(range, {
            iconMarkup: renderToStaticMarkup(createElement(LockIcon, { size: 13, 'aria-hidden': true })),
            removeButtonMarkup: renderToStaticMarkup(createElement(
                Button,
                { variant: 'ghost', size: 'icon-xs', 'aria-label': removeLabel, title: removeLabel },
                createElement(XIcon, { size: 12, 'aria-hidden': true }),
            )),
        });

        if (!marked) {
            toast.add({ title: t('composer.verbatimNested') });
        }

        syncEmpty();
    }

    /** The field's click handler: the × of a passage turns it back into text. */
    function handleFieldClick(event: MouseEvent<HTMLDivElement>) {
        const passage = closestVerbatim(event.target);

        if (passage && (event.target as Element).closest('button')) {
            unmarkVerbatim(passage);
            syncEmpty();
        }
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
        textInputRef,
        fileInputRef,
        isEmpty,
        hasSelection,
        attachments,
        syncEmpty,
        setText,
        readInput,
        reset,
        markSelectionVerbatim,
        handleFieldClick,
        openFilePicker,
        addFiles,
        toggleMode,
        removeAttachment,
    };
}
