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

    it('hides and shows the left column with the buttons in the columns', async () => {
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

    it('has no preview column and no right-column toggle while there is nothing to preview, but the left-column toggle', () => {
        render(<AppShell left={<p>Sessions</p>} center={<p>Chat</p>} right={<p>Preview</p>} isRightColumnVisible={false} />);

        expect(screen.getByText('Sessions')).toBeVisible();
        expect(screen.getByText('Chat')).toBeVisible();
        expect(screen.getByText('Preview')).not.toBeVisible();
        expect(screen.getByRole('button', { name: 'Hide the left column' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /right column/ })).not.toBeInTheDocument();
        expect(screen.getByRole('separator', { name: 'Width of the left column' })).toBeInTheDocument();
    });

    it('hides and shows the right column with the buttons in the columns', async () => {
        const user = userEvent.setup();

        renderShell();

        await user.click(screen.getByRole('button', { name: 'Hide the right column' }));

        expect(screen.getByText('Preview')).not.toBeVisible();
        expect(screen.getByText('Sessions')).toBeVisible();
        expect(screen.getAllByRole('separator')).toHaveLength(1);

        await user.click(screen.getByRole('button', { name: 'Show the right column' }));

        expect(screen.getByText('Preview')).toBeVisible();
        expect(screen.getAllByRole('separator')).toHaveLength(2);
    });

    it('hides both side columns, leaving the chat', async () => {
        const user = userEvent.setup();

        renderShell();

        await user.click(screen.getByRole('button', { name: 'Hide the left column' }));
        await user.click(screen.getByRole('button', { name: 'Hide the right column' }));

        expect(screen.getByText('Chat')).toBeVisible();
        expect(screen.getByText('Sessions')).not.toBeVisible();
        expect(screen.getByText('Preview')).not.toBeVisible();
        expect(screen.getByRole('button', { name: 'Show the left column' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Show the right column' })).toBeInTheDocument();
    });
});
