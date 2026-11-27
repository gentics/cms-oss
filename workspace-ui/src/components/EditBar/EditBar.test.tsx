import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { UiProvider } from '@/components/ui/provider';

import { EditBar } from './EditBar';

import '@/i18n';

const props = { label: 'Paragraph · Text', editor: null, count: 19, refusal: null, canApply: true, onCancel: vi.fn(), onApply: vi.fn() };

describe('EditBar', () => {
    it('sits above the box, counts the characters, and cancels or applies', async () => {
        const user = userEvent.setup();
        const onCancel = vi.fn();
        const onApply = vi.fn();

        render(<EditBar {...props} box={{ top: 200, left: 30, width: 300, height: 20 }} onCancel={onCancel} onApply={onApply} />, { wrapper: UiProvider });

        const bar = screen.getByRole('toolbar', { name: 'Paragraph · Text' });

        expect(bar.style.top).toBe('200px');
        expect(screen.getByText('19 characters')).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Cancel' }));
        await user.click(screen.getByRole('button', { name: 'Apply' }));

        expect(onCancel).toHaveBeenCalledTimes(1);
        expect(onApply).toHaveBeenCalledTimes(1);
    });

    it('sits below a box at the page\'s top edge, names a refusal, and cannot apply when not possible', () => {
        render(<EditBar {...props} box={{ top: 10, left: 30, width: 300, height: 20 }} refusal="unclearPart" canApply={false} />, { wrapper: UiProvider });

        expect(screen.getByRole('toolbar').style.top).toBe('30px');
        expect(screen.getByRole('alert')).toHaveTextContent('It is not clear which of its parts to write.');
        expect(screen.getByRole('button', { name: 'Apply' })).toBeDisabled();
    });
});
