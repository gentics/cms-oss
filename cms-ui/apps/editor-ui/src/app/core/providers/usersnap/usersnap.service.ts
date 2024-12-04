import { Injectable, OnDestroy } from '@angular/core';
import { InitOptions, loadSpace, WidgetApi } from '@usersnap/browser';
import { SpaceEventCallback, SpaceEventName } from '@usersnap/browser/dist/types';
import { combineLatest, forkJoin, Subscription } from 'rxjs';
import { debounceTime, filter, first, switchMap, tap } from 'rxjs/operators';
import { ApplicationStateService, UIActionsService } from '../../../state';

const DEFAULT_CONFIG: InitOptions = {
    // use native api for supporting browsers
    nativeScreenshot: navigator.mediaDevices && typeof navigator.mediaDevices.getUserMedia === 'function',
    collectGeoLocation: 'none',
    custom: {},
    user: {},
};

interface UsersnapApi {
    init: (params?: InitOptions) => Promise<void>;
    logEvent: (eventName: string) => Promise<void>;
    show: (apiKey: string) => Promise<WidgetApi>;
    hide: (apiKey: string) => Promise<void>;
    destroy: () => Promise<void>;
    on: (eventName: SpaceEventName, callback: SpaceEventCallback) => void;
    off: (eventName: SpaceEventName, callback: SpaceEventCallback) => void;
}

@Injectable()
export class UsersnapService implements OnDestroy {

    private usersnapApi: UsersnapApi;

    private subscription: Subscription;

    private initialized = false;

    constructor(
        private appState: ApplicationStateService,
        private uiActions: UIActionsService,
    ) {}

    init(): void {
        if (this.initialized) {
            throw new Error('The UsersnapService has already been initialized');
        }
        this.loadUsersnapSettingsAndActivateIfEnabled();
        this.initialized = true;
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

    private loadUsersnapSettingsAndActivateIfEnabled(): void {
        const settings$ = combineLatest([
            this.appState.select(state => state.features.usersnap),
            this.appState.select(state => state.ui.hideExtras),
        ]).pipe(
            // Debounce/Delay init by 3 seconds
            debounceTime(3_000),
            filter(([active, hideExtras]) => active && !hideExtras),
            tap(() => this.uiActions.getUsersnapSettings()),
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
