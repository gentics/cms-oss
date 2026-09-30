import { afterEach, describe, expect, it, vi } from 'vitest';

import { getMe } from './apiService';

describe('getMe', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('calls /me through the proxy path and returns the body', async () => {
        const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(Response.json({ user: { subject: 'sub_test' } }));

        vi.stubGlobal('fetch', fetchMock);

        await expect(getMe()).resolves.toEqual({ user: { subject: 'sub_test' } });
        expect(fetchMock.mock.calls[0]![0]).toBe('/genaix/api/v1/me');
    });
});
