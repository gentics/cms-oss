import { GCMSClientDriver, GCMSRestClientRequestData, GCMSRestClientRequest } from '../models';
import { Responder, TestRequest } from './test-models';

export const DEFAULT_RESPONDER: Responder = () => Promise.resolve(null);

export class TestDriver implements GCMSClientDriver {

    protected calls: TestRequest[] = [];
    protected responder: Responder = DEFAULT_RESPONDER;

    constructor() {}

    public getCalls(): TestRequest[] {
        return this.calls.slice();
    }

    public clearCalls(): void {
        this.calls = [];
    }

    public setResponse(responderOrValue: Responder | any): void {
        if (typeof responderOrValue === 'function') {
            this.responder = responderOrValue;
        } else {
            this.responder = () => responderOrValue;
        }
    }

    protected handleRequest(request: GCMSRestClientRequestData, body?: any): GCMSRestClientRequest<any> {
        const testReq: TestRequest = {
            ...request,
            body,
        };

        this.calls.push(testReq);

        return {
            cancel: () => {},
            send: () => this.responder(testReq),
        };
    }

    performMappedRequest<T>(request: GCMSRestClientRequestData, body?: string | FormData): GCMSRestClientRequest<T> {
        return this.handleRequest(request, body);
    }

    performRawRequest(request: GCMSRestClientRequestData, body?: string | FormData): GCMSRestClientRequest<string> {
        return this.handleRequest(request, body);
    }

    performDownloadRequest(request: GCMSRestClientRequestData, body?: string | FormData): GCMSRestClientRequest<Blob> {
        return this.handleRequest(request, body);
    }
}
