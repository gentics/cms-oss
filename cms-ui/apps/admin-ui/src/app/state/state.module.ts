import { NgModule } from '@angular/core';
import { NGXS_DEVTOOLS_OPTIONS, NgxsReduxDevtoolsPluginModule } from '@ngxs/devtools-plugin';
import { NgxsModule } from '@ngxs/store';
import { EntityStateModule } from './entity/entity.state';
import { FeaturesStateModule } from './features/features.state';
import { LoadingStateModule } from './loading/loading.state';
import { MaintenanceModeStateModule } from './maintenance-mode/maintenance-mode.state';
import { MessageStateModule } from './messages/message.state';
import { PermissionsStateModule } from './permissions/permissions.state';
import { AppStateService } from './providers/app-state/app-state.service';
import { assembleReduxDevToolsConfig, OPTIONS_CONFIG } from './state-store.config';
import { UIStateModule } from './ui/ui.state';

/** Contains all ngxs state modules. */
export const STATE_MODULES = [
    EntityStateModule,
    FeaturesStateModule,
    LoadingStateModule,
    MaintenanceModeStateModule,
    MessageStateModule,
    PermissionsStateModule,
    UIStateModule,
];

@NgModule({
    imports: [
        NgxsModule.forRoot(STATE_MODULES, OPTIONS_CONFIG),
        NgxsReduxDevtoolsPluginModule.forRoot(),

        // The logger produces too much output (using the Redux Devtools Chrome Extension is better).
        // If you want automatic state logging, uncomment the following line.
        // NgxsLoggerPluginModule.forRoot(LOGGER_CONFIG)
    ],
    providers: [
        AppStateService,

        // We need to provide a custom factory for the devtools config, such that we can enable/disable the plugin
        // based on the local storage config. We cannot simply assemble a static object that does this evaluation
        // and pass it to NgxsReduxDevtoolsPluginModule.forRoot(), because Angular transforms its own decorators
        // during AOT compilation and it does not allow function calls inside them.
        { provide: NGXS_DEVTOOLS_OPTIONS, useFactory: assembleReduxDevToolsConfig },
    ],
    exports: [NgxsModule],
})
export class StateModule {}
