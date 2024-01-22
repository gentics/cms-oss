
import { GCMSRestClientRequest } from '@gentics/cms-rest-client';
import { TestDriver, TestRequest } from '@gentics/cms-rest-client/testing';
import { from } from 'rxjs';
import { NGGCMSRestClientResponse } from '../models';

export class AngularTestDriver extends TestDriver {

    protected override handleRequest(request: GCMSRestClientRequest, body?: any): NGGCMSRestClientResponse<any> {
        const testReq: TestRequest = {
            ...request,
            body,
        };

        this.calls.push(testReq);

        return {
            cancel: () => {},
            send: () => this.responder(testReq),
            rx: () => from(this.responder(testReq)),
        };
    }
}
