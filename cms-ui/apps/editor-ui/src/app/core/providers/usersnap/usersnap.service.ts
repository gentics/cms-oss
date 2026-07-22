import { Injectable, OnDestroy } from '@angular/core';
import { User, UsersnapSettings } from '@gentics/cms-models';
import { InitOptions, loadSpace, SpaceApi } from '@usersnap/browser';
import { combineLatest, forkJoin, Subscription } from 'rxjs';
import { debounceTime, filter, first, map, switchMap, tap } from 'rxjs/operators';
import { UIState } from '../../../common/models';
import { ApplicationStateService, UIActionsService } from '../../../state';

const DEFAULT_CONFIG: InitOptions = {
    // use native api for supporting browsers
    nativeScreenshot: navigator.mediaDevices && typeof navigator.mediaDevices.getUserMedia === 'function',
    collectGeoLocation: 'none',
    custom: {},
    user: {},
};

/** TODO: Move this to the cms-components as own entrypoint */
@Injectable()
export class UsersnapService implements OnDestroy {

    private space: SpaceApi;

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

        if (!this.space) {
            return Promise.resolve();
        }
        return this.space.destroy();
    }

    ngOnDestroy(): void {
        this.destroy();
    }

    protected activateUsersnap(
        settings: UsersnapSettings,
        ui: UIState,
        user: User,
    ): void {
        loadSpace(settings.key).then((api) => {
            const builtConfig: InitOptions = {
                ...DEFAULT_CONFIG,
                custom: {
                    ...DEFAULT_CONFIG.custom,
                    language: ui.language,
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
    }

    private loadUsersnapSettingsAndActivateIfEnabled(): void {
        const settings$ = combineLatest([
            this.appState.select((state) => state.features.usersnap),
            this.appState.select((state) => state.ui.hideExtras),
        ]).pipe(
            // Debounce/Delay init by 3 seconds
            debounceTime(3_000),
            filter(([active, hideExtras]) => active && !hideExtras),
            tap(() => this.uiActions.getUsersnapSettings()),
            switchMap(() => this.appState.select((state) => state.ui.usersnap)),
            filter((usersnapSettings) => usersnapSettings && !!usersnapSettings.key),
            first(),
        );

        const ui$ = this.appState.select((state) => state.ui).pipe(
            filter((ui) => !!ui.language
              && !!ui.cmpVersion?.version
              && !!ui.uiVersion,
            ),
            first(),
        );

        const user$ = this.appState.select((state) => state.auth).pipe(
            map((auth) => auth.user),
            filter((user) => !!user),
            first(),
        );

        this.subscription = forkJoin([
            settings$,
            ui$,
            user$,
        ]).subscribe(([settings, ui, user]) => {
            this.activateUsersnap(settings, ui, user);
        });
    }
}
