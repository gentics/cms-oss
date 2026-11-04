import type { Editor } from '@tiptap/core';
import { CheckIcon, XIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { InlineEditorToolbar } from '@/components/InlineEditor/InlineEditorToolbar';
import { RefusalNote } from '@/components/RefusalNote/RefusalNote';
import { Button } from '@/components/ui/button';
import type { RefusalReason } from '@/helper/cmsEditable/cmsEditable';
import type { ElementBox } from '@/hooks/usePreviewElements';

import styles from './EditBar.module.css';

// How much room the bar needs above the box (its height and gap); with less it sits below.
const EDIT_BAR_ROOM = 44;

export interface EditBarProps {
    /** The box the bar sits above (or below, at the page's top edge), e.g. the edited element. */
    box: ElementBox;
    /** The bar's name, e.g. "Paragraph · Text". */
    label: string;
    /** The inline editor, for its formatting buttons; `null` for plain text. */
    editor: Editor | null;
    /** The number of characters written. */
    count: number;
    /** Why the last Apply could not be saved; `null` for none. */
    refusal: RefusalReason | null;
    canApply: boolean;
    onCancel: () => void;
    onApply: () => void;
}

/**
 * The bar of the editing in place (final_draft.html "fmt"): above the edited element, below it at the page's top edge,
 * with the formatting buttons, the character count, why a save was refused, Cancel and Apply.
 */
export function EditBar({ box, label, editor, count, refusal, canApply, onCancel, onApply }: EditBarProps) {
    const { t } = useTranslation();
    const isBelow = box.top < EDIT_BAR_ROOM;

    return (
        <div
            className={`${styles.editBar} ${isBelow ? styles.editBarBelow : ''}`}
            style={{ top: isBelow ? box.top + box.height : box.top, left: box.left }}
            role="toolbar"
            aria-label={label}
        >
            {editor && <InlineEditorToolbar editor={editor} />}
            <span className={styles.editCount}>{t('preview.edit.characters', { count })}</span>
            {refusal && <RefusalNote reason={refusal} />}
            <Button variant="ghost" size="sm" onClick={onCancel}>
                <XIcon size={14} />
                {t('preview.edit.cancel')}
            </Button>
            <Button variant="primary" size="sm" disabled={!canApply} onClick={onApply}>
                <CheckIcon size={14} />
                {t('preview.edit.apply')}
            </Button>
        </div>
    );
}
