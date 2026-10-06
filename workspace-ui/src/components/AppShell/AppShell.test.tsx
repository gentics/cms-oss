import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { AppShell } from './AppShell';

import '@/i18n';

class ResizeObserverStub {
    observe() {}

    disconnect() {}
}

function renderShell() {
    return render(
        <AppShell
            left={<p>Sessions</p>}
            center={<p>Chat</p>}
            right={<p>Preview</p>}
        />,
    );
}

describe('AppShell', () => {
    beforeEach(() => {
        vi.stubGlobal('ResizeObserver', ResizeObserverStub);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('renders the topbar and the content of all three columns with both splitters', () => {
        renderShell();

        expect(screen.getByRole('banner')).toHaveTextContent('Gentics Workspace');
        expect(screen.getByText('Sessions')).toBeVisible();
        expect(screen.getByText('Chat')).toBeVisible();
        expect(screen.getByText('Preview')).toBeVisible();
        expect(screen.getAllByRole('separator')).toHaveLength(2);
    });

    it('hides and shows the left column from the topbar', async () => {
        const user = userEvent.setup();

        renderShell();

        await user.click(screen.getByRole('button', { name: 'Hide the left column' }));

        expect(screen.getByText('Sessions')).not.toBeVisible();
        expect(screen.getByText('Chat')).toBeVisible();
        expect(screen.getAllByRole('separator')).toHaveLength(1);
        expect(screen.getByRole('separator', { name: 'Width of the right column' })).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Show the left column' }));

        expect(screen.getByText('Sessions')).toBeVisible();
        expect(screen.getAllByRole('separator')).toHaveLength(2);
    });

    it('has no preview column and no left-column toggle while there is nothing to preview', () => {
        render(<AppShell left={<p>Sessions</p>} center={<p>Chat</p>} right={<p>Preview</p>} isRightColumnVisible={false} />);

        expect(screen.getByText('Sessions')).toBeVisible();
        expect(screen.getByText('Chat')).toBeVisible();
        expect(screen.getByText('Preview')).not.toBeVisible();
        expect(screen.queryByRole('button', { name: /left column/ })).not.toBeInTheDocument();
        expect(screen.getByRole('separator', { name: 'Width of the left column' })).toBeInTheDocument();
    });

    it('shows the left column again when the preview goes away while it was hidden', async () => {
        const user = userEvent.setup();
        const { rerender } = renderShell();

        await user.click(screen.getByRole('button', { name: 'Hide the left column' }));

        rerender(<AppShell left={<p>Sessions</p>} center={<p>Chat</p>} right={<p>Preview</p>} isRightColumnVisible={false} />);

        expect(screen.getByText('Sessions')).toBeVisible();
        expect(screen.queryByRole('button', { name: /left column/ })).not.toBeInTheDocument();
    });
});
