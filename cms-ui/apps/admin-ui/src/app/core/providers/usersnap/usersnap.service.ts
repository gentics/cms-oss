import { Injectable, OnDestroy } from '@angular/core';
import { Feature, User, UsersnapSettings } from '@gentics/cms-models';
import { InitOptions, loadSpace, SpaceApi } from '@usersnap/browser';
import { forkJoin, Subscription } from 'rxjs';
import { filter, first, switchMap } from 'rxjs/operators';
import { InitializableServiceBase } from '../../../shared/providers/initializable-service-base';
import { AppStateService, UIStateModel } from '../../../state';
import { AdminOperations } from '../operations/admin/admin.operations';

const DEFAULT_CONFIG: InitOptions = {
    // use native api for supporting browsers
    nativeScreenshot: navigator.mediaDevices && typeof navigator.mediaDevices.getUserMedia === 'function',
    collectGeoLocation: 'none',
    custom: {},
    user: {},
};

/** TODO: Move this to the cms-components as own entrypoint */
@Injectable()
export class UsersnapService extends InitializableServiceBase implements OnDestroy {

    private space: SpaceApi;
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

        if (!this.space) {
            return Promise.resolve();
        }
        return this.space.destroy();
    }

    ngOnDestroy(): void {
        this.destroy();
    }

    protected onServiceInit(): void {
        this.loadUsersnapSettingsAndActivateIfEnabled();
    }

    protected activateUsersnap(
        settings: UsersnapSettings,
        ui: UIStateModel,
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
        const settings$ = this.appState.select((state) => state.features.global[Feature.USERSNAP]).pipe(
            filter((active) => active),
            switchMap(() => this.adminOps.getUsersnapSettings()),
            switchMap(() => this.appState.select((state) => state.ui.usersnap)),
            filter((usersnapSettings) => usersnapSettings && !!usersnapSettings.key),
            first(),
        );

        const ui$ = this.appState.select((state) => state.ui).pipe(
            filter((ui) => !!ui.language
              && !!ui.cmpVersion?.version,
            ),
            first(),
        );

        const user$ = this.appState.select((state) => state.auth.user).pipe(
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
