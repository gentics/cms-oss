import { create } from 'zustand';

import type { GenaixEvent } from '@/services/apiService/genaix/types';

/** `npm run dev:debug` (Vite mode `debug`): the session page shows the event log (`EventLog`). */
export const isEventLogEnabled = import.meta.env.MODE === 'debug';

interface EventLogState {
    /** Every event the stream of a session passed on, by session id, in the order they arrived. */
    events: Record<string, GenaixEvent[]>;
    /** Appends `event` to the log of `sessionId`. */
    add: (sessionId: string, event: GenaixEvent) => void;
}

/** The raw events of each session's stream, for the debug event log only. Filled only while `isEventLogEnabled`. */
export const useEventLogStore = create<EventLogState>((set) => ({
    events: {},

    add: (sessionId, event) =>
        set((state) => ({
            events: { ...state.events, [sessionId]: [...(state.events[sessionId] ?? []), event] },
        })),
}));
