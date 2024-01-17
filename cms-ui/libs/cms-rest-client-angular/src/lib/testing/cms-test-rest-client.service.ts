import { Injectable } from '@angular/core';
import { DEFAULT_RESPONDER, Responder, TestRequest } from '@gentics/cms-rest-client/testing';
import { GCMSRestClientService } from '../cms-rest-client.service';
import { AngularTestDriver } from './angular-test-driver';

/* eslint-disable @typescript-eslint/no-unsafe-call */

@Injectable()
export class GCMSTestRestClientService extends GCMSRestClientService {

    protected driver: AngularTestDriver;

    constructor() {
        // Bit hacky, but it works, as we don't really need it and it gets overwritten in an instant
        super(null as any);
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

    calls(): TestRequest[] {
        return this.driver.getCalls();
    }

    setResponder(responderOrValue: Responder | any): void {
        this.driver.setResponse(responderOrValue);
    }
}
