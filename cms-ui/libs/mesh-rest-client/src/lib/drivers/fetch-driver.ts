import { RequestFailedError } from '../errors';
import { MeshClientDriver, RequestMethod } from '../models';

export class MeshFetchDriver implements MeshClientDriver {

    constructor() {}

    async performJsonRequest(
        method: RequestMethod,
        url: string,
        headers?: Record<string, string>,
        body?: null | string,
    ): Promise<Record<string, any>> {
        const res = await fetch({
            method,
            url,
            headers: headers,
            body: body,
        } as any);

        if (res.ok) {
            return res.json();
        }

        let raw: string;
        let parsed: Record<string, any>;
        let bodyError: Error;

        try {
            raw = await res.text();
            try {
                parsed = JSON.parse(raw);
            } catch (err) {
                bodyError = err;
            }
        } catch (err) {
            bodyError = err;
        }

        throw new RequestFailedError(
            `Request "${method} ${url}" responded with error code ${res.status}: "${res.statusText}"`,
            method,
            url,
            res.status,
            raw,
            parsed,
            bodyError,
        );
    }

}
