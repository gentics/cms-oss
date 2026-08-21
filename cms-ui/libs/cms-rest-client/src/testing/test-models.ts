import type { GCMSRestClientRequestData } from '../lib/models';

export interface TestRequestData extends GCMSRestClientRequestData {
    body?: any;
}

export type Responder = (request: TestRequestData) => Promise<any>;
