import { ArrowUpIcon, MessagesSquareIcon, SparklesIcon, XIcon } from 'lucide-react';
import { type KeyboardEvent, type RefObject, useId, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { ReferenceToken } from '@/components/ReferenceToken/ReferenceToken';
import { Button } from '@/components/ui/button';
import { CardLayoutBody, CardLayoutFooter, CardLayoutHeader } from '@/components/ui/card-layout';
import { Label } from '@/components/ui/label';
import { Popover, PopoverClose, PopoverContent, PopoverTitle } from '@/components/ui/popover';
import { Textarea } from '@/components/ui/textarea';

export interface FeedbackCardProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    /** Where the card sits, e.g. the outline of the selected part. */
    anchor: RefObject<HTMLElement | null>;
    /** The card's title, e.g. "Text". */
    title: string;
    /** The part as a reference token, e.g. "Text · content_2". */
    reference: string;
    /** Sending is not possible from outside (e.g. while something is being sent). */
    disabled: boolean;
    /** Send (button or Enter) with the typed text, trimmed; the caller sends it and closes the card. */
    onSend: (text: string) => void;
    /** "In the chat" with the typed text, trimmed; the caller moves part and text into the chat. */
    onToChat: (text: string) => void;
}

/**
 * The feedback card (final_draft.html "FEEDBACK CARD AT THE ELEMENT"): a field for what should happen to the part,
 * at `anchor`; not modal, like the preview's other cards. It holds the typed text, empty each time it opens; Enter
 * sends, Shift+Enter starts a new line (final_draft.html "spotIn"). Sending, moving into the chat and closing are the
 * caller's.
 */
export function FeedbackCard({ open, onOpenChange, anchor, title, reference, disabled, onSend, onToChat }: FeedbackCardProps) {
    const { t } = useTranslation();
    const fieldId = useId();
    const fieldRef = useRef<HTMLTextAreaElement>(null);
    const [text, setText] = useState('');
    const [wasOpen, setWasOpen] = useState(open);

    // Opened again: an empty field. Adjusted while rendering rather than in an effect.
    if (open !== wasOpen) {
        setWasOpen(open);

        if (open) {
            setText('');
        }
    }

    const canSend = !disabled && text.trim() !== '';

    function send() {
        if (canSend) {
            onSend(text.trim());
        }
    }

    function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            send();
        }
    }

    return (
        <Popover open={open} onOpenChange={onOpenChange} modal={false}>
            <PopoverContent anchor={anchor} initialFocus={fieldRef} finalFocus={false}>
                <CardLayoutHeader>
                    <SparklesIcon size={16} aria-hidden="true" />
                    <PopoverTitle>{title}</PopoverTitle>
                    <PopoverClose render={<Button variant="ghost" size="icon" aria-label={t('preview.feedback.cancel')} title={t('preview.feedback.cancel')} />}>
                        <XIcon size={16} />
                    </PopoverClose>
                </CardLayoutHeader>
                <CardLayoutBody>
                    <ReferenceToken>{reference}</ReferenceToken>
                    <Label htmlFor={fieldId}>{t('preview.feedback.label')}</Label>
                    <Textarea id={fieldId} ref={fieldRef} value={text} onChange={(event) => setText(event.target.value)} onKeyDown={handleKeyDown} />
                </CardLayoutBody>
                <CardLayoutFooter>
                    <Button variant="secondary" size="sm" onClick={() => onToChat(text.trim())}>
                        <MessagesSquareIcon size={14} />
                        {t('preview.feedback.toChat')}
                    </Button>
                    <Button variant="primary" size="sm" disabled={!canSend} onClick={send}>
                        <ArrowUpIcon size={14} />
                        {t('preview.feedback.send')}
                    </Button>
                </CardLayoutFooter>
            </PopoverContent>
        </Popover>
    );
}
