import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';

import { UiProvider } from '@/components/ui/provider';

import { GuidelineEditor } from './GuidelineEditor';

import '@/i18n';

const MARKDOWN = '# Tone of voice\n\n- Address readers as "you".\n';

function Editable({ onChange }: { onChange: (value: string) => void }) {
    const [value, setValue] = useState(MARKDOWN);

    return (
        <GuidelineEditor
            fileName="10_tone-of-voice.default.md"
            value={value}
            onChange={(next) => {
                setValue(next);
                onChange(next);
            }}
        />
    );
}

describe('GuidelineEditor', () => {
    it('shows the Markdown as plain text in a field labelled with the file name', () => {
        render(<GuidelineEditor fileName="10_tone-of-voice.default.md" value={MARKDOWN} onChange={vi.fn()} />, { wrapper: UiProvider });

        const field = screen.getByRole('textbox', { name: 'Guideline 10_tone-of-voice.default.md' });

        expect(field).toHaveValue(MARKDOWN);
        // No formatting tools: the field is the only control.
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('reports every edit as the whole new text', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();

        render(<Editable onChange={onChange} />, { wrapper: UiProvider });

        await user.type(screen.getByRole('textbox'), '- Short sentences.');

        expect(onChange).toHaveBeenLastCalledWith(`${MARKDOWN}- Short sentences.`);
        expect(screen.getByRole('textbox')).toHaveValue(`${MARKDOWN}- Short sentences.`);
    });

    it('cannot be edited while disabled', () => {
        render(<GuidelineEditor fileName="10_tone-of-voice.default.md" value={MARKDOWN} onChange={vi.fn()} disabled />, { wrapper: UiProvider });

        expect(screen.getByRole('textbox')).toBeDisabled();
    });
});
