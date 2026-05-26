import { Injectable, OnDestroy } from '@angular/core';
import { ExposableEmbeddedToolAPI } from '@gentics/cms-integration-api-models';
import { ReplaySubject, Subscription } from 'rxjs';

import { ToolApi } from './tool-api';

/**
 * Angular wrapper around `ToolApi` — establishes the message-channel handshake
 * with the surrounding Gentics CMS UI.
 *
 * The protocol is pull-based: the UI polls `hasUnsavedChanges()` on the tool
 * when it wants to decide whether to show a "discard?" prompt.
 *
 * Mirrors the implementation in `ct-link-checker`. The reviewer asked for both
 * copies to be consolidated into `@gentics/cms-components`; tracked as follow-up.
 */
@Injectable({ providedIn: 'root' })
export class ToolApiService implements OnDestroy {

    api: ToolApi | undefined;
    readonly connected = new ReplaySubject<ToolApi>(1);
    readonly notConnected = new ReplaySubject<void>(1);

    private subscriptions = new Subscription();

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }

    /**
     * Connects to the embedding UI. Optionally pass an `exposedApi` so the UI
     * can pull state (e.g. `hasUnsavedChanges`) from the tool when needed.
     */
    initialize(exposedApi: ExposableEmbeddedToolAPI = {}): void {
        ToolApi
            .connect(exposedApi)
            .then(api => {
                this.api = api;
                this.connected.next(api);
            })
            .catch(err => {
                this.api = undefined;
                // eslint-disable-next-line no-console
                (console.info || console.log).call(console, 'ToolApi failed to initialize: ', err);
                this.notConnected.next(undefined);
            });
    }
}
