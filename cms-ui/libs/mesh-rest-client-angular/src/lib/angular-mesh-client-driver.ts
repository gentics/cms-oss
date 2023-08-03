import { HttpClient } from '@angular/common/http';
import { MeshClientDriver, RequestFailedError, RequestMethod } from '@gentics/mesh-rest-client';

export class AngularMeshClientDriver implements MeshClientDriver {

    constructor(
        private http: HttpClient,
    ) {}

    async performJsonRequest(
        method: RequestMethod,
        url: string,
        headers?: Record<string, string>,
        body?: string,
    ): Promise<Record<string, any>> {
        const res = await this.http.request(method, url, {
            body,
            headers,
            observe: 'response',
            responseType: 'text',
        }).toPromise();

        if (res.ok) {
            return JSON.parse(res.body);
        }

        let raw: string;
        let parsed: Record<string, any>;
        let bodyError: Error;

        try {
            raw = res.body;
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
