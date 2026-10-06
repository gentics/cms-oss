import { render, screen } from '@testing-library/react';
import { type Editor, useEditor } from '@tiptap/react';
import { describe, expect, it } from 'vitest';

import { composerExtensions } from './composerExtensions';
import { ComposerTextInput } from './ComposerTextInput';

function Field({ onEditor }: { onEditor: (editor: Editor) => void }) {
    const editor = useEditor({ extensions: composerExtensions });

    onEditor(editor);

    return <ComposerTextInput editor={editor} label="What should happen?" isEmpty size="base" onKeyDown={() => {}} onFocus={() => {}} onBlur={() => {}} />;
}

describe('ComposerTextInput', () => {
    it('is a named, editable multi-line textbox with its label as placeholder', () => {
        let editor: Editor | undefined;

        render(<Field onEditor={(created) => { editor = created; }} />);

        const field = screen.getByRole('textbox', { name: 'What should happen?' });

        expect(field).toBe(editor!.view.dom);
        expect(field).toHaveAttribute('contenteditable', 'true');
        expect(field).toHaveAttribute('aria-multiline', 'true');
        expect(field).toHaveAttribute('data-placeholder', 'What should happen?');
    });
});
