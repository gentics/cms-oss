import { afterEach, describe, expect, it, vi } from 'vitest';

import { httpRequest } from './http';

describe('httpRequest', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('returns the JSON body of a successful response', async () => {
        vi.stubGlobal('fetch', vi.fn<typeof fetch>().mockResolvedValue(Response.json({ ok: true })));

        await expect(httpRequest('/genaix/api/v1/me')).resolves.toEqual({ ok: true });
    });

    it('rejects when the response is not successful', async () => {
        vi.stubGlobal('fetch', vi.fn<typeof fetch>().mockResolvedValue(new Response(null, { status: 401 })));

        await expect(httpRequest('/genaix/api/v1/me')).rejects.toThrow('failed with status 401');
    });
});
