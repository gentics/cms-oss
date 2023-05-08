import { InitializableServiceBase } from '@admin-ui/shared/providers/initializable-service-base';
import {
    AppState,
    AppStateService,
    checkStateReduxDevtoolsEnabledForProd,
    disableStateReduxDevtoolsForProd,
    enableStateReduxDevtoolsForProd,
} from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { IndexByKey } from '@gentics/cms-models';
import { INGXLoggerConfig, NGXLogger, NgxLoggerLevel } from 'ngx-logger';

const LOGGING_LEVELS_MAP: IndexByKey<NgxLoggerLevel> = {
    TRACE: NgxLoggerLevel.TRACE,
    DEBUG: NgxLoggerLevel.DEBUG,
    INFO: NgxLoggerLevel.INFO,
    LOG: NgxLoggerLevel.LOG,
    WARN: NgxLoggerLevel.WARN,
    ERROR: NgxLoggerLevel.ERROR,
    FATAL: NgxLoggerLevel.FATAL,
    OFF: NgxLoggerLevel.OFF,
};

/**
 * Provides access to debug information from the browser console.
 */
export interface GcmsAdminUiDebug {

    getState(): AppState;

    enableStateReduxDevtoolsOnProd(): void;
    disableStateReduxDevtoolsOnProd(): void;
    getStateReduxDevtoolsOnProdEnabled(): boolean;

    getLoggerConfig(): INGXLoggerConfig;
    setLoggingLevel(level: NgxLoggerLevel | string): INGXLoggerConfig;

}

declare const window: Window & {
    GcmsAdminUiDebug: GcmsAdminUiDebug;
};

/**
 * Helper service for exposing logging tools to the browser console.
 */
@Injectable()
export class LoggingHelperService extends InitializableServiceBase {

    constructor(
        private appState: AppStateService,
        private logger: NGXLogger,
    ) {
        super();
    }

    protected onServiceInit(): void {
        window.GcmsAdminUiDebug = this.createDebugObject();
    }

    /**
     * Creates the GcmsAdminUiDebug object that can be used from the browser console.
     *
     * We cannot simply use a class for this and instantiate it, because the browser
     * would allow accessing private properties.
     */
    private createDebugObject(): GcmsAdminUiDebug {
        return {
            getState: () => this.appState.snapshot(),

            enableStateReduxDevtoolsOnProd: () => {
                enableStateReduxDevtoolsForProd();
                console.log('AppState Redux Devtools will be enabled on next tab reload.');
                console.log('For information on how to use them see https://www.ngxs.io/plugins/devtools');
            },
            disableStateReduxDevtoolsOnProd: () => {
                disableStateReduxDevtoolsForProd();
                console.log('AppState Redux Devtools will be disabled on next tab reload.');
            },
            getStateReduxDevtoolsOnProdEnabled: () => {
                return checkStateReduxDevtoolsEnabledForProd();
            },

            getLoggerConfig: () => {
                return this.logger.getConfigSnapshot();
            },
            setLoggingLevel: (level: NgxLoggerLevel | string) => {
                if (typeof level === 'string') {
                    level = level.toUpperCase();
                    level = LOGGING_LEVELS_MAP[level];
                }
                const config = this.logger.getConfigSnapshot();
                config.level = level;
                this.logger.updateConfig(config);
                return this.logger.getConfigSnapshot();
            },
        };
    }

}
