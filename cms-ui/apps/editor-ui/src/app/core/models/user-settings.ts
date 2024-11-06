import { DisplayFields } from '@editor-ui/app/common/models';
import { GcmsUiLanguage } from '@gentics/cms-integration-api-models';
import { Favourite, SortField } from '@gentics/cms-models';
import { FALLBACK_LANGUAGE } from '../../common/config/config';
import { SETTING_LAST_NODE_ID } from '../../common/models';

export interface UserSettings {
    activeLanguage: number;
    activeFormLanguage: number;
    contentFrameBreadcrumbsExpanded: boolean;
    displayAllLanguages: boolean;
    displayDeleted: boolean;
    displayStatusIcons: boolean;
    displayImagesGridView: boolean;
    favourites: Favourite[];
    fileDisplayFields: string[];
    fileDisplayFieldsRepositoryBrowser: DisplayFields;
    fileItemsPerPage: number;
    fileShowPath: boolean;
    fileSorting: { sortBy: SortField, sortOrder: 'asc' | 'desc' };
    folderDisplayFields: string[];
    folderDisplayFieldsRepositoryBrowser: DisplayFields;
    folderItemsPerPage: number;
    folderSorting: { sortBy: SortField, sortOrder: 'asc' | 'desc' };
    formDisplayFields: string[];
    formDisplayFieldsRepositoryBrowser: DisplayFields;
    formItemsPerPage: number;
    formSorting: { sortBy: SortField, sortOrder: 'asc' | 'desc' };
    imageDisplayFields: string[];
    imageDisplayFieldsRepositoryBrowser: DisplayFields;
    imageItemsPerPage: number;
    imageShowPath: boolean;
    imageSorting: { sortBy: SortField, sortOrder: 'asc' | 'desc' };
    itemListBreadcrumbsExpanded: boolean;
    [SETTING_LAST_NODE_ID]: number;
    openObjectPropertyGroups: string[];
    focusMode: boolean;
    pageDisplayFields: string[];
    pageDisplayFieldsRepositoryBrowser: DisplayFields;
    pageDisplayFieldsPublishQueue: string[];
    pageItemsPerPage: number;
    pageShowPath: boolean;
    pageSorting: { sortBy: SortField, sortOrder: 'asc' | 'desc' };
    repositoryBrowserBreadcrumbsExpanded: boolean;
    uiLanguage: GcmsUiLanguage;
    constructFavourites: string[];
}

export type UserSettingName = keyof UserSettings;

export const defaultUserSettings: UserSettings = {
    activeLanguage: 1,
    activeFormLanguage: 1,
    contentFrameBreadcrumbsExpanded: false,
    displayAllLanguages: false,
    displayDeleted: false,
    displayStatusIcons: false,
    displayImagesGridView: true,
    favourites: [],
    fileDisplayFields: [],
    fileDisplayFieldsRepositoryBrowser: { selection: [], showPath: true },
    fileItemsPerPage: 10,
    fileShowPath: true,
    fileSorting: { sortBy: 'name', sortOrder: 'asc' },
    folderDisplayFields: [],
    folderDisplayFieldsRepositoryBrowser: { selection: [], showPath: false },
    folderItemsPerPage: 10,
    folderSorting: { sortBy: 'name', sortOrder: 'asc' },
    formDisplayFields: [],
    formDisplayFieldsRepositoryBrowser: { selection: [], showPath: false },
    formItemsPerPage: 10,
    formSorting: { sortBy: 'name', sortOrder: 'asc' },
    imageDisplayFields: [],
    imageDisplayFieldsRepositoryBrowser: { selection: [], showPath: true },
    imageItemsPerPage: 10,
    imageShowPath: true,
    imageSorting: { sortBy: 'name', sortOrder: 'asc' },
    itemListBreadcrumbsExpanded: false,
    [SETTING_LAST_NODE_ID]: -1,
    openObjectPropertyGroups: [],
    focusMode: false,
    pageDisplayFields: [],
    pageDisplayFieldsRepositoryBrowser: { selection: [], showPath: true },
    pageDisplayFieldsPublishQueue: [],
    pageItemsPerPage: 10,
    pageShowPath: true,
    pageSorting: { sortBy: 'name', sortOrder: 'asc' },
    repositoryBrowserBreadcrumbsExpanded: false,
    uiLanguage: FALLBACK_LANGUAGE,
    constructFavourites: [],
};

export const userSettingNames = Object.keys(defaultUserSettings) as UserSettingName[];
