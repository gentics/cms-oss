import { useId } from 'react';
import { useTranslation } from 'react-i18next';

import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';

import styles from './GuidelineEditor.module.css';

export interface GuidelineEditorProps {
    /** The guideline's file name, e.g. `10_tone-of-voice.default.md`, shown next to the label. */
    fileName: string;
    /** The file's Markdown, as is. */
    value: string;
    onChange: (value: string) => void;
    disabled?: boolean;
}

/**
 * A guideline's Markdown as plain text to edit: one text field, no formatting tools and no preview
 * (ticket: "only a text editor without any styling options"). Controlled; saving is up to the caller.
 */
export function GuidelineEditor({ fileName, value, onChange, disabled }: GuidelineEditorProps) {
    const { t } = useTranslation();
    const id = useId();

    return (
        <div className={styles.editor}>
            <Label htmlFor={id}>
                {t('guidelineEditor.label')}{' '}
                <span className={styles.fileName}>{fileName}</span>
            </Label>
            <Textarea
                id={id}
                rows={20}
                spellCheck
                value={value}
                disabled={disabled}
                onChange={(event) => onChange(event.target.value)}
            />
        </div>
    );
}
