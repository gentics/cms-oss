import { ExposableEmbeddedToolAPI } from '../../../../../embedded-tools-api/exposable-embedded-tool-api';
import { Subscription } from 'rxjs';

export interface TabbedTool {
    key: string;
    window: Window;
    api: ExposableEmbeddedToolAPI;
    subscription: Subscription;
    close(): void;
    focus(): void;
}
