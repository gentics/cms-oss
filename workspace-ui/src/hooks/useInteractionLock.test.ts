import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { GenaixApiError } from '@/services/apiService/apiService';
import type { GenaixCode, Interaction } from '@/services/apiService/genaix/types';
import { useInteractionDraftStore } from '@/store/useInteractionDraftStore';

import { lockAfterFailure, useInteractionLock } from './useInteractionLock';

function interaction(expiresAt: string): Interaction {
    return { id: 'i-1', kind: 'confirm', prompt: 'Publish now?', multi: false, blocking: true, status: 'pending', requested_at: '2026-10-07T09:23:16Z', expires_at: expiresAt };
}

function refusal(status: number, code: GenaixCode) {
    return new GenaixApiError('/rest/proxy/genaix/sessions/s-1/interactions/i-1', status, { type: 't', title: 'Refused', status, genaix_code: code }, new Headers());
}

describe('useInteractionLock', () => {
    beforeEach(() => {
        vi.useFakeTimers({ now: new Date('2026-10-07T09:23:16Z') });
        useInteractionDraftStore.setState({ drafts: {}, locks: {} });
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('turns to expired at the moment `expires_at` passes', () => {
        const { result } = renderHook(() => useInteractionLock(interaction('2026-10-07T09:33:16Z')));

        expect(result.current).toBeUndefined();

        act(() => vi.advanceTimersByTime(10 * 60_000));

        expect(result.current).toBe('expired');
    });

    it('is expired at once for an `expires_at` already past', () => {
        const { result } = renderHook(() => useInteractionLock(interaction('2026-10-07T09:23:15Z')));

        expect(result.current).toBe('expired');
    });

    it('reports what the store recorded before the expiry', () => {
        useInteractionDraftStore.getState().lock('i-1', 'answered');

        const { result } = renderHook(() => useInteractionLock(interaction('2026-10-07T09:23:15Z')));

        expect(result.current).toBe('answered');
    });

    it('is undefined without an interaction', () => {
        const { result } = renderHook(() => useInteractionLock(undefined));

        expect(result.current).toBeUndefined();
    });
});

describe('lockAfterFailure', () => {
    beforeEach(() => {
        useInteractionDraftStore.setState({ drafts: {}, locks: { 'i-1': 'sending' } });
    });

    it.each([
        ['an expired interaction (410)', refusal(410, 'interaction_expired'), 'expired'],
        ['an interaction already answered (409)', refusal(409, 'interaction_already_answered'), 'answered'],
        ['a run still active (409)', refusal(409, 'run_already_active'), undefined],
        ['a failed request', new TypeError('Failed to fetch'), undefined],
    ])('after %s, locks it as %s', (_case, error, lock) => {
        lockAfterFailure('i-1', error);

        expect(useInteractionDraftStore.getState().locks['i-1']).toBe(lock);
    });
});
