import type { HTMLAttributes, Ref } from 'react';

import styles from './ComposerTextInput.module.css';

interface ComposerTextInputProps extends Omit<HTMLAttributes<HTMLDivElement>, 'contentEditable' | 'role'> {
    ref: Ref<HTMLDivElement>;
    /** The accessible name, also the placeholder while the field is empty. */
    label: string;
    isEmpty: boolean;
    /** `lg` on the dashboard (design.md §5.1 `--font-size-lg`), `base` in the chat. */
    size: 'lg' | 'base';
}

/**
 * The text field of a composer: a `contenteditable` element, so verbatim passages can sit inline
 * (`composerParts`). Its content is read and changed through `useComposer`, not through React.
 */
export function ComposerTextInput({ ref, label, isEmpty, size, className, ...props }: ComposerTextInputProps) {
    return (
        <div
            ref={ref}
            className={[styles.field, styles[size], isEmpty ? styles.empty : '', className].filter(Boolean).join(' ')}
            contentEditable
            suppressContentEditableWarning
            role="textbox"
            aria-multiline="true"
            aria-label={label}
            data-placeholder={label}
            {...props}
        />
    );
}
