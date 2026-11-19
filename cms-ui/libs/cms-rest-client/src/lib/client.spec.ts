import { GCMSRestClient } from './client';
import { GCMSClientDriver, GCMSRestClientRequest, GCMSRestClientRequestData, RequestMethod } from './models';

interface CapturedRequest {
    request: GCMSRestClientRequestData;
    body?: null | string | FormData;
}

/** Driver which only records the requests the client would send. */
function createCapturingDriver(captured: CapturedRequest[]): GCMSClientDriver {
    const record = <T>(request: GCMSRestClientRequestData, body?: null | string | FormData): GCMSRestClientRequest<T> => {
        captured.push({ request, body });
        return {
            send: () => Promise.resolve(null as T),
            cancel: () => {},
        };
    };

    return {
        performMappedRequest: record,
        performRawRequest: record,
        performDownloadRequest: record,
    };
}

describe('GCMSRestClient', () => {

    describe('auth.validatePasswordToken', () => {

        // The server writes every request line, query string included, into its access log (GPU-2776)
        it('sends the token of a password link in the body, not in the URL', () => {
            const TOKEN = 'eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI0MiJ9.c2lnbmF0dXJl';
            const captured: CapturedRequest[] = [];
            const client = new GCMSRestClient(createCapturingDriver(captured));

            client.auth.validatePasswordToken(TOKEN);

            expect(captured).toHaveLength(1);
            const { request, body } = captured[0];
            expect(request.method).toEqual(RequestMethod.POST);
            expect(request.url).toEqual('/rest/auth/password/validate');
            expect(request.url).not.toContain(TOKEN);
            expect(Object.values(request.params)).not.toContain(TOKEN);
            expect(JSON.parse(body as string)).toEqual({ token: TOKEN });
        });
    });
});
