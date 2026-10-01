import { afterEach, describe, expect, it, vi } from 'vitest';

import { HttpError, httpRequest } from './httpService';

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

    it('rejects with an HttpError that carries the status', async () => {
        vi.stubGlobal('fetch', vi.fn<typeof fetch>().mockResolvedValue(new Response('Forbidden', { status: 403 })));

        const error = await httpRequest('/rest/admin/token').catch((caught: unknown) => caught);

        expect(error).toBeInstanceOf(HttpError);
        expect((error as HttpError).status).toBe(403);
    });

    it('carries the JSON body and the headers of the response', async () => {
        const body = { messages: [{ message: 'Not allowed' }], responseInfo: { responseCode: 'PERMISSION' } };

        vi.stubGlobal('fetch', vi.fn<typeof fetch>().mockResolvedValue(
            Response.json(body, { status: 403, headers: { 'X-Request-Id': 'req_1' } }),
        ));

        const error = await httpRequest('/rest/admin/token').catch((caught: unknown) => caught) as HttpError;

        expect(error.body).toEqual(body);
        expect(error.headers?.get('X-Request-Id')).toBe('req_1');
    });

    it('leaves the body out when it is not JSON', async () => {
        vi.stubGlobal('fetch', vi.fn<typeof fetch>().mockResolvedValue(new Response('Forbidden', { status: 403 })));

        const error = await httpRequest('/rest/admin/token').catch((caught: unknown) => caught) as HttpError;

        expect(error.status).toBe(403);
        expect(error.body).toBeUndefined();
    });

    it('throws the error toError builds', async () => {
        const response = new Response(null, { status: 409 });
        const custom = new Error('custom');
        const toError = vi.fn(() => Promise.resolve(custom));

        vi.stubGlobal('fetch', vi.fn<typeof fetch>().mockResolvedValue(response));

        await expect(httpRequest('/x', undefined, toError)).rejects.toBe(custom);
        expect(toError).toHaveBeenCalledWith('/x', response);
    });

    it('does not call toError for a successful response', async () => {
        const toError = vi.fn(() => Promise.resolve(new Error('unused')));

        vi.stubGlobal('fetch', vi.fn<typeof fetch>().mockResolvedValue(Response.json({ ok: true })));

        await expect(httpRequest('/x', undefined, toError)).resolves.toEqual({ ok: true });
        expect(toError).not.toHaveBeenCalled();
    });
});
