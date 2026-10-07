import { ArrowUpIcon, KeyboardIcon, PaperclipIcon, QuoteIcon } from 'lucide-react';
import { type DragEvent, type Ref, useImperativeHandle, useState } from 'react';
import { flushSync } from 'react-dom';
import { useTranslation } from 'react-i18next';

import { AttachmentList } from '@/components/AttachmentList/AttachmentList';
import { ComposerTextInput } from '@/components/ComposerTextInput/ComposerTextInput';
import { IconButton } from '@/components/IconButton/IconButton';
import { Button } from '@/components/ui/button';
import { VoiceInput } from '@/components/VoiceInput/VoiceInput';
import { UPLOAD_ACCEPT } from '@/helper/uploadLimits/uploadLimits';
import { useComposer } from '@/hooks/useComposer';
import type { StartSessionInput } from '@/hooks/useGenaixQueries';
import { useSpeechDictation } from '@/hooks/useSpeechDictation';

import styles from './Composer.module.css';

export interface ComposerHandle {
    /** Replaces the field's content with `text` and opens the text prompt. */
    setText: (text: string) => void;
    /** Empties the field and drops the attachments, after a message was sent. */
    reset: () => void;
}

interface ComposerProps {
    /** `start` the dashboard's prompt without a box, `chat` the box floating over a session's chat. */
    variant: 'start' | 'chat';
    onSubmit: (input: StartSessionInput) => void;
    /** While the input is being sent: the send button is disabled and nothing is submitted twice. */
    isSubmitting: boolean;
    /**
     * While sending: the share uploaded of each submitted file, 0 to 1, by its position in the
     * submitted `files` (`StartSessionInput.onFileProgress`); a file without one is still waiting.
     */
    uploadProgress?: number[];
    ref?: Ref<ComposerHandle>;
}

// The texts that differ between the dashboard and the chat.
const TEXT_KEYS = {
    start: { placeholder: 'dashboard.placeholder', send: 'dashboard.send' },
    chat: { placeholder: 'chat.placeholder', send: 'chat.send' },
} as const;

/**
 * The prompt of the dashboard and of a session's chat: a field that takes typed or dictated text
 * with passages locked as verbatim, files attached as a source or verbatim, and the send button.
 * Enter or the send button submits the message parts and the files; a dictation only fills the
 * field. On phones it puts voice first. Sending is the caller's.
 */
export function Composer({ variant, onSubmit, isSubmitting, uploadProgress = [], ref }: ComposerProps) {
    const { t } = useTranslation();
    const {
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
    } = useComposer();
    const [hasFocus, setHasFocus] = useState(false);
    // While files are dragged over the composer, it looks as when it has the focus.
    const [isDraggingFiles, setIsDraggingFiles] = useState(false);
    // Mobile: the text prompt stays collapsed behind the big mic until the user asks for it.
    const [isTyping, setIsTyping] = useState(false);
    const texts = TEXT_KEYS[variant];
    const isChat = variant === 'chat';

    function textInput() {
        return editor.view.dom;
    }

    // Opens the collapsed text prompt (mobile) synchronously, so the field can take the focus.
    function openTyping() {
        flushSync(() => setIsTyping(true));
        textInput().focus();
    }

    useImperativeHandle(ref, () => ({
        setText: (text) => {
            setText(text);
            openTyping();
        },
        reset,
    }));

    // One recognizer for the inline and the big mic; the words go into the field, nothing is sent.
    const dictation = useSpeechDictation({ onText: setText, onEnd: () => textInput().focus() });

    function cancelDictation() {
        setText('');
        textInput().focus();
    }

    function submit() {
        const input = readInput();

        if (isSubmitting || !input) {
            return;
        }

        onSubmit(input);
    }

    // Enter sends; while dictating, `VoiceInput` takes Enter and Esc before the field sees them.
    function handleKeyDown(event: KeyboardEvent) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            submit();
        }
    }

    const canSubmit = !isSubmitting && (!isEmpty || attachments.length > 0);
    // Mobile, voice first: until the user types or dictates, only the big mic shows.
    const isVoiceFirst = dictation.isSupported && !isTyping && !dictation.isListening;
    // The submitted files are the attachments in their order: while sending they cannot be removed,
    // and new ones only come after them, so the position still names the same attachment.
    const progress = Object.fromEntries(attachments.flatMap(({ id }, index) => {
        const share = uploadProgress[index];

        return share === undefined ? [] : [[id, share]];
    }));

    // Voice input and send: on the dashboard at the end of the field's line, in the chat in the
    // bottom right corner of the box, after the actions (draft `.comrow`).
    const submitButtons = (
        <>
            <span className={styles.inlineVoice}>
                <VoiceInput dictation={dictation} onCancel={cancelDictation} size={isChat ? 'sm' : 'md'} />
            </span>

            <IconButton
                variant="primary"
                size={isChat ? 'icon' : 'icon-lg'}
                label={t(texts.send)}
                disabled={!canSubmit}
                onClick={submit}
            >
                <ArrowUpIcon size={isChat ? 16 : undefined} />
            </IconButton>
        </>
    );

    const actions = (
        <div className={styles.actions}>
            <Button variant="ghost" size="sm" onClick={openFilePicker}>
                <PaperclipIcon size={16} />
                {t('composer.file')}
            </Button>
            <input
                ref={fileInputRef}
                className={styles.fileInput}
                type="file"
                multiple
                accept={UPLOAD_ACCEPT}
                tabIndex={-1}
                aria-label={t('composer.file')}
                onChange={(event) => addFiles(event.target.files)}
            />
            <Button
                variant="ghost"
                size="sm"
                aria-pressed={hasSelection}
                // Keeps the selection in the field, which a click would otherwise clear.
                onMouseDown={(event) => event.preventDefault()}
                onClick={markSelectionVerbatim}
            >
                <QuoteIcon size={16} />
                {t('composer.verbatim')}
            </Button>
            {isChat && <span className={styles.submit}>{submitButtons}</span>}
        </div>
    );

    // Only files are dropped here; dragged text is left to the field.
    function dragsFiles(event: DragEvent) {
        return Array.from(event.dataTransfer.types).includes('Files');
    }

    return (
        <div
            className={[styles.composer, isChat ? styles.chat : '', isVoiceFirst ? styles.voiceFirst : ''].filter(Boolean).join(' ')}
            onDragOver={(event) => {
                if (dragsFiles(event)) {
                    event.preventDefault();
                    event.dataTransfer.dropEffect = 'copy';
                    setIsDraggingFiles(true);
                }
            }}
            onDragLeave={(event) => {
                // Moving onto a child is not leaving.
                if (!event.currentTarget.contains(event.relatedTarget as Node | null)) {
                    setIsDraggingFiles(false);
                }
            }}
            onDrop={(event) => {
                if (dragsFiles(event)) {
                    event.preventDefault();
                    setIsDraggingFiles(false);
                    addFiles(event.dataTransfer.files);
                }
            }}
        >
            {/* The main input on phones (≤ 640 px, see the module); hidden on wider screens. */}
            {dictation.isSupported && (
                <div className={styles.voiceStart}>
                    <VoiceInput dictation={dictation} onCancel={cancelDictation} size="xl" />
                    {isVoiceFirst && (
                        <Button variant="ghost" size="sm" onClick={openTyping}>
                            <KeyboardIcon size={16} />
                            {t('composer.typeInstead')}
                        </Button>
                    )}
                </div>
            )}

            <div
                className={[
                    styles.prompt,
                    hasFocus || isDraggingFiles ? styles.focus : '',
                    dictation.isListening ? styles.listening : '',
                ].filter(Boolean).join(' ')}
            >
                <div className={styles.attachments}>
                    <AttachmentList
                        attachments={attachments}
                        onToggleMode={toggleMode}
                        onRemove={removeAttachment}
                        isSending={isSubmitting}
                        progress={progress}
                    />
                </div>

                <div className={styles.line}>
                    <ComposerTextInput
                        editor={editor}
                        label={t(texts.placeholder)}
                        isEmpty={isEmpty}
                        size={isChat ? 'base' : 'lg'}
                        onKeyDown={handleKeyDown}
                        onFocus={() => setHasFocus(true)}
                        onBlur={() => setHasFocus(false)}
                    />

                    {!isChat && submitButtons}
                </div>

                {/* In the chat the actions sit inside the box; on the dashboard below the prompt. */}
                {isChat && actions}
            </div>

            {!isChat && actions}
        </div>
    );
}
