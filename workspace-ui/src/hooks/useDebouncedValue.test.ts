import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useDebouncedValue } from './useDebouncedValue';

describe('useDebouncedValue', () => {
    beforeEach(() => {
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('passes a value on only after it stayed the same for the delay', () => {
        const { result, rerender } = renderHook(({ value }) => useDebouncedValue(value, 250), { initialProps: { value: '' } });

        rerender({ value: 'te' });
        act(() => vi.advanceTimersByTime(200));
        rerender({ value: 'terms' });
        act(() => vi.advanceTimersByTime(200));

        expect(result.current).toBe('');

        act(() => vi.advanceTimersByTime(50));

        expect(result.current).toBe('terms');
    });
});
