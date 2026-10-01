import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { HttpError } from '@/services/httpService/httpService';
import { useCmsTokenStore } from '@/store/useCmsTokenStore';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';

import { type CmsTokenInfo, createCmsToken, getCmsToken } from './cmsApiService';

const NOW_S = 1_790_800_000;

function tokenInfo(overrides: Partial<CmsTokenInfo> = {}): Omit<CmsTokenInfo, 'token'> {
    return { id: 1, userId: 3, name: 'node', cdate: NOW_S - 3600, expires: NOW_S + 3600, lastUsed: 0, valid: true, ...overrides };
}

function stubFetchSequence(...responses: Response[]) {
    const fetchMock = vi.fn<typeof fetch>();

    for (const response of responses) {
        fetchMock.mockResolvedValueOnce(response);
    }

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

function createdResponse(overrides: Record<string, unknown> = {}) {
    return Response.json({ token: 'cmstok_secret', ...tokenInfo({ id: 4, name: 'genaix-workspace-1' }), ...overrides });
}

function postedBody(fetchMock: ReturnType<typeof stubFetchSequence>, call = 0): unknown {
    return JSON.parse(fetchMock.mock.calls[call]![1]?.body as string);
}

describe('createCmsToken', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('posts the name with the CMS session cookie and returns the created token', async () => {
        const fetchMock = stubFetchSequence(createdResponse());

        await expect(createCmsToken('genaix-workspace-1'))
            .resolves.toEqual({ token: 'cmstok_secret', ...tokenInfo({ id: 4, name: 'genaix-workspace-1' }) });

        const [url, init] = fetchMock.mock.calls[0]!;

        expect(url).toBe('/rest/admin/token');
        expect(init?.method).toBe('POST');
        expect(init?.credentials).toBe('same-origin');
        expect(postedBody(fetchMock)).toEqual({ name: 'genaix-workspace-1' });
    });

    it('throws an HttpError with the status when the CMS refuses', async () => {
        stubFetchSequence(new Response('Forbidden', { status: 403 }));

        const error = await createCmsToken('genaix-workspace-1').catch((caught: unknown) => caught);

        expect(error).toBeInstanceOf(HttpError);
        expect((error as HttpError).status).toBe(403);
    });
});

describe('getCmsToken', () => {
    const stored = { token: 'cmstok_stored', id: 7, name: 'genaix-workspace-stored', expires: NOW_S + 60 };
    const fromResponse = { token: 'cmstok_secret', id: 4, name: 'genaix-workspace-1', expires: NOW_S + 3600 };

    beforeEach(() => {
        useCmsTokenStore.getState().clearCmsToken();
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        vi.restoreAllMocks();
    });

    it('returns the token from the store without a request', async () => {
        const fetchMock = stubFetchSequence();

        useCmsTokenStore.getState().setCmsToken(stored);

        await expect(getCmsToken(NOW_S * 1000)).resolves.toEqual(stored);
        expect(fetchMock).not.toHaveBeenCalled();
    });

    it('returns a stored token that never expires', async () => {
        const fetchMock = stubFetchSequence();

        useCmsTokenStore.getState().setCmsToken({ ...stored, expires: 0 });

        await expect(getCmsToken(NOW_S * 1000)).resolves.toEqual({ ...stored, expires: 0 });
        expect(fetchMock).not.toHaveBeenCalled();
    });

    it('posts for a token when the store is empty and stores the token from the response', async () => {
        const fetchMock = stubFetchSequence(createdResponse());

        await expect(getCmsToken(NOW_S * 1000)).resolves.toEqual(fromResponse);

        expect(fetchMock).toHaveBeenCalledTimes(1);
        expect(fetchMock.mock.calls[0]![0]).toBe('/rest/admin/token');
        expect(fetchMock.mock.calls[0]![1]?.method).toBe('POST');
        expect(postedBody(fetchMock)).toEqual({ name: expect.stringMatching(/^genaix-workspace-/) });
        expect(useCmsTokenStore.getState().cmsToken).toEqual(fromResponse);
    });

    it('never reads the token list', async () => {
        const fetchMock = stubFetchSequence(createdResponse());

        await getCmsToken(NOW_S * 1000);

        expect(fetchMock.mock.calls.map(([, init]) => init?.method)).toEqual(['POST']);
    });

    it('posts again when the stored token has expired', async () => {
        const fetchMock = stubFetchSequence(createdResponse());

        useCmsTokenStore.getState().setCmsToken({ ...stored, expires: NOW_S });

        await expect(getCmsToken(NOW_S * 1000)).resolves.toEqual(fromResponse);
        expect(fetchMock).toHaveBeenCalledTimes(1);
    });

    it('posts again once the store has been cleared', async () => {
        const fetchMock = stubFetchSequence(createdResponse(), createdResponse({ token: 'cmstok_second', id: 5 }));

        await getCmsToken(NOW_S * 1000);
        useCmsTokenStore.getState().clearCmsToken();

        await expect(getCmsToken(NOW_S * 1000)).resolves.toMatchObject({ token: 'cmstok_second', id: 5 });
        expect(fetchMock).toHaveBeenCalledTimes(2);
    });

    it('sends one POST for parallel calls on an empty store', async () => {
        const fetchMock = stubFetchSequence(createdResponse(), createdResponse({ token: 'cmstok_second' }));

        const [first, second] = await Promise.all([getCmsToken(NOW_S * 1000), getCmsToken(NOW_S * 1000)]);

        expect(fetchMock).toHaveBeenCalledTimes(1);
        expect(second).toEqual(first);
    });

    it('rejects when the POST fails, leaves the store empty and tries again on the next call', async () => {
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
        const fetchMock = stubFetchSequence(new Response('Forbidden', { status: 403 }), createdResponse());

        await expect(getCmsToken(NOW_S * 1000)).rejects.toBeInstanceOf(HttpError);
        expect(useCmsTokenStore.getState().cmsToken).toBeNull();

        await expect(getCmsToken(NOW_S * 1000)).resolves.toEqual(fromResponse);
        expect(fetchMock).toHaveBeenCalledTimes(2);
    });

    describe('error notification', () => {
        beforeEach(() => {
            useErrorNotificationStore.setState({ errors: [] });
        });

        // The query shows it, once after its last retry (`createQueryClient`), not every attempt.
        it('shows no notification itself for a failed POST, and logs nothing', async () => {
            const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);

            stubFetchSequence(new Response('Forbidden', { status: 403 }));

            await getCmsToken(NOW_S * 1000).catch(() => undefined);

            expect(useErrorNotificationStore.getState().errors).toEqual([]);
            expect(errorSpy).not.toHaveBeenCalled();
        });

        it('shows no notification when a token is stored or created', async () => {
            stubFetchSequence(createdResponse());

            await getCmsToken(NOW_S * 1000);
            await getCmsToken(NOW_S * 1000);

            expect(useErrorNotificationStore.getState().errors).toEqual([]);
        });
    });
});
