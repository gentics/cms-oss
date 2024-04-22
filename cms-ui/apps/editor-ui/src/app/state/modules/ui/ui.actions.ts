import { UIOverrides } from '@editor-ui/app/shared/providers/ui-overrides/ui-overrides.model';
import { GcmsUiLanguage, GtxVersion, I18nLanguage, UsersnapSettings } from '@gentics/cms-models';
import { AppState, UIMode } from '../../../common/models';
import { ActionDeclaration } from '../../state-utils';

export const UI_STATE_KEY: keyof AppState = 'ui';

export enum BreadcrumbLocation {
    CONTENT_FRAME = 'content-frame',
    ITEM_LIST = 'item-list',
    CONTENT_REPOSITORY = 'content-repository',
}

@ActionDeclaration(UI_STATE_KEY)
export class SetBreadcrumbExpandedAction {
    constructor(
        public location: BreadcrumbLocation,
        public isExpanded: boolean,
    ) {}
}

@ActionDeclaration(UI_STATE_KEY)
export class SetCMPVersionAction {
    constructor(
        public version: GtxVersion,
    ) {}
}

@ActionDeclaration(UI_STATE_KEY)
export class SetUIVersionAction {
    constructor(
        public version: string,
    ) {}
}

@ActionDeclaration(UI_STATE_KEY)
export class SetUIOverridesAction {
    constructor(
        public overrides: UIOverrides,
    ) {}
}

@ActionDeclaration(UI_STATE_KEY)
export class SetBackendLanguageAction {
    constructor(
        public language: GcmsUiLanguage,
    ) {}
}

@ActionDeclaration(UI_STATE_KEY)
export class SetUILanguageAction {
    constructor(
        public language: GcmsUiLanguage,
    ) {}
}

@ActionDeclaration(UI_STATE_KEY)
export class SetAvailableUILanguageAction {
    constructor(
        public languages: I18nLanguage[],
    ) {}
}

@ActionDeclaration(UI_STATE_KEY)
export class SetBrokenLinksCountAction {
    constructor(
        public count: number,
    ) {}
}

@ActionDeclaration(UI_STATE_KEY)
export class SetUsersnapSettingsAction {
    constructor(
        public settings: UsersnapSettings,
    ) {}
}

@ActionDeclaration(UI_STATE_KEY)
export class SetHideExtrasAction {
    constructor(
        public doHide: boolean,
    ) {}
}

@ActionDeclaration(UI_STATE_KEY)
export class SetUIModeAction {
    constructor(
        public mode: UIMode,
    ) {}
}

@ActionDeclaration(UI_STATE_KEY)
export class SetConstructFavourites {
    constructor(
        public favourites: string[],
    ) {}
}

@ActionDeclaration(UI_STATE_KEY)
export class IncreaseOverlayCountAction {}

@ActionDeclaration(UI_STATE_KEY)
export class DecreaseOverlayCountAction {}

@ActionDeclaration(UI_STATE_KEY)
export class ResetOverlayCountAction {}

@ActionDeclaration(UI_STATE_KEY)
export class SetTagEditorOpenAction {
    constructor(
        public isOpen: boolean,
    ) {}
}

@ActionDeclaration(UI_STATE_KEY)
export class SetNodesLoadedAction {
    constructor(
        public loaded: boolean,
    ) {}
}
