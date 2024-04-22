import { EditableEntity } from '@admin-ui/common';
import { GcmsUiLanguage } from '@gentics/cms-integration-api-models';
import { EntityIdType, NormalizableEntityType, Update, UsersnapSettings, Version } from '@gentics/cms-models';
import { AppState } from '../app-state';
import { ActionDeclaration } from '../utils/state-utils';
import type { UIStateSettings } from './ui.state';

const UI: keyof AppState = 'ui';

@ActionDeclaration(UI)
export class SetUIVersion {
    static readonly type = 'SetUIVersion';
    constructor(public version: string) {}
}

@ActionDeclaration(UI)
export class SetCmpVersion {
    static readonly type = 'SetCmpVersion';
    constructor(public version: Version) {}
}

@ActionDeclaration(UI)
export class SetBackendLanguage {
    static readonly type = 'SetBackendLanguage';
    constructor(public backendLanguage: GcmsUiLanguage) {}
}

@ActionDeclaration(UI)
export class SetCmsUpdates {
    static readonly type = 'SetCmsUpdates';
    constructor(public available: Update[]) {}
}

@ActionDeclaration(UI)
export class SetUILanguage {
    static readonly type = 'SetUILanguage';
    constructor(public language: GcmsUiLanguage) {}
}

@ActionDeclaration(UI)
export class SetUISettings {
    static readonly type = 'SetUISettings';
    constructor(public settings: UIStateSettings) {}
}

@ActionDeclaration(UI)
export class OpenEditor {
    static readonly type = 'OpenEditor';
    constructor() {}
}

@ActionDeclaration(UI)
export class CloseEditor {
    static readonly type = 'CloseEditor';
    constructor() {}
}

@ActionDeclaration(UI)
export class FocusEditor {
    static readonly type = 'FocusEditor';
    constructor() {}
}

@ActionDeclaration(UI)
export class FocusList {
    static readonly type = 'FocusList';
    constructor() {}
}

@ActionDeclaration(UI)
export class EnableFocusMode {
    static readonly type = 'EnableFocusMode';
    constructor() {}
}

@ActionDeclaration(UI)
export class DisableFocusMode {
    static readonly type = 'DisableFocusMode';
    constructor() {}
}

@ActionDeclaration(UI)
export class SetUIFocusEntity {
    static readonly type = 'SetUIFocusEntity';
    constructor(
        public focusEntityType: NormalizableEntityType | EditableEntity,
        public focusEntityId: EntityIdType,
        public focusEntityNodeId?: number,
    ) {}
}

@ActionDeclaration(UI)
export class SetUsersnapSettings {
    static readonly type = 'SetUsersnapSettings';
    constructor(public settings: UsersnapSettings) {}
}

@ActionDeclaration(UI)
export class SwitchEditorTab {
    static readonly type = 'SwitchEditorTab';
    constructor(public tabId: string) {}
}

@ActionDeclaration(UI)
export class SetUserSettingAction<T extends keyof UIStateSettings> {
    static readonly type = 'SetUserSettingAction';
    constructor(
        public setting: T,
        public value: UIStateSettings[T],
    ) {}
}
