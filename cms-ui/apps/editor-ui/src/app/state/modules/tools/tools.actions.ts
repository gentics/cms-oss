import { EmbeddedTool } from '@gentics/cms-models';
import { AppState } from '../../../common/models';
import { ActionDeclaration } from '../../state-utils';

export const TOOLS_STATE_KEY: keyof AppState = 'tools';

@ActionDeclaration(TOOLS_STATE_KEY)
export class StartToolsFetchingAction {}

@ActionDeclaration(TOOLS_STATE_KEY)
export class ToolsFetchingSuccessAction {
    constructor(
        public tools: EmbeddedTool[],
    ) {}
}

@ActionDeclaration(TOOLS_STATE_KEY)
export class ToolsFetchingErrorAction {}

@ActionDeclaration(TOOLS_STATE_KEY)
export class OpenToolAction {
    constructor(
        public toolKey: string,
        public subpath: string = '',
    ) {}
}

@ActionDeclaration(TOOLS_STATE_KEY)
export class CloseToolAction {
    constructor(
        public toolKey: string,
    ) {}
}

@ActionDeclaration(TOOLS_STATE_KEY)
export class HideToolsAction {}

@ActionDeclaration(TOOLS_STATE_KEY)
export class ToolNavigationAction {
    constructor(
        public toolKey: string,
        public subpath: string,
    ) {}
}

@ActionDeclaration(TOOLS_STATE_KEY)
export class ToolBreadcrumbAction {
    constructor(
        public toolKey: string,
        public breadcrumbs: { text: string, url: string }[],
    ) {}
}
