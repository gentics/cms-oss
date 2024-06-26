import type { GCMSClientDriver, GCMSRestClientRequest, GCMSRestClientRequestData } from '@gentics/cms-rest-client';

export class CypressDriver implements GCMSClientDriver {

    protected prepareRequest<T>(
        request: GCMSRestClientRequestData,
        fn: (fullUrl: string) => Partial<Cypress.RequestOptions>,
        handler: (res: Cypress.Response<any>) => Promise<T>,
    ): GCMSRestClientRequest<T> {
        let fullUrl = request.url;
        if (request.params) {
            const q = new URLSearchParams();

            Object.entries(request.params).forEach(([key, value]) => {
                if (Array.isArray(value)) {
                    value.forEach(v => q.append(key, v));
                } else {
                    q.append(key, value);
                }
            });

            const params = q.toString();
            if (params) {
                fullUrl += `?${params}`;
            }
        }

        let sentRequest: Promise<T> | null = null;

        const sendRequest = () => {
            if (sentRequest != null) {
                return sentRequest;
            }

            sentRequest = new Promise((resolve, reject) => {
                cy.request({
                    log: false,
                    url: fullUrl,
                    method: request.method,
                    headers: request.headers,
                    ...fn(fullUrl),
                })
                    .then(res => handler(res)
                        .then(value => resolve(value))
                        .catch(err => reject(err)),
                    );
            });

            return sentRequest;
        }

        return {
            send: () => sendRequest(),
            cancel: () => {
                // Cypress-Requests can't be canceled
            },
        };
    }

    performMappedRequest<T>(
        request: GCMSRestClientRequestData,
        body?: string | FormData | null | undefined,
    ): GCMSRestClientRequest<T> {
        return this.prepareRequest(request, () => ({
            body: body,
        }), (res) => {
            if (typeof res.body === 'string') {
                return Promise.resolve(JSON.parse(res.body));
            }
            return Promise.resolve(res.body);
        });
    }

    performRawRequest(
        request: GCMSRestClientRequestData,
        body?: string | FormData | null | undefined,
    ): GCMSRestClientRequest<string> {
        return this.prepareRequest(request, () => ({
            body: body,
        }), (res) => Promise.resolve(res.body));
    }

    performDownloadRequest(
        request: GCMSRestClientRequestData,
        body?: string | FormData | null | undefined,
    ): GCMSRestClientRequest<Blob> {
        return this.prepareRequest(request, () => ({
            body: body,
            encoding: 'binary',
        }), (res) => Promise.resolve(res.body));
    }
}
