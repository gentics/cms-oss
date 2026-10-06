import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { UiProvider } from '@/components/ui/provider';

import { AttachmentList } from './AttachmentList';

import '@/i18n';

const attachments = [
    { id: 'a', file: new File(['a'], 'brief.pdf'), mode: 'source' as const },
    { id: 'b', file: new File(['b'], 'terms.txt'), mode: 'verbatim' as const },
];

describe('AttachmentList', () => {
    it('renders nothing without attachments', () => {
        const { container } = render(<AttachmentList attachments={[]} onToggleMode={vi.fn()} onRemove={vi.fn()} />, { wrapper: UiProvider });

        expect(container).toBeEmptyDOMElement();
    });

    it('shows each file with its mode and reports swap and remove by id', async () => {
        const user = userEvent.setup();
        const onToggleMode = vi.fn();
        const onRemove = vi.fn();

        render(<AttachmentList attachments={attachments} onToggleMode={onToggleMode} onRemove={onRemove} />, { wrapper: UiProvider });

        expect(screen.getByText('brief.pdf')).toBeInTheDocument();
        expect(screen.getByText('as a source')).toBeInTheDocument();
        expect(screen.getByText('verbatim')).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'verbatim / as a source: brief.pdf' }));
        await user.click(screen.getByRole('button', { name: 'Remove: terms.txt' }));

        expect(onToggleMode).toHaveBeenCalledWith('a');
        expect(onRemove).toHaveBeenCalledWith('b');
    });
});
