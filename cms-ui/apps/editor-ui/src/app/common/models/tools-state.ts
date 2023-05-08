import { EmbeddedTool } from '@gentics/cms-models';

export interface ToolsState {
    active: string[];
    available: EmbeddedTool[];
    breadcrumbs: { [toolKey: string]: { text: string, url: string }[] };
    fetching: boolean;
    received: boolean;
    subpath: { [toolKey: string]: string };
    visible: string | undefined;
}
