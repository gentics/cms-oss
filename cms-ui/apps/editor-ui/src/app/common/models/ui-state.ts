import { GcmsUiLanguage, GtxVersion, I18nLanguage, UsersnapSettings } from '@gentics/cms-models';
import { UIOverrides } from '../../shared/providers/ui-overrides/ui-overrides.model';

export enum UIMode {
    EDIT = 'edit',
    STAGING = 'staging',
}

export interface Alerts {
    linkChecker?: Alert;
}

export interface Alert {
    [key: string]: number;
}

export interface UIState {
    mode: UIMode;
    alerts: Alerts;
    contentFrameBreadcrumbsExpanded: boolean;
    itemListBreadcrumbsExpanded: boolean;
    repositoryBrowserBreadcrumbsExpanded: boolean;
    backendLanguage: GcmsUiLanguage;
    language: GcmsUiLanguage;
    availableUiLanguages: I18nLanguage[];
    cmpVersion: GtxVersion;
    overrides: UIOverrides;
    overridesReceived: boolean;
    uiVersion: string;
    usersnap: UsersnapSettings;
    hideExtras: boolean;
    overlayCount: number;
    constructFavourites: string[];
    tagEditorOpen: boolean;
}
