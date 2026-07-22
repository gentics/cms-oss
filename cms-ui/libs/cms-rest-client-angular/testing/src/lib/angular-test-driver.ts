import { GCMSRestClientRequestData } from '@gentics/cms-rest-client';
import { NGGCMSRestClientRequest } from '@gentics/cms-rest-client-angular';
import { GCMSClientTestDriver, TestRequestData } from '@gentics/cms-rest-client/testing';
import { from } from 'rxjs';

export class AngularTestDriver extends GCMSClientTestDriver {

    protected override handleRequest(request: GCMSRestClientRequestData, body?: any): NGGCMSRestClientRequest<any> {
        const testReq: TestRequestData = {
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
