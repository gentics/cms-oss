import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { type Editor, useEditor } from '@tiptap/react';
import { describe, expect, it } from 'vitest';

import { UiProvider } from '@/components/ui/provider';
import { readParts } from '@/helper/composerParts/composerParts';

import { composerExtensions } from './composerExtensions';
import { ComposerTextInput } from './ComposerTextInput';

import '@/i18n';

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

    it('shows a token as a chip with its label, and its remove button takes it out', async () => {
        const user = userEvent.setup();
        let editor: Editor | undefined;

        render(<Field onEditor={(created) => { editor = created; }} />, { wrapper: UiProvider });
        act(() => {
            editor!.commands.setContent({
                type: 'doc',
                content: [
                    { type: 'text', text: 'Shorten ' },
                    { type: 'reference', attrs: { ref: { type: 'page', id: '8871', node_id: 3, label: 'Product launch 2025' } } },
                ],
            });
        });

        expect(await screen.findByText('Product launch 2025')).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Remove Product launch 2025' }));

        expect(screen.queryByText('Product launch 2025')).not.toBeInTheDocument();
        expect(readParts(editor!.state.doc)).toEqual([{ type: 'text', text: 'Shorten' }]);
    });
});
