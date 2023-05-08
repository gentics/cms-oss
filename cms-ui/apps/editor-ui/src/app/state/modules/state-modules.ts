import { Provider } from '@angular/core';
import { StateClass } from '@ngxs/store/internals';
import { AuthStateModule } from './auth';
import { ContentStagingStateModule } from './content-staging';
import { EditorStateModule } from './editor';
import { EntityStateModule } from './entity';
import { FavouritesStateModule } from './favourites';
import { FeaturesStateModule } from './features';
import { FolderStateModule } from './folder';
import { MaintenanceModeStateModule } from './maintenance-mode';
import { MessageStateModule } from './messages';
import { NodeSettingsStateModule } from './node-settings';
import { PublishQueueStateModule } from './publish-queue';
import { ToolsStateModule } from './tools';
import { UIStateModule } from './ui';
import { UsageStateModule } from './usage';
import { WastebinStateModule } from './wastebin';

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
