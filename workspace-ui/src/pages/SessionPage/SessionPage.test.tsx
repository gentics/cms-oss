import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createMemoryHistory, createRouter, RouterProvider } from '@tanstack/react-router';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { UiProvider } from '@/components/ui/provider';
import { routeTree } from '@/router';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

import '@/i18n';

// The page follows the session's event stream (tested in useSessionEvents.test.tsx); here it would
// take the stubbed fetch responses meant for sending.
vi.mock('@/hooks/useSessionEvents', () => ({ useSessionEvents: () => {} }));

// jsdom has no ResizeObserver; the page's AppShell measures its columns with one.
class ResizeObserverStub {
    observe() {}

    disconnect() {}
}

// Stands in for the browser's recognizer: the test plays the results and the end of listening.
class FakeRecognizer {
    static last: FakeRecognizer | undefined;
    lang = '';
    interimResults = false;
    continuous = true;
    onresult: ((event: unknown) => void) | null = null;
    onend: (() => void) | null = null;
    start = vi.fn();
    stop = vi.fn(() => this.onend?.());
    abort = vi.fn(() => this.onend?.());

    constructor() {
        FakeRecognizer.last = this;
    }

    say(transcript: string) {
        this.onresult?.({ results: [[{ transcript }]] });
    }
}

function stubFetch(...responses: Response[]) {
    const fetchMock = vi.fn<typeof fetch>();

    responses.forEach((response) => fetchMock.mockResolvedValueOnce(response));
    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

const accepted = () => Response.json({ message_id: 'm-1', run_id: 'r-1', session_id: 's-1' }, { status: 202 });

async function renderComposer() {
    const router = createRouter({ routeTree, history: createMemoryHistory({ initialEntries: ['/sessions/s-1'] }) });

    render(
        <QueryClientProvider client={new QueryClient()}>
            <UiProvider>
                <RouterProvider router={router} />
            </UiProvider>
        </QueryClientProvider>,
    );

    return screen.findByRole('textbox', { name: 'What should happen?' });
}

describe('SessionPage', () => {
    beforeEach(() => {
        FakeRecognizer.last = undefined;
        vi.stubGlobal('ResizeObserver', ResizeObserverStub);
        useWorkspaceEventStore.setState({ sessions: {} });
        useErrorNotificationStore.setState({ errors: [] });
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('sends the turn on Enter, records it in the store and empties the field', async () => {
        const user = userEvent.setup();
        const fetchMock = stubFetch(accepted());
        const field = await renderComposer();

        await user.click(field);
        await user.keyboard('Shorten the intro{Enter}');

        await waitFor(() => expect(field).toHaveTextContent(''));
        expect(fetchMock.mock.calls[0]![0]).toBe('/genaix/api/v1/sessions/s-1/messages');
        expect(JSON.parse(fetchMock.mock.calls[0]![1]!.body as string)).toEqual({ parts: [{ type: 'text', text: 'Shorten the intro' }] });
        expect(selectSession('s-1')(useWorkspaceEventStore.getState()).messages).toMatchObject([
            { kind: 'sent', messageId: 'm-1', status: 'sent' },
        ]);
    });

    it('disables send while there is nothing to send, and sends with the button', async () => {
        const user = userEvent.setup();
        const fetchMock = stubFetch(accepted());
        const field = await renderComposer();
        const send = screen.getByRole('button', { name: 'Send' });

        expect(send).toBeDisabled();

        await user.click(field);
        await user.keyboard('Hello');
        await user.click(send);

        await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    });

    it('uploads an attached file before the message and drops the chip after sending', async () => {
        const user = userEvent.setup();
        const fetchMock = stubFetch(Response.json({ id: 'f-1' }, { status: 201 }), accepted());

        await renderComposer();

        await user.upload(screen.getByLabelText('File', { selector: 'input' }), new File(['a'], 'brief.pdf'));

        expect(screen.getByText('brief.pdf')).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Send' }));

        await waitFor(() => expect(screen.queryByText('brief.pdf')).not.toBeInTheDocument());
        expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
            '/genaix/api/v1/sessions/s-1/files',
            '/genaix/api/v1/sessions/s-1/messages',
        ]);
        expect(JSON.parse(fetchMock.mock.calls[1]![1]!.body as string)).toEqual({
            parts: [{ type: 'file_ref', file_id: 'f-1', mode: 'source' }],
        });
    });

    it('keeps the input and shows an error when sending fails', async () => {
        const user = userEvent.setup();

        stubFetch(Response.json({ type: 't', title: 't', status: 409, genaix_code: 'run_already_active' }, { status: 409 }));

        const field = await renderComposer();

        await user.click(field);
        await user.keyboard('Shorten the intro{Enter}');

        await waitFor(() => expect(useErrorNotificationStore.getState().errors).toMatchObject([
            { messageKey: 'chat.sendFailed', detailKey: 'errors.genaix.run_already_active' },
        ]));
        expect(field).toHaveTextContent('Shorten the intro');
    });

    it('dictates into the field without sending by itself', async () => {
        vi.stubGlobal('SpeechRecognition', FakeRecognizer);

        const user = userEvent.setup();
        const fetchMock = stubFetch();
        const field = await renderComposer();

        await user.click(screen.getByRole('button', { name: 'Dictate' }));
        act(() => FakeRecognizer.last!.say('Shorten the intro'));
        await user.click(screen.getByRole('button', { name: 'Stop dictating' }));

        expect(field).toHaveTextContent('Shorten the intro');
        expect(fetchMock).not.toHaveBeenCalled();
    });

    it('shows a disabled mic without speech recognition, saying why', async () => {
        await renderComposer();

        expect(screen.getByRole('button', { name: 'Voice input is not available in this browser' })).toHaveAttribute('aria-disabled', 'true');
    });

    it('offers attaching and verbatim as labelled buttons, as on the dashboard', async () => {
        await renderComposer();

        expect(screen.getByRole('button', { name: 'File' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Verbatim' })).toBeInTheDocument();
    });

    // The big mic shows only up to 640 px; jsdom applies no media query, so it counts as hidden
    // here and is queried with `hidden: true`. Its visibility is tested in e2e/SessionMobile.spec.ts.
    describe('voice first (mobile)', () => {
        it('offers the big mic and "Type instead" only with speech recognition', async () => {
            await renderComposer();

            expect(screen.queryByRole('button', { name: 'Tap to speak', hidden: true })).not.toBeInTheDocument();
            expect(screen.queryByRole('button', { name: 'Type instead', hidden: true })).not.toBeInTheDocument();
        });

        it('dictates from the big mic into the field without sending', async () => {
            vi.stubGlobal('SpeechRecognition', FakeRecognizer);

            const user = userEvent.setup();
            const fetchMock = stubFetch();
            const field = await renderComposer();

            await user.click(screen.getByRole('button', { name: 'Tap to speak', hidden: true }));
            act(() => FakeRecognizer.last!.say('Shorten the intro'));

            expect(field).toHaveTextContent('Shorten the intro');
            expect(screen.queryByRole('button', { name: 'Type instead', hidden: true })).not.toBeInTheDocument();

            await user.click(screen.getByRole('button', { name: 'Tap to stop', hidden: true }));

            expect(FakeRecognizer.last?.stop).toHaveBeenCalledTimes(1);
            expect(fetchMock).not.toHaveBeenCalled();
        });

        it('opens the text prompt with "Type instead" and focuses the field', async () => {
            vi.stubGlobal('SpeechRecognition', FakeRecognizer);

            const user = userEvent.setup();
            const field = await renderComposer();

            await user.click(screen.getByRole('button', { name: 'Type instead', hidden: true }));

            expect(field).toHaveFocus();
            expect(screen.queryByRole('button', { name: 'Type instead', hidden: true })).not.toBeInTheDocument();
            expect(screen.getByRole('button', { name: 'Tap to speak', hidden: true })).toBeInTheDocument();
        });
    });
});
