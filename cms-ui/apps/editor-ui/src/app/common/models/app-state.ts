import { AuthState } from './auth-state';
import { ContentStagingState } from './content-staging-state';
import { EditorState } from './editor-state';
import { EntityState } from './entity-state';
import { FavouritesState } from './favourites-state';
import { FeaturesState } from './features-state';
import { FolderState } from './folder-state';
import { MaintenanceModeState } from './maintenance-mode-state';
import { MessageState } from './message-state';
import { NodeSettingsState } from './node-settings-state';
import { PublishQueueState } from './publish-queue-state';
import { ToolsState } from './tools-state';
import { UIState } from './ui-state';
import { UserState } from './user-state';
import { WastebinState } from './wastebin-state';

export interface AppState {
    auth: AuthState;
    contentStaging: ContentStagingState;
    editor: EditorState;
    entities: EntityState;
    favourites: FavouritesState;
    features: FeaturesState;
    folder: FolderState;
    maintenanceMode: MaintenanceModeState;
    messages: MessageState;
    nodeSettings: NodeSettingsState;
    publishQueue: PublishQueueState;
    tools: ToolsState;
    ui: UIState;
    user: UserState;
    wastebin: WastebinState;
}
