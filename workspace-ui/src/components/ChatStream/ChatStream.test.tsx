import { act, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { GenaixEvent, Message } from '@/services/apiService/genaix/types';
import { useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';
import { createWrapper } from '@/test/renderWithProviders';

import { ChatStream } from './ChatStream';

const SESSION = 's-1';

function event(seq: number, type: string, payload: Record<string, unknown> = {}): GenaixEvent {
    return { seq, ts: '2026-10-07T09:00:00Z', session_id: SESSION, type, ...payload } as GenaixEvent;
}

function apply(...events: GenaixEvent[]) {
    act(() => events.forEach((e) => useWorkspaceEventStore.getState().applyEvent(SESSION, e)));
}

function message(id: string, role: Message['role'], text: string, runId?: string): Message {
    return {
        id,
        role,
        run_id: runId,
        created_at: '2026-10-07T09:00:00Z',
        parts: role === 'user' ? [{ type: 'text', text }] : [{ type: 'text', format: 'plain', text }],
        references: [],
        files: [],
    };
}

// Answers the session's message history with `history`, in one page.
function stubHistory(history: Message[]) {
    vi.stubGlobal('fetch', vi.fn<typeof fetch>(async () => Response.json({ items: history, next_cursor: null })));
}

// Answers by route: the session (of `workflow`), the workflow catalogue with that module's steps, the history.
function stubSession(workflow: string, stepsTemplate: { id: string; label: string }[], history: Message[]) {
    const fetchMock = vi.fn<typeof fetch>(async (input) => {
        const url = String(input);

        if (url.endsWith('/workflows')) {
            return Response.json({ items: [{ id: workflow, version: '1.0.0', title: workflow, description: '', roles: [], steps_template: stepsTemplate }] });
        }

        if (url.endsWith(`/sessions/${SESSION}`)) {
            return Response.json({ id: SESSION, workflow });
        }

        return Response.json({ items: history, next_cursor: null });
    });

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

// The chat's own items, the steps card as `steps`.
function chatItems() {
    const [chat] = screen.getAllByRole('list');

    return Array.from(chat!.children).map((item) => (
        within(item as HTMLElement).queryByRole('group', { name: 'Steps' }) ? 'steps' : item.textContent
    ));
}

// The chat list's items as rendered, also the agent's head (hidden from assistive technology).
function listChildren() {
    const [chat] = screen.getAllByRole('list');

    return Array.from(chat!.children) as HTMLElement[];
}

const TEMPLATE = [{ id: 'st-1', label: 'Read source material' }, { id: 'st-2', label: 'Propose plan' }];

function texts() {
    return screen.getAllByRole('listitem').map((item) => item.textContent);
}

describe('ChatStream', () => {
    beforeEach(() => {
        useWorkspaceEventStore.setState({ sessions: {} });
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('shows the history, then what the stream added, each message once', async () => {
        stubHistory([message('m-1', 'user', 'Which pages are offline?', 'r-1'), message('m-2', 'assistant', 'Two pages are offline.', 'r-1')]);
        render(<ChatStream sessionId={SESSION} />, { wrapper: createWrapper() });

        await screen.findByText('Two pages are offline.');

        // The replay brings the persisted answer again, then a new one.
        apply(
            event(1, 'message.started', { run_id: 'r-1', message_id: 'm-2', role: 'assistant' }),
            event(2, 'part.completed', { run_id: 'r-1', message_id: 'm-2', part_index: 0, part: { type: 'text', format: 'plain', text: 'Two pages are offline.' } }),
            event(3, 'message.started', { run_id: 'r-2', message_id: 'm-3', role: 'assistant' }),
            event(4, 'part.started', { run_id: 'r-2', message_id: 'm-3', part_index: 0, part: { type: 'text', format: 'plain', text: 'Let me check' } }),
        );

        // The agent's avatar and name head its block once, above both of its messages.
        expect(chatItems()).toEqual(['YouWhich pages are offline?', 'Assistant', 'Two pages are offline.', 'Let me check']);
    });

    it('shows the streamed copy of a message the history lists while it is still in progress', async () => {
        // The history already lists the assistant message of the running turn, without its parts.
        stubHistory([message('m-1', 'user', 'Create a landing page', 'r-1'), { ...message('m-2', 'assistant', '', 'r-1'), parts: [] }]);
        render(<ChatStream sessionId={SESSION} />, { wrapper: createWrapper() });

        await screen.findByText('Create a landing page');
        apply(
            event(1, 'message.started', { run_id: 'r-1', message_id: 'm-2', role: 'assistant' }),
            event(2, 'part.started', { run_id: 'r-1', message_id: 'm-2', part_index: 0, part: { type: 'text', format: 'plain', text: 'Ich habe die Aktualisierung' } }),
        );

        expect(chatItems()).toEqual(['YouCreate a landing page', 'Assistant', 'Ich habe die Aktualisierung']);
    });

    it('shows a turn being sent, and the thinking indicator with the latest status while the run works', async () => {
        stubHistory([]);
        render(<ChatStream sessionId={SESSION} />, { wrapper: createWrapper() });

        act(() => useWorkspaceEventStore.getState().addSentMessage(SESSION, 'local-1', { parts: [{ type: 'text', text: 'Create a landing page' }] }));

        expect(await screen.findByRole('article', { name: 'You' })).toHaveTextContent('Create a landing page');
        expect(screen.getByRole('status')).toHaveTextContent('Thinking');

        apply(
            event(1, 'run.started', { run_id: 'r-1', run: { id: 'r-1', status: 'running' } }),
            event(2, 'status', { run_id: 'r-1', text: 'Reading the source material', kind: 'reading' }),
        );
        act(() => useWorkspaceEventStore.getState().markMessageSent(SESSION, 'local-1', 'm-1'));

        expect(screen.getByRole('status')).toHaveTextContent('Reading the source material');

        apply(event(3, 'run.completed', { run_id: 'r-1', run: { id: 'r-1', status: 'completed' } }));

        expect(screen.queryByRole('status')).not.toBeInTheDocument();
    });

    it('hides the thinking indicator while the run waits for an answer, and shows the question instead', async () => {
        stubHistory([]);
        render(<ChatStream sessionId={SESSION} />, { wrapper: createWrapper() });

        apply(
            event(1, 'run.started', { run_id: 'r-1', run: { id: 'r-1', status: 'running' } }),
            event(2, 'interaction.requested', {
                run_id: 'r-1',
                interaction: { id: 'i-1', kind: 'confirm', prompt: 'Create it as a draft?', blocking: true, status: 'pending', run_id: 'r-1' },
            }),
        );

        expect(await screen.findByText('Create it as a draft?')).toBeInTheDocument();
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
    });

    it('shows how a run ended after the last message of that run', async () => {
        stubHistory([message('m-1', 'user', 'First', 'r-1'), message('m-2', 'assistant', 'Answer one', 'r-1'), message('m-3', 'user', 'Second', 'r-2')]);
        render(<ChatStream sessionId={SESSION} />, { wrapper: createWrapper() });

        await screen.findByText('Answer one');
        apply(event(1, 'run.cancelled', { run_id: 'r-1', run: { id: 'r-1', status: 'cancelled' } }));

        const items = screen.getAllByRole('listitem');

        expect(within(items[2]!).getByRole('note')).toHaveTextContent('Stopped');
        expect(items[3]).toHaveTextContent('Second');
    });

    describe('the agent\'s head', () => {
        const plan = { version: 1, summary: 'Create the page in Richtlinien.', items: [] };

        it('shows first, before a plan and the thinking indicator, while the agent has not written yet', async () => {
            stubHistory([]);
            render(<ChatStream sessionId={SESSION} />, { wrapper: createWrapper() });

            act(() => useWorkspaceEventStore.getState().addSentMessage(SESSION, 'local-1', { parts: [{ type: 'text', text: 'Create a landing page' }] }));
            await screen.findByRole('article', { name: 'You' });
            apply(
                event(1, 'run.started', { run_id: 'r-1', run: { id: 'r-1', status: 'running' } }),
                event(2, 'plan.updated', { run_id: 'r-1', plan }),
            );

            const items = listChildren();

            expect(items.map((item) => item.textContent).slice(0, 2)).toEqual(['YouCreate a landing page', 'Assistant']);
            expect(items[2]).toHaveTextContent('Create the page in Richtlinien.');
            expect(items[3]).toHaveTextContent('Thinking');
        });

        it('is left out of the list for assistive technology, each message naming its sender instead', async () => {
            stubHistory([message('m-1', 'user', 'Which pages are offline?', 'r-1'), message('m-2', 'assistant', 'Two pages are offline.', 'r-1')]);
            render(<ChatStream sessionId={SESSION} />, { wrapper: createWrapper() });

            await screen.findByText('Two pages are offline.');

            expect(listChildren()[1]).toHaveAttribute('aria-hidden', 'true');
            expect(texts()).toEqual(['YouWhich pages are offline?', 'Two pages are offline.']);
            expect(screen.getByRole('article', { name: 'Assistant' })).toHaveTextContent('Two pages are offline.');
        });
    });

    describe('stream order', () => {
        const plan = { version: 1, summary: 'Create the page in Richtlinien.', items: [] };
        const settings = [{ type: 'setting', key: 'template', value: '17', label: 'Kampagnen-Landingpage' }];

        it('splits an agent message where a confirmation and a plan came while it was streaming', async () => {
            stubHistory([]);
            render(<ChatStream sessionId={SESSION} />, { wrapper: createWrapper() });

            apply(
                event(1, 'run.started', { run_id: 'r-1', run: { id: 'r-1', status: 'running' } }),
                event(2, 'message.started', { run_id: 'r-1', message_id: 'm-1', role: 'assistant' }),
                event(3, 'part.completed', { run_id: 'r-1', message_id: 'm-1', part_index: 0, part: { type: 'text', format: 'plain', text: 'I read the update.' } }),
                event(4, 'interaction.resolved', { run_id: 'r-1', interaction_id: 'i-1', by: 'user', answer: { settings }, message_id: 'c-1' }),
                event(5, 'plan.updated', { run_id: 'r-1', plan }),
                event(6, 'part.completed', { run_id: 'r-1', message_id: 'm-1', part_index: 1, part: { type: 'text', format: 'plain', text: 'I create the page now.' } }),
            );

            await screen.findByText('I create the page now.');

            const items = listChildren();

            expect(items[0]).toHaveTextContent(/^Assistant$/);
            expect(items[1]).toHaveTextContent(/^I read the update\.$/);
            expect(within(items[2]!).getByRole('region', { name: 'Check settings' })).toHaveTextContent('Kampagnen-Landingpage');
            expect(items[2]).toHaveTextContent('Confirmed');
            expect(items[3]).toHaveTextContent('Create the page in Richtlinien.');
            expect(items[4]).toHaveTextContent(/^I create the page now\.$/);
            expect(items[5]).toHaveTextContent('Thinking');
            // The confirmation is no turn of the user's: no bubble, no second head, one card.
            expect(screen.queryByRole('article', { name: 'You' })).not.toBeInTheDocument();
            expect(items.filter((item) => item.getAttribute('aria-hidden') === 'true')).toHaveLength(1);
            expect(screen.getAllByRole('region', { name: 'Check settings' })).toHaveLength(1);
        });

        it('keeps the split once the history holds the message and its confirmation', async () => {
            const confirmation: Message = { ...message('c-1', 'user', '', 'r-1'), interaction_id: 'i-1', parts: settings as Message['parts'] };
            const answer: Message = {
                ...message('m-2', 'assistant', '', 'r-1'),
                parts: [{ type: 'text', format: 'plain', text: 'I read the update.' }, { type: 'text', format: 'plain', text: 'I create the page now.' }],
            };

            // The history lists the confirmation after the whole answer.
            stubHistory([message('m-1', 'user', 'Create a landing page', 'r-1'), answer, confirmation]);
            render(<ChatStream sessionId={SESSION} />, { wrapper: createWrapper() });
            await screen.findByText('I create the page now.');

            // The replay of the stream says where the confirmation came; then the live copies go.
            apply(
                event(1, 'message.started', { run_id: 'r-1', message_id: 'm-2', role: 'assistant' }),
                event(2, 'part.completed', { run_id: 'r-1', message_id: 'm-2', part_index: 0, part: { type: 'text', format: 'plain', text: 'I read the update.' } }),
                event(3, 'interaction.resolved', { run_id: 'r-1', interaction_id: 'i-1', by: 'user', answer: { settings }, message_id: 'c-1' }),
            );
            act(() => useWorkspaceEventStore.getState().clearMessages(SESSION, ['m-2', 'c-1']));

            const items = listChildren();

            expect(items.map((item) => item.textContent).slice(0, 3)).toEqual(['YouCreate a landing page', 'Assistant', 'I read the update.']);
            expect(items[3]).toHaveTextContent('Confirmed');
            expect(items[4]).toHaveTextContent(/^I create the page now\.$/);
            // Shown once: at its place from the stream, not again where the history lists it.
            expect(screen.getAllByRole('region', { name: 'Check settings' })).toHaveLength(1);
            expect(items).toHaveLength(5);
        });

        it('shows a confirmation from the history as the confirmed card in the agent\'s block, where it has no place from the stream', async () => {
            const confirmation: Message = { ...message('c-1', 'user', '', 'r-1'), interaction_id: 'i-1', parts: settings as Message['parts'] };

            stubHistory([message('m-1', 'user', 'Create a landing page', 'r-1'), message('m-2', 'assistant', 'I read the update.', 'r-1'), confirmation]);
            render(<ChatStream sessionId={SESSION} />, { wrapper: createWrapper() });

            const card = await screen.findByRole('region', { name: 'Check settings' });

            expect(card).toHaveTextContent('Kampagnen-Landingpage');
            expect(chatItems().slice(0, 3)).toEqual(['YouCreate a landing page', 'Assistant', 'I read the update.']);
            expect(screen.getAllByRole('article', { name: 'You' })).toHaveLength(1);
            expect(listChildren().filter((item) => item.getAttribute('aria-hidden') === 'true')).toHaveLength(1);
        });
    });

    describe('workflow steps', () => {
        it('shows the current step right under the agent\'s head in its latest block, and moves it down with it', async () => {
            stubSession('content_create', TEMPLATE, [message('m-1', 'user', 'Create a landing page', 'r-1'), message('m-2', 'assistant', 'I read the source.', 'r-1')]);
            render(<ChatStream sessionId={SESSION} />, { wrapper: createWrapper() });

            const steps = await screen.findByRole('group', { name: 'Steps' });

            expect(chatItems()).toEqual(['YouCreate a landing page', 'Assistant', 'steps', 'I read the source.']);
            expect(steps).toHaveTextContent('0 of 2 steps done');

            apply(
                event(1, 'step.updated', { run_id: 'r-1', step: { id: 'st-1', label: 'Read source material', status: 'done' } }),
                event(2, 'step.updated', { run_id: 'r-2', step: { id: 'st-2', label: 'Propose plan', status: 'running' } }),
                event(3, 'message.started', { run_id: 'r-2', message_id: 'm-4', role: 'assistant' }),
                event(4, 'part.started', { run_id: 'r-2', message_id: 'm-4', part_index: 0, part: { type: 'text', format: 'plain', text: 'Here is the plan.' } }),
            );
            act(() => useWorkspaceEventStore.getState().addSentMessage(SESSION, 'local-1', { parts: [{ type: 'text', text: 'Go on' }] }));

            // The turn just sent opens the agent's next block: the steps go with it, under its head.
            expect(chatItems()).toEqual([
                'YouCreate a landing page',
                'Assistant',
                'I read the source.',
                'Here is the plan.',
                'YouGo on',
                'Assistant',
                'steps',
                'Thinking',
            ]);
            expect(screen.getByRole('group', { name: 'Steps' })).toHaveTextContent('Step 2 of 2: Propose plan');
        });

        it('shows the steps before the thinking indicator while the agent has not answered yet', async () => {
            stubSession('content_create', TEMPLATE, []);
            render(<ChatStream sessionId={SESSION} />, { wrapper: createWrapper() });

            act(() => useWorkspaceEventStore.getState().addSentMessage(SESSION, 'local-1', { parts: [{ type: 'text', text: 'Create a landing page' }] }));
            await screen.findByRole('group', { name: 'Steps' });

            expect(chatItems()).toEqual(['YouCreate a landing page', 'Assistant', 'steps', 'Thinking']);
        });

        it('opens the agent\'s next block with the steps once a sent turn is the last thing, before its run starts', async () => {
            stubSession('content_create', TEMPLATE, [message('m-1', 'user', 'Create a landing page', 'r-1'), message('m-2', 'assistant', 'I read the source.', 'r-1')]);
            render(<ChatStream sessionId={SESSION} />, { wrapper: createWrapper() });

            await screen.findByRole('group', { name: 'Steps' });
            act(() => {
                useWorkspaceEventStore.getState().addSentMessage(SESSION, 'local-1', { parts: [{ type: 'text', text: 'Go on' }] });
                useWorkspaceEventStore.getState().markMessageSent(SESSION, 'local-1', 'm-3');
            });

            expect(screen.queryByRole('status')).not.toBeInTheDocument();
            expect(chatItems()).toEqual(['YouCreate a landing page', 'Assistant', 'I read the source.', 'YouGo on', 'Assistant', 'steps']);
        });

        it('shows no steps for a workflow without any', async () => {
            const fetchMock = stubSession('free_chat', [], [message('m-1', 'user', 'Hello', 'r-1'), message('m-2', 'assistant', 'Hi.', 'r-1')]);

            render(<ChatStream sessionId={SESSION} />, { wrapper: createWrapper() });

            await screen.findByText('Hi.');
            await waitFor(() => expect(fetchMock.mock.calls.map(([input]) => String(input))).toContain('/rest/proxy/genaix/workflows'));

            expect(chatItems()).toEqual(['YouHello', 'Assistant', 'Hi.']);
        });
    });
});
