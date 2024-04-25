import { GCMSRestClientRequestData } from '../models';

export interface TestRequest extends GCMSRestClientRequestData {
    body?: any;
}

export type Responder = (request: TestRequest) => Promise<any>;
