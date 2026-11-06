import { CheckIcon, LinkIcon, XIcon } from 'lucide-react';
import { type RefObject, useId, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { LinkTargetPicker, type LinkTargetValue } from '@/components/LinkTargetPicker/LinkTargetPicker';
import { ReferenceToken } from '@/components/ReferenceToken/ReferenceToken';
import { RefusalNote } from '@/components/RefusalNote/RefusalNote';
import { Button } from '@/components/ui/button';
import { CardLayoutBody, CardLayoutFooter, CardLayoutHeader } from '@/components/ui/card-layout';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Popover, PopoverClose, PopoverContent, PopoverTitle } from '@/components/ui/popover';
import type { LinkTargetChange, RefusalReason } from '@/helper/cmsEditable/cmsEditable';
import { collapseWhiteSpace } from '@/helper/collapseWhiteSpace/collapseWhiteSpace';


/** A link's text and target, as the card starts from them. */
export interface LinkValue {
    text: string;
    target: LinkTargetValue;
}

/** What the card changes: the text if it reads differently, the target if it is another URL or page; empty for none. */
export interface LinkChange {
    text?: string;
    target?: LinkTargetChange;
}

export interface LinkCardProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    /** Where the card sits, e.g. the outline of the selected link. */
    anchor: RefObject<HTMLElement | null>;
    /** The card's title, e.g. "Link · Text". */
    title: string;
    /** The link as a reference token, e.g. "Text · content_2 · Link". */
    reference: string;
    /** The link's text and target when the card opens; a new value starts the card over with it. */
    initial: LinkValue;
    /** The node whose pages can be the target, and the page the link is on (`LinkTargetPicker`). */
    nodeId: number | undefined;
    pageId: number;
    /** Why the last Apply could not be saved, shown in the card; `null` for none. */
    refusal: RefusalReason | null;
    /** Apply is not possible from outside (e.g. while something is being sent). */
    disabled: boolean;
    /** Apply (button or Enter in the text) with what changed; the caller saves it and closes the card. */
    onApply: (change: LinkChange) => void;
}

/**
 * The link card (final_draft.html "askLink"): the link's text and target (a URL or a CMS page,
 * `LinkTargetPicker`), at `anchor`; not modal, like the preview's other cards. It holds the values being edited and
 * tells on Apply what changed against `initial`; saving, closing and the refusal are the caller's.
 */
export function LinkCard({ open, onOpenChange, anchor, title, reference, initial, nodeId, pageId, refusal, disabled, onApply }: LinkCardProps) {
    const { t } = useTranslation();
    const textId = useId();
    const textRef = useRef<HTMLInputElement>(null);
    const [shownInitial, setShownInitial] = useState(initial);
    const [text, setText] = useState(initial.text);
    const [target, setTarget] = useState(initial.target);

    // Another link (or the same one opened again): start over from it. Adjusted while rendering rather than in an effect.
    if (initial !== shownInitial) {
        setShownInitial(initial);
        setText(initial.text);
        setTarget(initial.target);
    }

    const canApply = !disabled && text.trim() !== '' && (target.type === 'url' ? target.url.trim() !== '' : target.page !== null);

    function change(): LinkChange {
        const result: LinkChange = {};

        if (collapseWhiteSpace(text) !== collapseWhiteSpace(initial.text)) {
            result.text = text;
        }

        if (target.type === 'url' && (initial.target.type !== 'url' || target.url.trim() !== initial.target.url.trim())) {
            result.target = { type: 'url', url: target.url.trim() };
        } else if (target.type === 'page' && target.page && (initial.target.type !== 'page' || target.page.id !== initial.target.page?.id)) {
            result.target = { type: 'page', pageId: target.page.id, ...(nodeId !== undefined ? { nodeId } : {}) };
        }

        return result;
    }

    function apply() {
        if (canApply) {
            onApply(change());
        }
    }

    return (
        <Popover open={open} onOpenChange={onOpenChange} modal={false}>
            <PopoverContent anchor={anchor} initialFocus={textRef} finalFocus={false}>
                <CardLayoutHeader>
                    <LinkIcon size={16} aria-hidden="true" />
                    <PopoverTitle>{title}</PopoverTitle>
                    <PopoverClose render={<Button variant="ghost" size="icon" aria-label={t('preview.link.cancel')} title={t('preview.link.cancel')} />}>
                        <XIcon size={16} />
                    </PopoverClose>
                </CardLayoutHeader>
                <CardLayoutBody>
                    <ReferenceToken>{reference}</ReferenceToken>
                    <Label htmlFor={textId}>{t('preview.link.text')}</Label>
                    <Input
                        id={textId}
                        ref={textRef}
                        value={text}
                        onChange={(event) => setText(event.target.value)}
                        onKeyDown={(event) => {
                            if (event.key === 'Enter') {
                                event.preventDefault();
                                apply();
                            }
                        }}
                    />
                    <LinkTargetPicker nodeId={nodeId} pageId={pageId} value={target} onValueChange={setTarget} />
                    {refusal && <RefusalNote reason={refusal} />}
                </CardLayoutBody>
                <CardLayoutFooter>
                    <Button variant="primary" size="sm" disabled={!canApply} onClick={apply}>
                        <CheckIcon size={14} />
                        {t('preview.link.apply')}
                    </Button>
                </CardLayoutFooter>
            </PopoverContent>
        </Popover>
    );
}
