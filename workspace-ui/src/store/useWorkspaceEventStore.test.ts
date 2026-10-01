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

    it('setConnection records the stream status per session', () => {
        useWorkspaceEventStore.getState().setConnection(SESSION, 'open');

        expect(session().connection).toBe('open');
        expect(selectSession('other')(useWorkspaceEventStore.getState()).connection).toBe('closed');
    });
});
