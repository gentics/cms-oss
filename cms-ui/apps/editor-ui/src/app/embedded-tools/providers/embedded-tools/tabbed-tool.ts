import { ExposableEmbeddedToolAPI } from '@gentics/cms-integration-api-models';
import { Subscription } from 'rxjs';

export interface TabbedTool {
    key: string;
    window: Window;
    api: ExposableEmbeddedToolAPI;
    subscription: Subscription;
    close(): void;
    focus(): void;
}
