import { Injectable } from '@angular/core';
import { Feature } from '@gentics/cms-models';
import { InitOptions, loadSpace, WidgetApi } from '@usersnap/browser';
import { SpaceEventCallback, SpaceEventName } from '@usersnap/browser/dist/types';
import { forkJoin, Subscription } from 'rxjs';
import { filter, first, switchMap } from 'rxjs/operators';
import { InitializableServiceBase } from '../../../shared/providers/initializable-service-base';
import { AppStateService } from '../../../state';
import { AdminOperations } from '../operations/admin/admin.operations';

/**
 * Describes the Usersnap API.
 *
 * There does not seem to be an official typing for this. Since this is third-party code
 * we cannot ship this in @gentics/cms-models.
 */
interface UsersnapApi {
    init: (params?: InitOptions) => Promise<void>;
    logEvent: (eventName: string) => Promise<void>;
    show: (apiKey: string) => Promise<WidgetApi>;
    hide: (apiKey: string) => Promise<void>;
    destroy: () => Promise<void>;
    on: (eventName: SpaceEventName, callback: SpaceEventCallback) => void;
    off: (eventName: SpaceEventName, callback: SpaceEventCallback) => void;
}

const DEFAULT_CONFIG: InitOptions = {
    // use native api for supporting browsers
    nativeScreenshot: navigator.mediaDevices && typeof navigator.mediaDevices.getUserMedia === 'function',
    collectGeoLocation: 'none',
    custom: {},
    user: {},
};

@Injectable()
export class UsersnapService extends InitializableServiceBase {

    private usersnapApi: UsersnapApi;
    private subscription: Subscription;

    constructor(
        private appState: AppStateService,
        private adminOps: AdminOperations,
    ) {
        super();
    }

    destroy(): Promise<any> {
        if (this.subscription != null) {
            this.subscription.unsubscribe();
            this.subscription = null;
        }

        if (!this.usersnapApi) {
            return Promise.resolve();
        }
        return this.usersnapApi.destroy();
    }

    ngOnDestroy(): void {
        this.destroy();
    }

    protected onServiceInit(): void {
        this.loadUsersnapSettingsAndActivateIfEnabled();
    }

    private loadUsersnapSettingsAndActivateIfEnabled(): void {
        const settings$ = this.appState.select(state => state.features.global[Feature.USERSNAP]).pipe(
            filter(active => active),
            switchMap(() => this.adminOps.getUsersnapSettings()),
            switchMap(() => this.appState.select(state => state.ui.usersnap)),
            filter(usersnapSettings => usersnapSettings && !!usersnapSettings.key),
            first(),
        );

        const ui$ = this.appState.select(state => state.ui).pipe(
            filter(ui => !!ui.language
                && !!ui.cmpVersion?.version
                && !!ui.uiVersion,
            ),
            first(),
        );

        const user$ = this.appState.select(state => state.auth.currentUser).pipe(
            filter(user => !!user),
            first(),
        );

        this.subscription = forkJoin([
            settings$,
            ui$,
            user$,
        ]).subscribe(([settings, ui, user]) => {
            console.log('Activating Usersnap');
            loadSpace(settings.key).then(api => {
                const builtConfig: InitOptions = {
                    ...DEFAULT_CONFIG,
                    custom: {
                        ...DEFAULT_CONFIG.custom,
                        language: ui.language,
                        uiVersion: ui.uiVersion,
                        cmsVersion: ui.cmpVersion.version,
                        cmsVariant: ui.cmpVersion.variant,
                        cmpVersion: ui.cmpVersion.cmpVersion,
                        user: {
                            id: user.id,
                            email: user.email,
                            firstName: user.firstName,
                            lastName: user.lastName,
                        },
                    },
                };
                api.init(builtConfig);
            });
        });
    }
}
