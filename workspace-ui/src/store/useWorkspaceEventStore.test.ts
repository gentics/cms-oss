import { beforeEach, describe, expect, it } from 'vitest';

import type { GenaixEvent } from '@/services/apiService/genaix/types';

import { persistedMessageKeys, selectSession, useWorkspaceEventStore } from './useWorkspaceEventStore';

const SESSION = 's-1';

// Builds a stream event; only the fields the store reads matter here.
function event(seq: number, type: string, payload: Record<string, unknown> = {}): GenaixEvent {
    return { seq, ts: '2026-10-07T09:00:00Z', session_id: SESSION, type, ...payload } as GenaixEvent;
}

function session() {
    return selectSession(SESSION)(useWorkspaceEventStore.getState());
}

function apply(...events: GenaixEvent[]) {
    events.forEach((e) => useWorkspaceEventStore.getState().applyEvent(SESSION, e));
}

const text = (value: string) => ({ type: 'text', format: 'markdown', text: value });

describe('useWorkspaceEventStore', () => {
    beforeEach(() => {
        useWorkspaceEventStore.setState({ sessions: {} });
    });

    it('starts every session empty', () => {
        expect(session()).toEqual({ lastSeq: 0, connection: 'closed', messages: [] });
    });

    describe('sent messages', () => {
        it('goes from sending to sent with the server id', () => {
            const { addSentMessage, markMessageSent } = useWorkspaceEventStore.getState();

            addSentMessage(SESSION, 'local-1', { content: 'Hello' });
            expect(session().messages).toEqual([{ kind: 'sent', localId: 'local-1', request: { content: 'Hello' }, status: 'sending' }]);

            markMessageSent(SESSION, 'local-1', 'm-1');
            expect(session().messages).toEqual([{ kind: 'sent', localId: 'local-1', messageId: 'm-1', request: { content: 'Hello' }, status: 'sent' }]);
        });

        it('keeps a failed turn with its error', () => {
            const { addSentMessage, markSendFailed } = useWorkspaceEventStore.getState();
            const error = new Error('409');

            addSentMessage(SESSION, 'local-1', { content: 'Hello' });
            markSendFailed(SESSION, 'local-1', error);

            expect(session().messages[0]).toMatchObject({ status: 'send_failed', error });
        });

        it('does not show the stream echo of its own turn twice', () => {
            const { addSentMessage, markMessageSent } = useWorkspaceEventStore.getState();

            addSentMessage(SESSION, 'local-1', { content: 'Hello' });
            markMessageSent(SESSION, 'local-1', 'm-user');
            apply(event(1, 'message.started', { message_id: 'm-user', role: 'user' }));

            expect(session().messages).toHaveLength(1);
        });
    });

    describe('received messages', () => {
        it('assembles a streamed assistant message', () => {
            apply(
                event(1, 'message.started', { message_id: 'm-1', role: 'assistant' }),
                event(2, 'part.started', { message_id: 'm-1', part_index: 0, part: text('') }),
                event(3, 'part.delta', { message_id: 'm-1', part_index: 0, delta: 'Hello', patch_format: 'append' }),
                event(4, 'part.delta', { message_id: 'm-1', part_index: 0, delta: ' world', patch_format: 'append' }),
            );

            expect(session().messages).toEqual([{ kind: 'received', id: 'm-1', role: 'assistant', parts: [text('Hello world')], status: 'streaming' }]);

            apply(
                event(5, 'part.completed', { message_id: 'm-1', part_index: 0, part: text('Hello world') }),
                event(6, 'message.completed', { message_id: 'm-1', part_count: 1 }),
            );

            expect(session().messages[0]).toMatchObject({ parts: [text('Hello world')], status: 'completed' });
            expect(session().lastSeq).toBe(6);
        });

        it('applies a merge_patch delta to a structured part', () => {
            apply(
                event(1, 'part.started', { message_id: 'm-1', part_index: 0, part: { type: 'table', label: 'Pages', columns: [], rows: [] } }),
                event(2, 'part.delta', { message_id: 'm-1', part_index: 0, delta: { rows: [{ page: 'a' }], label: null }, patch_format: 'merge_patch' }),
            );

            expect(session().messages[0]).toMatchObject({ parts: [{ type: 'table', columns: [], rows: [{ page: 'a' }] }] });
            expect(session().messages[0]).not.toHaveProperty('parts.0.label');
        });

        it('trusts part.completed over the accumulated copy', () => {
            apply(
                event(1, 'part.started', { message_id: 'm-1', part_index: 0, part: text('') }),
                event(2, 'part.delta', { message_id: 'm-1', part_index: 0, delta: 'Helo', patch_format: 'append' }),
                event(3, 'part.completed', { message_id: 'm-1', part_index: 0, part: text('Hello') }),
            );

            expect(session().messages[0]).toMatchObject({ parts: [text('Hello')] });
        });

        it('keeps parts by part_index, also when they start out of order', () => {
            apply(
                event(1, 'part.started', { message_id: 'm-1', part_index: 1, part: text('second') }),
                event(2, 'part.started', { message_id: 'm-1', part_index: 0, part: text('first') }),
            );

            expect(session().messages[0]).toMatchObject({ parts: [text('first'), text('second')] });
        });

        it('creates an assistant message when its message.started was missed', () => {
            apply(event(9, 'part.started', { message_id: 'm-1', part_index: 0, part: text('Hi') }));

            expect(session().messages).toEqual([{ kind: 'received', id: 'm-1', role: 'assistant', parts: [text('Hi')], status: 'streaming' }]);
        });

        it('ignores a replayed event at or below lastSeq', () => {
            apply(
                event(1, 'part.started', { message_id: 'm-1', part_index: 0, part: text('') }),
                event(2, 'part.delta', { message_id: 'm-1', part_index: 0, delta: 'Hi', patch_format: 'append' }),
                event(2, 'part.delta', { message_id: 'm-1', part_index: 0, delta: 'Hi', patch_format: 'append' }),
                event(1, 'part.started', { message_id: 'm-1', part_index: 0, part: text('') }),
            );

            expect(session().messages[0]).toMatchObject({ parts: [text('Hi')] });
            expect(session().messages).toHaveLength(1);
        });

        it('ignores types it does not handle, and unknown ones, but advances lastSeq', () => {
            apply(event(1, 'heartbeat'), event(2, 'plan.updated', { plan: {} }), event(3, 'something.new', { x: 1 }));

            expect(session()).toMatchObject({ lastSeq: 3, messages: [] });
        });
    });

    it('persistedMessageKeys and clearMessages drop only what the server has persisted', () => {
        const { addSentMessage, markMessageSent, clearMessages } = useWorkspaceEventStore.getState();

        addSentMessage(SESSION, 'local-sent', { content: 'one' });
        markMessageSent(SESSION, 'local-sent', 'm-user');
        addSentMessage(SESSION, 'local-sending', { content: 'two' });
        apply(
            event(1, 'message.started', { message_id: 'm-done', role: 'assistant' }),
            event(2, 'message.completed', { message_id: 'm-done' }),
            event(3, 'message.started', { message_id: 'm-streaming', role: 'assistant' }),
        );

        const keys = persistedMessageKeys(session());

        expect(keys).toEqual(['local-sent', 'm-done']);

        clearMessages(SESSION, keys);

        expect(session().messages.map((m) => (m.kind === 'sent' ? m.localId : m.id))).toEqual(['local-sending', 'm-streaming']);
    });

    it('resetSession goes back to lastSeq 0 and keeps only unsent turns', () => {
        const { addSentMessage, markSendFailed, resetSession } = useWorkspaceEventStore.getState();

        addSentMessage(SESSION, 'local-failed', { content: 'one' });
        markSendFailed(SESSION, 'local-failed', new Error('x'));
        apply(event(7, 'message.started', { message_id: 'm-1', role: 'assistant' }));

        resetSession(SESSION);

        expect(session().lastSeq).toBe(0);
        expect(session().messages).toMatchObject([{ localId: 'local-failed' }]);

        apply(event(1, 'message.started', { message_id: 'm-1', role: 'assistant' }));
        expect(session().messages).toHaveLength(2);
    });

    describe('runs', () => {
        const run = (status: string) => ({ run: { id: 'r-1', status, started_at: '2026-10-07T09:00:00Z', step_count: 0 } });
        const problem = { type: 't', title: 'Locked', status: 423, genaix_code: 'cms_conflict' };

        it('follows the active run from run.started to its terminal event, with the latest status text', () => {
            apply(event(1, 'run.started', { run_id: 'r-1', ...run('running') }));
            expect(session().run).toEqual({ id: 'r-1', status: 'running' });

            apply(event(2, 'status', { run_id: 'r-1', text: 'Reading the source', kind: 'reading' }));
            expect(session().run).toEqual({ id: 'r-1', status: 'running', statusText: 'Reading the source' });

            apply(event(3, 'run.completed', { run_id: 'r-1', ...run('completed') }));
            expect(session().run).toBeNull();
        });

        it('drops the status text once the agent starts writing a part', () => {
            apply(event(1, 'run.started', { run_id: 'r-1', ...run('running') }));
            apply(event(2, 'status', { run_id: 'r-1', text: 'Reading the previous turn', kind: 'reading' }));
            apply(event(3, 'message.started', { run_id: 'r-1', message_id: 'm-1', role: 'assistant' }));
            expect(session().run).toMatchObject({ statusText: 'Reading the previous turn' });

            apply(event(4, 'part.started', { run_id: 'r-1', message_id: 'm-1', part_index: 0, part: { type: 'text', format: 'markdown', text: '' } }));
            expect(session().run).toEqual({ id: 'r-1', status: 'running' });

            apply(event(5, 'status', { run_id: 'r-1', text: 'Checking the page', kind: 'calling_cms' }));
            expect(session().run).toEqual({ id: 'r-1', status: 'running', statusText: 'Checking the page' });
        });

        it('markRunCancelling marks the active run until run.cancelled ends it with a notice', () => {
            apply(event(1, 'run.started', { run_id: 'r-1', ...run('running') }));
            useWorkspaceEventStore.getState().markRunCancelling(SESSION, 'r-1');
            expect(session().run).toMatchObject({ status: 'cancelling' });

            apply(event(2, 'run.cancelled', { run_id: 'r-1', ...run('cancelled') }));
            expect(session().run).toBeNull();
            expect(session().notices).toEqual([{ seq: 2, runId: 'r-1', kind: 'cancelled' }]);
        });

        it('records failures, recoverable errors and auth.required as notices, but not an unrecoverable error', () => {
            apply(
                event(1, 'error', { run_id: 'r-1', error: problem, recoverable: true }),
                event(2, 'auth.required', { run_id: 'r-1', connection_id: 'c-1', connector: 'cms', reason: 'invalid', how_to_fix: 'Register a token.' }),
                event(3, 'error', { run_id: 'r-1', error: problem, recoverable: false }),
                event(4, 'run.failed', { run_id: 'r-1', ...run('failed'), error: problem }),
            );

            expect(session().notices).toEqual([
                { seq: 1, runId: 'r-1', kind: 'error', error: problem },
                { seq: 2, runId: 'r-1', kind: 'auth', connector: 'cms', howToFix: 'Register a token.' },
                { seq: 4, runId: 'r-1', kind: 'failed', error: problem },
            ]);
        });

        it('keeps the run_id on received messages', () => {
            apply(event(1, 'message.started', { run_id: 'r-1', message_id: 'm-1', role: 'assistant' }));

            expect(session().messages[0]).toMatchObject({ id: 'm-1', runId: 'r-1' });
        });
    });

    describe('interactions', () => {
        const interaction = { id: 'i-1', kind: 'confirm', prompt: 'Create it?', blocking: true, status: 'pending', run_id: 'r-1' };

        it('holds a requested interaction until it is resolved, and the run waits meanwhile', () => {
            apply(
                event(1, 'run.started', { run_id: 'r-1', run: { id: 'r-1', status: 'running' } }),
                event(2, 'interaction.requested', { run_id: 'r-1', interaction }),
            );

            expect(session().interactions).toEqual([interaction]);
            expect(session().run).toMatchObject({ status: 'waiting_for_input' });

            apply(event(3, 'interaction.resolved', { run_id: 'r-1', interaction_id: 'i-1', by: 'user', answer: { approved: true } }));

            expect(session().interactions).toEqual([]);
            expect(session().run).toMatchObject({ status: 'running' });
        });

        it('drops the interactions of a run that ended', () => {
            apply(
                event(1, 'interaction.requested', { run_id: 'r-1', interaction }),
                event(2, 'run.cancelled', { run_id: 'r-1', run: { id: 'r-1', status: 'cancelled' } }),
            );

            expect(session().interactions).toEqual([]);
        });

        it('keeps the user message a confirmed settings review became, and where in the agent\'s message it came', () => {
            const settings = [{ type: 'setting', key: 'template', value: '17', label: 'Kampagnen-Landingpage' }];

            apply(
                event(1, 'message.started', { run_id: 'r-1', message_id: 'm-1', role: 'assistant' }),
                event(2, 'part.completed', { run_id: 'r-1', message_id: 'm-1', part_index: 0, part: text('I read the update.') }),
                event(3, 'interaction.resolved', { run_id: 'r-1', interaction_id: 'i-1', by: 'user', answer: { settings }, message_id: 'c-1' }),
            );

            expect(session().messages[1]).toEqual({
                kind: 'received',
                id: 'c-1',
                role: 'user',
                runId: 'r-1',
                interactionId: 'i-1',
                parts: settings,
                status: 'completed',
            });
            expect(session().confirmations).toEqual([{ seq: 3, messageId: 'c-1', afterMessageId: 'm-1', afterPart: 1 }]);
        });

        it('keeps no message for an answer GenAIx stored none for', () => {
            apply(event(1, 'interaction.resolved', { run_id: 'r-1', interaction_id: 'i-1', by: 'user', answer: { approved: true } }));

            expect(session().messages).toEqual([]);
            expect(session().confirmations).toBeUndefined();
        });
    });

    describe('plans', () => {
        const plan = { version: 1, summary: 'Create the page in Richtlinien.', items: [] };

        it('keeps each plan.updated with the agent message and the number of its parts it came after', () => {
            apply(
                event(1, 'plan.updated', { run_id: 'r-1', plan }),
                event(2, 'message.started', { run_id: 'r-1', message_id: 'm-1', role: 'assistant' }),
                event(3, 'part.completed', { run_id: 'r-1', message_id: 'm-1', part_index: 0, part: text('Here is the plan.') }),
                event(4, 'plan.updated', { run_id: 'r-1', plan: { ...plan, version: 2 } }),
            );

            expect(session().plans).toEqual([
                { seq: 1, plan, runId: 'r-1' },
                { seq: 4, plan: { ...plan, version: 2 }, runId: 'r-1', afterMessageId: 'm-1', afterPart: 1 },
            ]);
        });

        it('resetSession drops the plans and the confirmations', () => {
            const settings = [{ type: 'setting', key: 'language', value: 'de', label: 'Deutsch' }];

            apply(
                event(1, 'plan.updated', { run_id: 'r-1', plan }),
                event(2, 'interaction.resolved', { run_id: 'r-1', interaction_id: 'i-1', by: 'user', answer: { settings }, message_id: 'c-1' }),
            );

            expect(session().plans).toHaveLength(1);
            expect(session().confirmations).toHaveLength(1);

            useWorkspaceEventStore.getState().resetSession(SESSION);

            expect(session().plans).toBeUndefined();
            expect(session().confirmations).toBeUndefined();
        });
    });

    describe('workflow steps', () => {
        it('keeps each step.updated by step id, a later one replacing it', () => {
            apply(
                event(1, 'step.updated', { run_id: 'r-1', step: { id: 'st-1', label: 'Read source material', status: 'running' } }),
                event(2, 'step.updated', { run_id: 'r-1', step: { id: 'st-2', label: 'Derive folder', status: 'pending' } }),
                event(3, 'step.updated', { run_id: 'r-1', step: { id: 'st-1', label: 'Read source material', status: 'done', detail: '14 pages' } }),
            );

            expect(session().steps).toEqual({
                'st-1': { id: 'st-1', label: 'Read source material', status: 'done', detail: '14 pages' },
                'st-2': { id: 'st-2', label: 'Derive folder', status: 'pending' },
            });
        });

        it('ignores the update of a substep', () => {
            apply(event(1, 'step.updated', { step: { id: 'st-2-2', label: 'Check templates', status: 'done' }, parent_step_id: 'st-2' }));

            expect(session().steps).toBeUndefined();
        });

        it('resetSession drops the steps', () => {
            apply(event(1, 'step.updated', { step: { id: 'st-1', label: 'Read source material', status: 'running' } }));

            useWorkspaceEventStore.getState().resetSession(SESSION);

            expect(session().steps).toBeUndefined();
        });
    });

    it('resetSession also drops the run, the notices and the interactions', () => {
        apply(
            event(1, 'run.started', { run_id: 'r-1', run: { id: 'r-1', status: 'running' } }),
            event(2, 'interaction.requested', { run_id: 'r-1', interaction: { id: 'i-1', run_id: 'r-1' } }),
            event(3, 'error', { run_id: 'r-1', error: { title: 'x' }, recoverable: true }),
        );

        useWorkspaceEventStore.getState().resetSession(SESSION);

        expect(session()).toMatchObject({ lastSeq: 0, run: undefined, notices: undefined, interactions: undefined });
    });

    it('setConnection records the stream status per session', () => {
        useWorkspaceEventStore.getState().setConnection(SESSION, 'open');

        expect(session().connection).toBe('open');
        expect(selectSession('other')(useWorkspaceEventStore.getState()).connection).toBe('closed');
    });
});
