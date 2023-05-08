import { NgModule, Provider } from '@angular/core';
import { NgxsReduxDevtoolsPluginModule } from '@ngxs/devtools-plugin';
import { NgxsModule } from '@ngxs/store';
import { STATE_MODULES } from './modules';
import {
    ApplicationStateService,
    AuthActionsService,
    ContentRepositoryActionsService,
    ContentStagingActionsService,
    EditorActionsService,
    FeaturesActionsService,
    FolderActionsService,
    MessageActionsService,
    NodeSettingsActionsService,
    PublishQueueActionsService,
    TemplateActionsService,
    UIActionsService,
    UsageActionsService,
    WastebinActionsService,
} from './providers';
import { OPTIONS_CONFIG } from './state-store.config';

export const STATE_PROVIDERS: Provider[] = [
    ...STATE_MODULES,
    ApplicationStateService,
    AuthActionsService,
    ContentRepositoryActionsService,
    ContentStagingActionsService,
    EditorActionsService,
    FeaturesActionsService,
    FolderActionsService,
    MessageActionsService,
    NodeSettingsActionsService,
    PublishQueueActionsService,
    TemplateActionsService,
    UIActionsService,
    UsageActionsService,
    WastebinActionsService,
];

@NgModule({
    providers: STATE_PROVIDERS,
    imports: [
        NgxsModule.forRoot(STATE_MODULES, OPTIONS_CONFIG),
        NgxsReduxDevtoolsPluginModule.forRoot(),
    ],
})
export class StateModule {}
