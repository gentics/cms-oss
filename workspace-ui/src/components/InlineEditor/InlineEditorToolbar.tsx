import type { Editor } from '@tiptap/core';
import { useEditorState } from '@tiptap/react';
import {
    BoldIcon,
    CheckIcon,
    ItalicIcon,
    LinkIcon,
    type LucideIcon,
    StrikethroughIcon,
    SubscriptIcon,
    SuperscriptIcon,
    UnderlineIcon,
    UnlinkIcon,
} from 'lucide-react';
import { useId, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { CardLayoutBody, CardLayoutFooter, CardLayoutHeader } from '@/components/ui/card-layout';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Popover, PopoverContent, PopoverTitle, PopoverTrigger } from '@/components/ui/popover';

import { hrefOf, type LinkAttributes, type MarkName, withHref } from './inlineEditorSchema';
import { focusEditor } from './useInlineEditor';

import styles from './InlineEditorToolbar.module.css';

const MARK_BUTTONS: { mark: MarkName; Icon: LucideIcon }[] = [
    { mark: 'bold', Icon: BoldIcon },
    { mark: 'italic', Icon: ItalicIcon },
    { mark: 'underline', Icon: UnderlineIcon },
    { mark: 'strike', Icon: StrikethroughIcon },
    { mark: 'subscript', Icon: SubscriptIcon },
    { mark: 'superscript', Icon: SuperscriptIcon },
];

/**
 * The formatting buttons of the inline editor (`useInlineEditor`): one per mark, pressed while the selection has
 * it, and Link, which opens a card to set, change or remove the link's URL. For an editor with marks only; the
 * caller places it, e.g. in a bar next to its own Cancel and Apply.
 */
export function InlineEditorToolbar({ editor }: { editor: Editor }) {
    const { t } = useTranslation();
    const urlId = useId();
    const [isLinkOpen, setIsLinkOpen] = useState(false);
    const [url, setUrl] = useState('');
    // Nothing is active once the editor is gone (it may be destroyed before this toolbar is removed).
    const state = useEditorState({
        editor,
        selector: ({ editor: current }) => ({
            active: Object.fromEntries(MARK_BUTTONS.map(({ mark }) => [mark, !current.isDestroyed && current.isActive(mark)])) as Record<MarkName, boolean>,
            isLink: !current.isDestroyed && current.isActive('link'),
            hasSelection: !current.isDestroyed && !current.state.selection.empty,
        }),
    });
    // A link is set on selected text, or changed where the caret is in one.
    const canLink = state.isLink || state.hasSelection;
    const canApplyLink = canLink && url.trim() !== '';

    function toggle(mark: MarkName) {
        editor.chain().toggleMark(mark).run();
        focusEditor(editor);
    }

    function openLink(open: boolean) {
        if (open) {
            setUrl(hrefOf((editor.getAttributes('link').attributes ?? []) as LinkAttributes));
        }

        setIsLinkOpen(open);
    }

    function applyLink() {
        if (!canApplyLink) {
            return;
        }

        const href = url.trim();

        if (state.isLink) {
            const attributes = (editor.getAttributes('link').attributes ?? []) as LinkAttributes;

            editor.chain().extendMarkRange('link').setMark('link', { attributes: withHref(attributes, href) }).run();
        } else {
            editor.chain().setMark('link', { attributes: [['href', href]] }).run();
        }

        setIsLinkOpen(false);
        focusEditor(editor);
    }

    function removeLink() {
        editor.chain().extendMarkRange('link').unsetMark('link').run();
        setIsLinkOpen(false);
        focusEditor(editor);
    }

    return (
        <div className={styles.toolbar} role="group" aria-label={t('inlineEditor.formatting')}>
            {MARK_BUTTONS.map(({ mark, Icon }) => (
                <Button
                    key={mark}
                    variant="ghost"
                    size="icon"
                    aria-label={t(`inlineEditor.${mark}`)}
                    title={t(`inlineEditor.${mark}`)}
                    aria-pressed={state.active[mark]}
                    onClick={() => toggle(mark)}
                >
                    <Icon size={16} />
                </Button>
            ))}
            <Popover open={isLinkOpen} onOpenChange={openLink} modal={false}>
                <PopoverTrigger
                    render={(
                        <Button
                            variant="ghost"
                            size="icon"
                            aria-label={t('inlineEditor.link')}
                            title={t('inlineEditor.link')}
                            aria-pressed={state.isLink}
                            disabled={!canLink}
                        />
                    )}
                >
                    <LinkIcon size={16} />
                </PopoverTrigger>
                <PopoverContent finalFocus={false}>
                    <CardLayoutHeader>
                        <LinkIcon size={16} aria-hidden="true" />
                        <PopoverTitle>{t('inlineEditor.link')}</PopoverTitle>
                    </CardLayoutHeader>
                    <CardLayoutBody>
                        <Label htmlFor={urlId}>{t('inlineEditor.url')}</Label>
                        <Input
                            id={urlId}
                            type="url"
                            value={url}
                            onChange={(event) => setUrl(event.target.value)}
                            onKeyDown={(event) => {
                                if (event.key === 'Enter') {
                                    event.preventDefault();
                                    applyLink();
                                }
                            }}
                        />
                    </CardLayoutBody>
                    <CardLayoutFooter>
                        {state.isLink && (
                            <Button variant="secondary" size="sm" onClick={removeLink}>
                                <UnlinkIcon size={14} />
                                {t('inlineEditor.removeLink')}
                            </Button>
                        )}
                        <Button variant="primary" size="sm" disabled={!canApplyLink} onClick={applyLink}>
                            <CheckIcon size={14} />
                            {t('inlineEditor.applyLink')}
                        </Button>
                    </CardLayoutFooter>
                </PopoverContent>
            </Popover>
        </div>
    );
}
