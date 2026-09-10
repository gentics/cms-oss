import { Response as GCMSResponse, ResponseCode } from '@gentics/cms-models';
import { GCMSRestClientAbortError, GCMSRestClientRequestError } from '../errors';
import { RequestMethod } from '../models';
import { GCMSFetchDriver } from './fetch-driver';

/* Safe and restore the original fetch implementation in the runs */

let originalFetch: typeof global.fetch;

beforeEach(() => {
    originalFetch = global.fetch;
});

afterEach(() => {
    global.fetch = originalFetch;
});

it('should execute a created request only once, and return the same value', async () => {
    const driver = new GCMSFetchDriver();
    const REQUEST_URL = 'http://localhost:8080/rest/nowhere';
    const STATUS_MSG = 'Ok';
    const STATUS_CODE = 200;
    const RESPONSE_DATA: GCMSResponse = {
        responseInfo: {
            responseCode: ResponseCode.OK,
            responseMessage: 'Success',
        },
        messages: [],
    };

    let execCounter = 0;

    global.fetch = vitest.fn(() => {
        execCounter++;

        return Promise.resolve<Partial<Response>>({
            status: STATUS_CODE,
            statusText: STATUS_MSG,
            ok: STATUS_CODE < 400,
            headers: new Headers(),
            text: () => Promise.resolve(JSON.stringify(RESPONSE_DATA)),
            json: () => Promise.resolve(RESPONSE_DATA),
        });
    }) as any;

    const req = driver.performMappedRequest({
        headers: {},
        method: RequestMethod.GET,
        url: REQUEST_URL,
        params: {},
    });

    const [res1, res2, res3] = await Promise.all([
        req.send(),
        req.send(),
        req.send(),
    ]);

    expect(execCounter).toEqual(1);
    expect(res1).toBe(res2);
    expect(res1).toBe(res3);
    expect(res2).toBe(res3);
});

it('should return a proper error on an error response', async () => {
    const driver = new GCMSFetchDriver();
    const REQUEST_URL = 'http://localhost:8080/rest/nowhere';
    const STATUS_MSG = 'Invalid';
    const STATUS_CODE = 400;
    const RESPONSE_DATA: GCMSResponse = {
        responseInfo: {
            responseCode: ResponseCode.INVALID_DATA,
            responseMessage: 'Invalid Data sent',
        },
        messages: [],
    };

    global.fetch = vitest.fn(() => {
        return Promise.resolve<Partial<Response>>({
            status: STATUS_CODE,
            statusText: STATUS_MSG,
            ok: STATUS_CODE < 400,
            headers: new Headers(),
            text: () => Promise.resolve(JSON.stringify(RESPONSE_DATA)),
            json: () => Promise.resolve(RESPONSE_DATA),
        });
    }) as any;

    await expect(function () {
        return driver.performMappedRequest<GCMSResponse>({
            headers: {},
            method: RequestMethod.GET,
            url: REQUEST_URL,
            params: {},
        }).send();
    }).rejects.toThrowErrorMatchingInlineSnapshot('[Error: Request "GET http://localhost:8080/rest/nowhere" responded with error code 400: "Invalid"]');
});

/*
     * Sometimes some older Endpoints will *always* return a 200, where we have to find out
     * that the `responseInfo` actually tells us otherwise.
     * The driver/client should properly detect this and give us a proper error response.
     */
it('should return a proper error on a HTTP success response', async () => {
    const driver = new GCMSFetchDriver();
    const REQUEST_URL = 'http://localhost:8080/rest/nowhere';
    const STATUS_MSG = 'OK';
    const STATUS_CODE = 200;
    const RESPONSE_DATA: GCMSResponse = {
        responseInfo: {
            responseCode: ResponseCode.INVALID_DATA,
            responseMessage: 'Invalid Data sent',
        },
        messages: [],
    };

    global.fetch = vitest.fn(() => {
        return Promise.resolve<Partial<Response>>({
            status: STATUS_CODE,
            statusText: STATUS_MSG,
            ok: STATUS_CODE < 400,
            headers: new Headers(),
            text: () => Promise.resolve(JSON.stringify(RESPONSE_DATA)),
            json: () => Promise.resolve(RESPONSE_DATA),
        });
    }) as any;

    await expect(function () {
        return driver.performMappedRequest<GCMSResponse>({
            headers: {},
            method: RequestMethod.GET,
            url: REQUEST_URL,
            params: {},
        }).send();
    }).rejects.toThrowErrorMatchingInlineSnapshot('[Error: Unexpected error while parsing response-data from "GET http://localhost:8080/rest/nowhere"]');
});

it('should cancel the request when told to do so', async () => {
    const driver = new GCMSFetchDriver();
    const REQUEST_URL = 'http://localhost:8080/rest/nowhere';
    const STATUS_MSG = 'Ok';
    const STATUS_CODE = 200;
    const RESPONSE_DATA: GCMSResponse = {
        responseInfo: {
            responseCode: ResponseCode.OK,
            responseMessage: 'Success',
        },
        messages: [],
    };

    global.fetch = vitest.fn((args) => {
        if (typeof args === 'string') {
            args = { url: args };
        } else if (args instanceof URL) {
            args = { url: args.toString() };
        }
        const signal = (args as RequestInit).signal;

        return new Promise<Partial<Response>>((resolve, reject) => {
            let aborted = false;

            setTimeout(() => {
                if (aborted) {
                    return;
                }

                resolve({
                    status: STATUS_CODE,
                    statusText: STATUS_MSG,
                    ok: STATUS_CODE < 400,
                    headers: new Headers(),
                    text: () => Promise.resolve(JSON.stringify(RESPONSE_DATA)),
                    json: () => Promise.resolve(RESPONSE_DATA),
                });
            }, 1_000);

            signal.addEventListener('abort', () => {
                aborted = true; // 🤘
                reject(signal.reason);
            });
        });
    }) as any;

    const req = driver.performMappedRequest({
        headers: {},
        method: RequestMethod.GET,
        url: REQUEST_URL,
        params: {},
    });

    const res = req.send();

    req.cancel();

    try {
        await res;
        expect.fail('Should not resolve!');
    } catch (err) {
        // Don't use `toBeInstanceOf`, doesn't work!
        expect(err instanceof GCMSRestClientAbortError).toEqual(true);
    }
});
