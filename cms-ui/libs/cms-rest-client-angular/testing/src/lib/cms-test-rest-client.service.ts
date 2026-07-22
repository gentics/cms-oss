import { Injectable } from '@angular/core';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { DEFAULT_RESPONDER, Responder, TestRequestData } from '@gentics/cms-rest-client/testing';
import { AngularTestDriver } from './angular-test-driver';

/* eslint-disable @typescript-eslint/no-unsafe-call */

@Injectable()
export class GCMSTestRestClientService extends GCMSRestClientService {

    protected declare driver: AngularTestDriver;

    constructor() {
        // Bit hacky, but it works, as we don't really need it and it gets overwritten in an instant
        super(null);
        this.driver = new AngularTestDriver();
        this.client.driver = this.driver;
        this.client.config = {
            connection: {
                absolute: false,
                basePath: '/rest',
            },
        };
    }

    reset(): void {
        this.driver.clearCalls();
        this.driver.setResponse(DEFAULT_RESPONDER);
    }

    clearCalls(): void {
        this.driver.clearCalls();
    }

    calls(): TestRequestData[] {
        return this.driver.getCalls();
    }

    setResponder(responderOrValue: Responder): void {
        this.driver.setResponse(responderOrValue);
    }
}
