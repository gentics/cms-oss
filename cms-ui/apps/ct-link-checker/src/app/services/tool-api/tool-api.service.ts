import { Injectable, OnDestroy } from '@angular/core';
import { ReplaySubject, Subscription } from 'rxjs';
import { ToolApi } from './tool-api';

@Injectable()
export class ToolApiService implements OnDestroy {

    api: ToolApi;
    connected = new ReplaySubject<ToolApi>(1);
    notConnected = new ReplaySubject<void>(1);

    private subscriptions = new Subscription();

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }

    initialize(): void {
        ToolApi
            .connect()
            .then(api => {
                this.api = api;
                this.connected.next(api);
            })
            .catch(err => {
                this.api = undefined;
                // tslint:disable-next-line: no-console
                (console.info || console.log).call(console, 'ToolApi failed to initialize: ', err);
                this.notConnected.next(undefined);
            });
    }

}
