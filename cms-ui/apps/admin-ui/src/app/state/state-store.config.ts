import { NgxsDevtoolsOptions } from '@ngxs/devtools-plugin';
import { NgxsLoggerPluginOptions } from '@ngxs/logger-plugin';
import { NgxsConfig } from '@ngxs/store';

import { environment } from '../../environments/environment';
import { checkStateReduxDevtoolsEnabledForProd } from './utils';

export const OPTIONS_CONFIG: Partial<NgxsConfig> = {
    /**
     * Run in development mode. This will add additional debugging features:
     * - Object.freeze on the state and actions to guarantee immutability
     */
    developmentMode: !environment.production,
};

export function assembleReduxDevToolsConfig(): NgxsDevtoolsOptions {
    return {
        name: 'NGXS',

        /**
         * Whether the dev tools are enabled or not. Useful for setting during production.
         */
        disabled: environment.production && !checkStateReduxDevtoolsEnabledForProd(),
    };
}

export const LOGGER_CONFIG: NgxsLoggerPluginOptions = {
    /**
     * Disable the logger. Useful for prod mode..
     */
    disabled: environment.production,
};
