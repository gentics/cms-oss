import { Injectable } from '@angular/core';
import { UsersnapSettings } from '@gentics/cms-models';
import { NGXLogger } from 'ngx-logger';
import { filter, switchMap, take, takeUntil } from 'rxjs/operators';
import { InitializableServiceBase } from '../../../shared/providers/initializable-service-base';
import { AppStateService } from '../../../state';
import { AdminOperations } from '../operations/admin/admin.operations';

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
     * Configures the button that is added to the UI.
     * Set this to `null` to create a custom button.
     */
    button?: {
        icon?: string,
        label?: string,
        position?: 'bottomRight' | 'rightCenter' | 'rightBottom' | 'bottomLeft' | 'leftCenter',
    };

    colors?: {

        /**
         * The primary color is the color of the feedback button and the main color of the widget on the header,
         * the button and the frame that marks your screenshot area.
         */
        primary?: string,

        /**
         * The secondary color is only visible on certain dialogs as the second button color.
         */
        secondary?: string,

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
}

const USERSNAP_CONFIG: UsersnapConfig = {
    button: {
        position: 'bottomLeft',
    },
    colors: {
        primary: '#A97BE5',
    },
};

@Injectable()
export class UsersnapService extends InitializableServiceBase {

    private usersnapApi: UsersnapApi;

    constructor(
        private appState: AppStateService,
        private adminOps: AdminOperations,
        private logger: NGXLogger,
    ) {
        super();
    }

    protected onServiceInit(): void {
        this.loadUsersnapSettingsAndActivateIfEnabled();
    }

    protected activateUsersnap(settings: UsersnapSettings): void {
        this.logger.info('Activating Usersnap');
        this.registerUsersnapLoadEventHandler();

        const script = document.createElement('script');
        script.async = true;
        script.src = USERSNAP_URL.replace(KEY_PLACEHOLDER, settings.key);
        document.getElementsByTagName('head')[0].appendChild(script);
    }

    private loadUsersnapSettingsAndActivateIfEnabled(): void {
        this.appState.select(state => state.features.global.usersnap).pipe(
            filter(active => active),
            switchMap(() => this.adminOps.getUsersnapSettings()),
            switchMap(() => this.appState.select(state => state.ui.usersnap)),
            filter(usersnapSettings => usersnapSettings && !!usersnapSettings.key),
            take(1),
            takeUntil(this.stopper.stopper$),
        ).subscribe(settings => this.activateUsersnap(settings));
    }

    private registerUsersnapLoadEventHandler(): void {
        (window as any)[USERSNAP_LOAD_FN] = (api) => {
            api.init(USERSNAP_CONFIG);
            this.usersnapApi = api;
        };
    }

}
