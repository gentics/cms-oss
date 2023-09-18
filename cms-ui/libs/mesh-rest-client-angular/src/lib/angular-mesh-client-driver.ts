import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { GenericErrorResponse } from '@gentics/mesh-models';
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
        try {
            const res = await this.http.request(method, url, {
                body,
                headers,
                observe: 'response',
                responseType: 'text',
            }).toPromise();

            if (res.ok) {
                return JSON.parse(res.body);
            }

            throw new HttpErrorResponse({
                headers: new HttpHeaders(headers),
                status: res.status,
                statusText: res.statusText,
                error: res.body,
                url: url,
            });
        } catch (err) {
            if (!(err instanceof HttpErrorResponse)) {
                throw err;
            }

            let raw: string;
            let parsed: GenericErrorResponse;
            let bodyError: Error;

            try {
                raw = err.error;
                try {
                    parsed = JSON.parse(raw);
                } catch (err) {
                    bodyError = err;
                }
            } catch (err) {
                bodyError = err;
            }

            throw new RequestFailedError(
                `Request "${method} ${url}" responded with error code ${err.status}: "${err.statusText}"`,
                method,
                url,
                err.status,
                raw,
                parsed,
                bodyError,
            );
        }

    }

}
