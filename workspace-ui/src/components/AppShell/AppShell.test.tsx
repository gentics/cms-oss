import { act, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ComponentProps } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '@/test/renderWithProviders';

import { AppShell } from './AppShell';

import '@/i18n';

class ResizeObserverStub {
    observe() {}

    disconnect() {}
}

// The Topbar's brand is a router link, so the shell renders inside a router, which renders
// asynchronously: wait for the topbar first.
async function renderShell(props: Partial<ComponentProps<typeof AppShell>> = {}) {
    const result = renderWithProviders(
        <AppShell
            left={<p>Sessions</p>}
            center={<p>Chat</p>}
            right={<p>Preview</p>}
            {...props}
        />,
    );

    await screen.findByRole('banner');

    return result;
}

describe('AppShell', () => {
    beforeEach(() => {
        vi.stubGlobal('ResizeObserver', ResizeObserverStub);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('renders the topbar and the content of all three columns with both splitters', async () => {
        await renderShell();

        expect(screen.getByRole('banner')).toHaveTextContent('Gentics Workspace');
        expect(screen.getByText('Sessions')).toBeVisible();
        expect(screen.getByText('Chat')).toBeVisible();
        expect(screen.getByText('Preview')).toBeVisible();
        expect(screen.getAllByRole('separator')).toHaveLength(2);
    });

    it('hides and shows the left column with the button in the topbar', async () => {
        const user = userEvent.setup();

        await renderShell();

        await user.click(within(screen.getByRole('banner')).getByRole('button', { name: 'Hide the left column' }));

        expect(screen.getByText('Sessions')).not.toBeVisible();
        expect(screen.getByText('Chat')).toBeVisible();
        expect(screen.getAllByRole('separator')).toHaveLength(1);
        expect(screen.getByRole('separator', { name: 'Width of the right column' })).toBeInTheDocument();

        await user.click(within(screen.getByRole('banner')).getByRole('button', { name: 'Show the left column' }));

        expect(screen.getByText('Sessions')).toBeVisible();
        expect(screen.getAllByRole('separator')).toHaveLength(2);
    });

    it('has no preview column and no right-column toggle while there is nothing to preview, but the left-column toggle', async () => {
        await renderShell({ isRightColumnVisible: false });

        expect(screen.getByText('Sessions')).toBeVisible();
        expect(screen.getByText('Chat')).toBeVisible();
        expect(screen.getByText('Preview')).not.toBeVisible();
        expect(screen.getByRole('button', { name: 'Hide the left column' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /right column/ })).not.toBeInTheDocument();
        expect(screen.getByRole('separator', { name: 'Width of the left column' })).toBeInTheDocument();
    });

    it('hides and shows the right column with the button in the topbar', async () => {
        const user = userEvent.setup();

        await renderShell();

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

        await renderShell();

        await user.click(screen.getByRole('button', { name: 'Hide the left column' }));
        await user.click(screen.getByRole('button', { name: 'Hide the right column' }));

        expect(screen.getByText('Chat')).toBeVisible();
        expect(screen.getByText('Sessions')).not.toBeVisible();
        expect(screen.getByText('Preview')).not.toBeVisible();
        expect(screen.getByRole('button', { name: 'Show the left column' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Show the right column' })).toBeInTheDocument();
    });

    // The topbar's are the only buttons for the side columns; the columns have none of their own.
    it('toggles both side columns from the topbar', async () => {
        await renderShell();

        const topbar = screen.getByRole('banner');

        expect(within(topbar).getByRole('button', { name: 'Hide the left column' })).toBeInTheDocument();
        expect(within(topbar).getByRole('button', { name: 'Hide the right column' })).toBeInTheDocument();
        expect(screen.getAllByRole('button', { name: /(left|right) column/ })).toHaveLength(2);
    });

    // jsdom applies no media queries, so these see the state, not the phone layout (one column at a
    // time, e2e/SessionMobile.spec.ts): which side columns are hidden, and the toggles' labels.
    describe('on a phone', () => {
        beforeEach(() => {
            vi.stubGlobal('matchMedia', (query: string) => ({ matches: query === '(max-width: 640px)', media: query }));
        });

        it('starts with both side columns hidden, so the chat comes first', async () => {
            await renderShell();

            expect(screen.getByText('Sessions')).not.toBeVisible();
            expect(screen.getByText('Preview')).not.toBeVisible();
            expect(screen.getByRole('button', { name: 'Show the left column' })).toBeInTheDocument();
            expect(screen.getByRole('button', { name: 'Show the right column' })).toBeInTheDocument();
        });

        it('shows one side column at a time', async () => {
            const user = userEvent.setup();

            await renderShell();

            await user.click(screen.getByRole('button', { name: 'Show the left column' }));

            expect(screen.getByText('Sessions')).toBeVisible();
            expect(screen.getByText('Preview')).not.toBeVisible();

            await user.click(screen.getByRole('button', { name: 'Show the right column' }));

            expect(screen.getByText('Preview')).toBeVisible();
            expect(screen.getByText('Sessions')).not.toBeVisible();
            expect(screen.getByRole('button', { name: 'Show the left column' })).toBeInTheDocument();

            await user.click(screen.getByRole('button', { name: 'Hide the right column' }));

            expect(screen.getByText('Sessions')).not.toBeVisible();
            expect(screen.getByText('Preview')).not.toBeVisible();
        });

        it('hides the side columns on a new page, e.g. a session chosen in the left column', async () => {
            const user = userEvent.setup();
            const { router } = await renderShell();

            await user.click(screen.getByRole('button', { name: 'Show the left column' }));

            expect(screen.getByText('Sessions')).toBeVisible();

            await act(() => router.navigate({ to: '/sessions/$id', params: { id: 's-1' } }));

            expect(screen.getByText('Sessions')).not.toBeVisible();
            expect(screen.getByRole('button', { name: 'Show the left column' })).toBeInTheDocument();
        });
    });

    it('keeps the side columns on a new page on a wider screen', async () => {
        const user = userEvent.setup();
        const { router } = await renderShell();

        await user.click(screen.getByRole('button', { name: 'Hide the right column' }));
        await act(() => router.navigate({ to: '/sessions/$id', params: { id: 's-1' } }));

        expect(screen.getByText('Sessions')).toBeVisible();
        expect(screen.getByText('Preview')).not.toBeVisible();
    });
});
