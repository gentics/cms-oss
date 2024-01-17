import { GCMSRestClientRequest } from '../models';

export interface TestRequest extends GCMSRestClientRequest {
    body?: any;
}

export type Responder = (request: TestRequest) => Promise<any>;
