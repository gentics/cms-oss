import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createRef } from 'react';
import { describe, expect, it, vi } from 'vitest';

import { UiProvider } from '@/components/ui/provider';

import { PreviewHeader, type PreviewHeaderProps } from './PreviewHeader';

import '@/i18n';

const props: PreviewHeaderProps = {
    titleId: 'title',
    url: 'https://cms.example.com/alohapage?realid=9142',
    pages: null,
    selection: null,
    settings: null,
    settingsButtonRef: createRef(),
    isMobile: false,
    onMobileChange: vi.fn(),
    isFullScreen: false,
    onFullScreenChange: vi.fn(),
};

describe('PreviewHeader', () => {
    it('shows the title, opens the page in a new tab, and switches width and full screen', async () => {
        const user = userEvent.setup();
        const onMobileChange = vi.fn();
        const onFullScreenChange = vi.fn();

        render(<PreviewHeader {...props} onMobileChange={onMobileChange} onFullScreenChange={onFullScreenChange} />, { wrapper: UiProvider });

        expect(screen.getByRole('heading', { name: 'Preview' })).toHaveAttribute('id', 'title');
        expect(screen.getByRole('link', { name: 'Open' })).toHaveAttribute('target', '_blank');
        expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Page settings' })).not.toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Mobile · 390 px' }));
        await user.click(screen.getByRole('button', { name: 'Full screen' }));

        expect(onMobileChange).toHaveBeenCalledWith(true);
        expect(onFullScreenChange).toHaveBeenCalledWith(true);
    });

    it('names the selection with a button to clear it, and toggles the page settings', async () => {
        const user = userEvent.setup();
        const onClear = vi.fn();
        const onToggle = vi.fn();

        render(<PreviewHeader {...props} selection={{ name: 'Paragraph · Text', onClear }} settings={{ open: true, onToggle }} />, { wrapper: UiProvider });

        expect(screen.getByText('Selected: Paragraph · Text')).toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: 'Clear selection' }));
        expect(onClear).toHaveBeenCalledTimes(1);

        const settings = screen.getByRole('button', { name: 'Page settings' });

        expect(settings).toHaveAttribute('aria-expanded', 'true');
        await user.click(settings);
        expect(onToggle).toHaveBeenCalledTimes(1);
    });

    it('offers the pages to choose from when there are several', () => {
        render(<PreviewHeader {...props} pages={{ names: { a: 'Home', b: 'News' }, current: 'b', onPick: vi.fn() }} />, { wrapper: UiProvider });

        expect(screen.getByRole('combobox', { name: 'Pages' })).toHaveTextContent('News');
    });
});
