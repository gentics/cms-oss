import { Injectable, OnDestroy } from '@angular/core';
import { UsersnapSettings } from '@gentics/cms-models';
import { combineLatest } from 'rxjs';
import { debounceTime, filter, switchMap, take, takeUntil, tap } from 'rxjs/operators';
import { ObservableStopper } from '../../../common/utils/observable-stopper/observable-stopper';
import { ApplicationStateService, UIActionsService } from '../../../state';

const KEY_PLACEHOLDER = '{KEY}';
const USERSNAP_LOAD_FN = 'onUsersnapLoad';
const USERSNAP_URL = `https://widget.usersnap.com/global/load/${KEY_PLACEHOLDER}?onload=${USERSNAP_LOAD_FN}`;

/**
 * Describes a part of the configuration object accepted by UsersnapApi.init().
 *
 * There does not seem to be an official typing for this. Since this is third-party code
 * we cannot ship this in @gentics/cms-models.
 *
 * Some of the descriptions were copied from https://help.usersnap.com/docs/api-for-usersnap-classic-new
 */
interface UsersnapConfig {
    /**
     * Browser screenshots are taken via the browser-in-built "media-record-API".
     * https://help.usersnap.com/docs/feedback-with-a-screenshot#taking-screenshots-without-our-rendering-technology
     */
    nativeScreenshot: boolean;
    /**
     * Configures the button that is added to the UI.
     * Set this to `null` to create a custom button.
     */
    button?: {
        icon?: string;
        label?: string;
        position?:
            | 'bottomRight'
            | 'rightCenter'
            | 'rightBottom'
            | 'bottomLeft'
            | 'leftCenter';
    };

    colors?: {
        /**
         * The primary color is the color of the feedback button and the main color of the widget on the header,
         * the button and the frame that marks your screenshot area.
         */
        primary?: string;

        /**
         * The secondary color is only visible on certain dialogs as the second button color.
         */
        secondary?: string;
    };
}

/**
 * Describes the Usersnap API.
 *
 * There does not seem to be an official typing for this. Since this is third-party code
 * we cannot ship this in @gentics/cms-models.
 */
interface UsersnapApi {
    init(config: UsersnapConfig): void;
    destroy(): Promise<any>;
    hide(): Promise<any>;
    show(): Promise<any>;
}

const USERSNAP_CONFIG: UsersnapConfig = {
    // use native api for supporting browsers
    nativeScreenshot:
        navigator.mediaDevices && navigator.mediaDevices.getUserMedia
            ? true
            : false,
    button: {
        position: 'bottomLeft',
    },
    colors: {
        primary: '#A97BE5',
    },
};

@Injectable()
export class UsersnapService implements OnDestroy {

    private usersnapApi: UsersnapApi;

    private stopper = new ObservableStopper();

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
        if (!this.usersnapApi) {
            return Promise.resolve();
        }
        return this.usersnapApi.destroy();
    }

    ngOnDestroy(): void {
        this.stopper.stop();
        this.destroy();
    }

    protected activateUsersnap(settings: UsersnapSettings): void {
        console.log('Activating Usersnap');
        this.registerUsersnapLoadEventHandler();

        const script = document.createElement('script');
        script.async = true;
        script.src = USERSNAP_URL.replace(KEY_PLACEHOLDER, settings.key);
        document.getElementsByTagName('head')[0].appendChild(script);
    }

    private loadUsersnapSettingsAndActivateIfEnabled(): void {
        combineLatest([
            this.appState.select(state => state.features.usersnap),
            this.appState.select(state => state.ui.hideExtras),
        ]).pipe(
            // Debounce/Delay init by 3 seconds
            debounceTime(3_000),
            filter(([active, hideExtras]) => active && !hideExtras),
            tap(() => this.uiActions.getUsersnapSettings()),
            switchMap(() => this.appState.select(state => state.ui.usersnap)),
            filter(usersnapSettings => usersnapSettings && !!usersnapSettings.key),
            take(1),
            takeUntil(this.stopper.stopper$),
        ).subscribe(settings => {
            this.activateUsersnap(settings);
        });
    }

    private registerUsersnapLoadEventHandler(): void {
        (window as any)[USERSNAP_LOAD_FN] = (api: UsersnapApi) => {
            api.init(USERSNAP_CONFIG);
            this.usersnapApi = api;
        };
    }

}
