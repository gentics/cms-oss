import { Provider } from '@angular/core';
import { StateClass } from '@ngxs/store/internals';
import { AuthStateModule } from './auth/auth.state-module';
import { ContentStagingStateModule } from './content-staging/content-staging.state-module';
import { EditorStateModule } from './editor/editor.state-module';
import { EntityStateModule } from './entity/entity.state-module';
import { FavouritesStateModule } from './favourites/favourites.state-module';
import { FeaturesStateModule } from './features/features.state-module';
import { FolderStateModule } from './folder/folder.state-module';
import { MaintenanceModeStateModule } from './maintenance-mode/maintenance-mode.state-module';
import { MessageStateModule } from './messages/message.state-module';
import { NodeSettingsStateModule } from './node-settings/node-settings.state-module';
import { PublishQueueStateModule } from './publish-queue/publish-queue.state-module';
import { ToolsStateModule } from './tools/tools.state-module';
import { UIStateModule } from './ui/ui.state-module';
import { UsageStateModule } from './usage/usage.state-module';
import { WastebinStateModule } from './wastebin/wastebin.state-module';

/** Contains all ngxs state modules. */
export const STATE_MODULES: (StateClass & Provider)[] = [
    AuthStateModule,
    ContentStagingStateModule,
    EditorStateModule,
    EntityStateModule,
    FavouritesStateModule,
    FeaturesStateModule,
    FolderStateModule,
    MaintenanceModeStateModule,
    MessageStateModule,
    NodeSettingsStateModule,
    PublishQueueStateModule,
    ToolsStateModule,
    UIStateModule,
    UsageStateModule,
    WastebinStateModule,
];
