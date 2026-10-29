import { type Editor, EditorContent } from '@tiptap/react';
import { useEffect, useLayoutEffect, useRef } from 'react';

import styles from './ComposerTextInput.module.css';

interface ComposerTextInputProps {
    /** From `useComposer`, created with `composerExtensions` (`./composerExtensions`). */
    editor: Editor;
    /** The accessible name, also the placeholder while the field is empty. */
    label: string;
    isEmpty: boolean;
    /** `lg` on the dashboard (design.md §5.1 `--font-size-lg`), `base` in the chat. */
    size: 'lg' | 'base';
    /** Key presses in the field; one that is `preventDefault()`ed is not handled by the editor. */
    onKeyDown: (event: KeyboardEvent) => void;
    onFocus: () => void;
    onBlur: () => void;
}

/**
 * The text field of a composer: a Tiptap editor, so verbatim passages can sit inline
 * (`composerParts`). Its content is read and changed through `useComposer`, not through React.
 */
export function ComposerTextInput({ editor, label, isEmpty, size, onKeyDown, onFocus, onBlur }: ComposerTextInputProps) {
    const onKeyDownRef = useRef(onKeyDown);
    const className = [styles.field, styles[size], isEmpty ? styles.empty : ''].filter(Boolean).join(' ');

    useEffect(() => {
        onKeyDownRef.current = onKeyDown;
    });

    // The editor's own element is the textbox; before paint, so it is never shown unnamed.
    useLayoutEffect(() => {
        editor.setOptions({
            editorProps: {
                attributes: {
                    'role': 'textbox',
                    'aria-multiline': 'true',
                    'aria-label': label,
                    'data-placeholder': label,
                    'class': className,
                },
                handleKeyDown: (_view, event) => {
                    onKeyDownRef.current(event);

                    return event.defaultPrevented;
                },
            },
        });
    }, [editor, label, className]);

    // Focus and blur bubble up from the editor's element, as React focus events do.
    return <EditorContent editor={editor} className={styles.wrapper} onFocus={onFocus} onBlur={onBlur} />;
}
