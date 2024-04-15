import { Inject, Injectable } from '@angular/core';
import { RecentItem, plural } from '@editor-ui/app/common/models';
import { Favourite, GcmsUiLanguage, ItemInNode, ItemType, SortField } from '@gentics/cms-models';
import { isEqual, merge } from 'lodash-es';
import { Observable, forkJoin } from 'rxjs';
import { filter, map, take } from 'rxjs/operators';
import { deepEqual } from '../../../common/utils/deep-equal';
import { environment as ENVIRONMENT_TOKEN } from '../../../development/development-tools';
import {
    ApplicationStateService,
    FavouritesLoadedAction,
    FolderActionsService,
    ItemFetchingSuccessAction,
    PublishQueueActionsService,
    RecentItemsFetchingSuccessAction,
    SetActiveFolderAction,
    SetActiveNodeAction,
    SetConstructFavourites,
    SetOpenObjectPropertyGroupsAction,
    SetUILanguageAction,
    UIActionsService,
} from '../../../state';
import { UserSettings, defaultUserSettings, userSettingNames } from '../../models';
import { ErrorHandler } from '../error-handler/error-handler.service';
import { I18nNotification } from '../i18n-notification/i18n-notification.service';
import { I18nService } from '../i18n/i18n.service';
import { LocalStorage } from '../local-storage/local-storage.service';
import { ServerStorage } from '../server-storage/server-storage.service';
import { Version } from './version.class';

/**
 * A service that stores and retreives the settings of the current user and updates the app state.
 */
@Injectable()
export class UserSettingsService {

    private currentUserId: number;

    constructor(
        private appState: ApplicationStateService,
        private folderActions: FolderActionsService,
        private publishQueueActions: PublishQueueActionsService,
        @Inject(ENVIRONMENT_TOKEN) private environment: string,
        private uiActions: UIActionsService,
        private i18nService: I18nService,
        private notification: I18nNotification,
        private localStorage: LocalStorage,
        private serverStorage: ServerStorage,
        private errorHandler: ErrorHandler,
    ) { }

    /**
     * Initializes the application state from the localStorage before a user logs in.
     * Sets the user language.
     */
    loadInitialSettings(): void {
        let uiLanguage = this.localStorage.getUiLanguage();
        if (!uiLanguage) {
            uiLanguage = this.i18nService.inferUserLanguage();
        }
        // preset data to prevent application being without ui language until loaded
        this.appState.dispatch(new SetUILanguageAction(uiLanguage));
        this.i18nService.setLanguage(uiLanguage);
    }

    /**
     * Watches the app state and loads user settings when a user logs in.
     */
    loadUserSettingsWhenLoggedIn(): void {
        this.appState.select(state => state.auth.currentUserId).pipe(
            filter(id => id != null),
        ).subscribe(userId => {
            this.currentUserId = userId;
            this.loadUserSettings();
        });
    }

    watchForSettingChangesInOtherTabs(): void {
        this.localStorage.change$.pipe(
            filter(change =>
                this.currentUserId &&
                change.key.startsWith(`USER-${this.currentUserId}_`),
            ),
        ).subscribe(change => {
            const name = change.key.replace(/^USER-\d+_/, '');
            if (name === 'sid') {
                // nothing ... yet
            } else {
                this.dispatchChangedSetting(name, change.newValue);
            }
        });
    }

    saveRecentItemsOnUpdate(): void {
        this.appState.select(state => state.folder.recentItems).pipe(
            filter(recentItems => recentItems && recentItems.length > 0),
            filter(() => this.appState.now.auth.isLoggedIn),
        ).subscribe(recentItems => {
            const savedInLocalStorage = this.localStorage.getForUser(this.currentUserId, 'recentItems');
            if (!deepEqual(savedInLocalStorage, recentItems)) {
                this.localStorage.setForUser(this.currentUserId, 'recentItems', recentItems);
            }
        });
    }

    private loadUserSettings(): void {
        for (const setting of userSettingNames) {
            let value = this.localStorage.getForUser(this.currentUserId, setting);
            if (value == null) {
                value = defaultUserSettings[setting];
            }
            this.dispatchChangedSetting(setting, value);
        }

        const lastNodeidFromLocalStorage = this.localStorage.getForUser(this.currentUserId, 'lastNodeId');

        this.serverStorage.getAll().subscribe(settings => {
            // If last node is set in local storage and/or server settings, fall back on default node if all last nodes are invalid
            const invalidNodeInLocalStorage$ = this.appState.select(state => state.entities.node[lastNodeidFromLocalStorage]).pipe(
                map(node => !node),
                take(1),
            )
            const invalidNodeInServerSettings$ = this.appState.select(state => state.entities.node[settings.lastNodeId]).pipe(
                map(node => !node),
                take(1),
            );

            forkJoin({
                invalidNodeInLocalStorage: invalidNodeInLocalStorage$,
                invalidNodeInServerSettings: invalidNodeInServerSettings$,
            }).subscribe(({ invalidNodeInLocalStorage, invalidNodeInServerSettings }) => {
                if (invalidNodeInLocalStorage && invalidNodeInServerSettings) {
                    this.navigateToFallbackNode();
                }
            });

            if (this.serverStorage.supported === false) {
                return;
            }

            const firstLoginOfUser = !Object.keys(settings).length;
            if (firstLoginOfUser) {
                // fetch actual ui language data to override serverstorage data
                this.initializeUiLanguages();
                return;
            }

            const currentSettings = this.getSettingsFromAppState();

            if (!settings['lastSaveUiVersion']) {
                this.serverStorage.set('lastSaveUiVersion', this.appState.now.ui.uiVersion);
            } else {
                this.migrateSettings();
            }

            // Update language if not stored on server yet
            if (Object.keys(settings).indexOf('uiLanguage') === -1) {
                this.setUiLanguage(this.appState.now.ui.language);
            }

            for (const key of Object.keys(settings)) {
                if (userSettingNames.indexOf(key as any) === -1
                    || settings[key] == null
                    || deepEqual(settings[key], (<any> currentSettings)[key])
                ) {
                    continue;
                }

                // A setting on the server is different than the current setting in the app state
                this.dispatchChangedSetting(key, settings[key]);

                // Save setting to localStorage as well
                this.localStorage.setForUser(this.currentUserId, key, settings[key]);
                if (key !== 'favourites') {
                    this.localStorage.setForAllUsers(key, settings[key]);
                }
            }

            // fetch actual ui language data to override serverstorage data
            this.initializeUiLanguages();
        }, error => {
            this.handleError(error);
        });

        const recentItems: RecentItem[] = this.localStorage.getForUser(this.currentUserId, 'recentItems');
        this.appState.dispatch(new RecentItemsFetchingSuccessAction(recentItems));
    }

    /**
     * UI language used to be hardcoded and is now available via `i18n`endpoint.
     * This method fetches all available and current active UI language and stores it to state and localstorage.
     * In case fetching fails, fallback language logic should be in place by localstorage and browser language.
     *
     * @see `loadInitialSettings()`
     */
    private initializeUiLanguages(): void {
        this.appState.select(state => state.ui.language).subscribe(language => {
            this.setUiLanguage(language);
        });
        this.uiActions.getAvailableUiLanguages();
        this.uiActions.getActiveUiLanguage();
    }

    /**
     * Gets all user settings from the application state
     */
    getSettingsFromAppState(): UserSettings {
        const state = this.appState.now;

        const result: UserSettings = {
            activeLanguage: state.folder.activeLanguage,
            activeFormLanguage: state.folder.activeFormLanguage,
            contentFrameBreadcrumbsExpanded: state.ui.contentFrameBreadcrumbsExpanded,
            displayAllLanguages: state.folder.displayAllLanguages,
            displayStatusIcons: state.folder.displayStatusIcons,
            displayImagesGridView: state.folder.displayImagesGridView,
            displayDeleted: state.folder.displayDeleted,
            favourites: state.favourites.list,

            fileShowPath: state.folder.files.showPath,
            fileDisplayFields: state.folder.files.displayFields,
            fileDisplayFieldsRepositoryBrowser: state.folder.files.displayFieldsRepositoryBrowser,
            fileItemsPerPage: state.folder.files.itemsPerPage,
            fileSorting: { sortBy: state.folder.files.sortBy, sortOrder: state.folder.files.sortOrder },

            folderDisplayFields: state.folder.folders.displayFields,
            folderDisplayFieldsRepositoryBrowser: state.folder.folders.displayFieldsRepositoryBrowser,
            folderItemsPerPage: state.folder.folders.itemsPerPage,
            folderSorting: { sortBy: state.folder.folders.sortBy, sortOrder: state.folder.folders.sortOrder },

            formDisplayFields: state.folder.forms.displayFields,
            formDisplayFieldsRepositoryBrowser: state.folder.forms.displayFieldsRepositoryBrowser,
            formItemsPerPage: state.folder.forms.itemsPerPage,
            formSorting: { sortBy: state.folder.forms.sortBy, sortOrder: state.folder.forms.sortOrder },

            imageDisplayFields: state.folder.images.displayFields,
            imageDisplayFieldsRepositoryBrowser: state.folder.images.displayFieldsRepositoryBrowser,
            imageItemsPerPage: state.folder.images.itemsPerPage,
            imageSorting: { sortBy: state.folder.images.sortBy, sortOrder: state.folder.images.sortOrder },
            imageShowPath: state.folder.files.showPath,

            itemListBreadcrumbsExpanded: state.ui.itemListBreadcrumbsExpanded,
            lastNodeId: state.folder.activeNode,
            openObjectPropertyGroups: state.editor.openObjectPropertyGroups,
            focusMode: state.editor.focusMode,

            pageDisplayFields: state.folder.pages.displayFields,
            pageDisplayFieldsRepositoryBrowser: state.folder.pages.displayFieldsRepositoryBrowser,
            pageDisplayFieldsPublishQueue: state.publishQueue.pages.displayFields,

            pageItemsPerPage: state.folder.pages.itemsPerPage,
            pageSorting: { sortBy: state.folder.pages.sortBy, sortOrder: state.folder.pages.sortOrder },
            pageShowPath: state.folder.files.showPath,

            repositoryBrowserBreadcrumbsExpanded: state.ui.repositoryBrowserBreadcrumbsExpanded,
            uiLanguage: state.ui.language,
            constructFavourites: state.ui.constructFavourites,
        };
        return result;
    }

    setDisplayFields(itemType: ItemType, displayFields: string[]): void {
        this.dispatchAndSaveChange(`${itemType}DisplayFields`, displayFields);
    }

    setRepositoryBrowserDisplayFields(itemType: ItemType, displayFields: string[]): void {
        this.dispatchAndSaveChange(`${itemType}DisplayFieldsRepositoryBrowser`, displayFields);
    }

    setPublishQueueDisplayFields(itemType: ItemType, displayFields: string[]): void {
        this.dispatchAndSaveChange(`${itemType}DisplayFieldsPublishQueue`, displayFields);
    }

    setShowPath(itemType: ItemType, showPath: boolean): void {
        this.dispatchAndSaveChange(`${itemType}ShowPath`, showPath);
    }

    setSorting(itemType: ItemType, sortBy: SortField, sortOrder: 'asc' | 'desc'): void {
        this.dispatchAndSaveChange(`${itemType}Sorting`, { sortBy, sortOrder });
    }

    setItemsPerPage(itemType: ItemType, itemsPerPage: number): void {
        this.dispatchAndSaveChange(`${itemType}ItemsPerPage`, itemsPerPage);
    }

    setActiveLanguage(languageId: number): void {
        this.dispatchAndSaveChange('activeLanguage', languageId);
    }

    setActiveFormLanguage(languageId: number): void {
        this.dispatchAndSaveChange('activeFormLanguage', languageId);
    }

    setContentFrameBreadcrumbsExpanded(isExpanded: boolean): void {
        this.dispatchAndSaveChange('contentFrameBreadcrumbsExpanded', isExpanded);
    }

    setItemListBreadcrumbsExpanded(isExpanded: boolean): void {
        this.dispatchAndSaveChange('itemListBreadcrumbsExpanded', isExpanded);
    }

    setRepositoryBrowserBreadcrumbsExpanded(isExpanded: boolean): void {
        this.dispatchAndSaveChange('repositoryBrowserBreadcrumbsExpanded', isExpanded);
    }

    setOpenObjectPropertyGroups(openObjectPropertyGroups: string[]): void {
        this.dispatchAndSaveChange('openObjectPropertyGroups', openObjectPropertyGroups);
    }

    setDisplayAllLanguages(all: boolean): void {
        this.dispatchAndSaveChange('displayAllLanguages', all);
    }

    setDisplayStatusIcons(all: boolean): void {
        this.dispatchAndSaveChange('displayStatusIcons', all);
    }

    setDisplayDeleted(all: boolean): void {
        this.dispatchAndSaveChange('displayDeleted', all);
    }

    setDisplayImagesGridView(all: boolean): void {
        this.dispatchAndSaveChange('displayImagesGridView', all);
    }

    setUiLanguage(language: GcmsUiLanguage): void {
        this.dispatchAndSaveChange('uiLanguage', language);
    }

    setConstructFavourites(favourites: string[]): void {
        this.dispatchAndSaveChange('constructFavourites', favourites);
    }

    setLastNodeId(nodeId: number): void {
        this.set('lastNodeId', nodeId);
    }

    saveFavourites(favourites: Favourite[]): void {
        this.set('favourites', favourites);
    }

    private dispatchAndSaveChange(key: string, value: any): void {
        this.dispatchChangedSetting(key, value);
        this.set(key, value);
    }

    private set<T>(key: string, value: T): Promise<void | T> {
        if (this.serverStorage.supported !== false) {
            return this.serverStorage.set(key, value)
                .then(
                    result => {
                        if (this.currentUserId) {
                            this.localStorage.setForUser(this.currentUserId, key, value);
                        }
                        this.localStorage.setForAllUsers(key, value);
                        return result;
                    },
                    err => this.handleError(err),
                );
        } else {
            if (this.currentUserId) {
                this.localStorage.setForUser(this.currentUserId, key, value);
            }
            this.localStorage.setForAllUsers(key, value);
            return Promise.resolve(value);
        }
    }

    // Dispatches actions to the app store when settings are changed
    private dispatchChangedSetting(key: string, value: any): void {
        switch (key) {
            case 'activeLanguage':
                this.folderActions.setActiveLanguage(value);
                break;

            case 'activeFormLanguage':
                this.folderActions.setActiveFormLanguage(value);
                break;

            case 'displayAllLanguages':
                this.folderActions.setDisplayAllPageLanguages(value);
                break;

            case 'displayStatusIcons':
                this.folderActions.setDisplayStatusIcons(value);
                break;

            case 'displayDeleted':
                this.folderActions.setDisplayDeleted(value);
                break;

            case 'displayImagesGridView':
                this.folderActions.setDisplayImagesGridView(value);
                break;

            case 'fileItemsPerPage':
            case 'folderItemsPerPage':
            case 'formItemsPerPage':
            case 'imageItemsPerPage':
            case 'pageItemsPerPage': {
                const itemsPerPageType = key.split('ItemsPerPage')[0] as 'file' | 'folder' | 'form' | 'image' | 'page';
                this.folderActions.setItemsPerPage(itemsPerPageType, value);
                break;
            }

            case 'favourites': {
                const favourites = this.filterAndRemoveNonExistingFavouriteItems(value as Favourite[]);
                favourites.then(filteredFavourites => {
                    this.appState.dispatch(new FavouritesLoadedAction(filteredFavourites));
                });
                break;
            }

            case 'fileDisplayFields':
            case 'folderDisplayFields':
            case 'formDisplayFields':
            case 'imageDisplayFields':
            case 'pageDisplayFields': {
                const displayFieldType = key.split('DisplayFields')[0] as 'file' | 'folder' | 'form' | 'image' | 'page';
                this.folderActions.setDisplayFields(displayFieldType, value);
                break;
            }

            case 'fileDisplayFieldsRepositoryBrowser':
            case 'folderDisplayFieldsRepositoryBrowser':
            case 'formDisplayFieldsRepositoryBrowser':
            case 'imageDisplayFieldsRepositoryBrowser':
            case 'pageDisplayFieldsRepositoryBrowser': {
                const displayFieldTypeRepositoryBrowser = key.split('DisplayFieldsRepositoryBrowser')[0] as 'file' | 'folder' | 'form' | 'image' | 'page';
                this.folderActions.setRepositoryBrowserDisplayFields(displayFieldTypeRepositoryBrowser, value);
                break;
            }

            case 'fileDisplayFieldsPublishQueue':
            case 'folderDisplayFieldsPublishQueue':
            case 'formDisplayFieldsPublishQueue':
            case 'imageDisplayFieldsPublishQueue':
            case 'pageDisplayFieldsPublishQueue': {
                const pageDisplayFieldsPublishQueue = key.split('DisplayFieldsPublishQueue')[0] as 'file' | 'folder' | 'form' | 'image' | 'page';
                this.publishQueueActions.setDisplayFields(pageDisplayFieldsPublishQueue, value);
                break;
            }

            case 'fileShowPath':
            case 'imageShowPath':
            case 'pageShowPath':
            case 'formShowPath': {
                const showPathType = key.split('ShowPath')[0] as 'file' | 'image' | 'page' | 'form';
                this.folderActions.setShowPath(showPathType, value);
                break;
            }

            case 'fileSorting':
            case 'folderSorting':
            case 'formSorting':
            case 'imageSorting':
            case 'pageSorting': {
                const sortingType = key.split('Sorting')[0] as 'file' | 'folder' | 'form' | 'image' | 'page';
                const { sortBy, sortOrder } = value || {};
                this.folderActions.setSorting(sortingType, sortBy, sortOrder);
                break;
            }

            case 'uiLanguage':
                this.uiActions.setActiveUiLanguageInFrontend(value);
                break;

            case 'contentFrameBreadcrumbsExpanded':
                this.uiActions.setContentFrameBreadcrumbsExpanded(value);
                break;

            case 'itemListBreadcrumbsExpanded':
                this.uiActions.setItemListBreadcrumbsExpanded(value);
                break;

            case 'repositoryBrowserBreadcrumbsExpanded':
                this.uiActions.setRepositoryBrowserBreadcrumbsExpanded(value);
                break;

            case 'lastNodeId':
                this.appState.select(state => state.entities.node[value]).pipe(
                    filter(node => !!node),
                    take(1),
                ).subscribe(node => {
                    if (this.appState.now.folder.activeNode == null) {
                        this.appState.dispatch(new SetActiveNodeAction(node.id));
                        this.appState.dispatch(new SetActiveFolderAction(node.folderId));
                    }
                    // TODO: Reenable this?
                    // this.navigationService.list(node.id, node.folderId).navigateIfNotSet();
                });
                break;

            case 'openObjectPropertyGroups':
                this.appState.dispatch(new SetOpenObjectPropertyGroupsAction(value));
                break;

            case 'recentItems':
                if (value) {
                    this.appState.dispatch(new RecentItemsFetchingSuccessAction(value));
                }
                break;

            case 'lastSaveUiVersion':
                break;

            case 'constructFavourites':
                this.appState.dispatch(new SetConstructFavourites(value));
                break;

            default:
                if (this.environment === 'development') {
                    // eslint-disable-next-line no-console
                    console.info(`UserSettings: Setting "${key}" not handled.`);
                }
        }
    }

    private migrateSettings(): void {
        const currentVersion = Version.parse(this.appState.now.ui.uiVersion);
        const storedVersion = Version.parse(this.localStorage.getVersionOfLastSave());
        if (storedVersion.isEqualTo(currentVersion)) {
            return;
        }

        if (storedVersion.isNewerThan(currentVersion)) {
            // Local storage data might be corrupt :-/
            return;
        }

        // Could migrate settings here
        //

        this.set('lastSaveUiVersion', this.appState.now.ui.uiVersion);
    }

    private navigateToFallbackNode(): void {
        this.appState.select(state => state.folder.nodes.list).pipe(
            filter(nodes => nodes.length > 0),
            filter(() => !this.appState.now.folder.activeNode),
            take(1),
        ).subscribe(() => {
            this.folderActions.navigateToDefaultNode();
        });
    }

    /*
     * Fix for SUP-7125 (but it should be done server side, which is currently not possible because of the
     * structure how it saved).
     */
    private filterAndRemoveNonExistingFavouriteItems(items: Favourite[]): Promise<Favourite[]> {
        let grouppedFavs = {} as any;
        const itemRequests: Observable<any>[] = [];

        /* Create groups of favorites to group requests together */
        items.forEach((fav) => {
            const favObj = {} as any;
            favObj[fav.nodeId] = {};
            favObj[fav.nodeId][fav.type] = {};
            favObj[fav.nodeId][fav.type][fav.id] = fav;
            grouppedFavs = merge(favObj, grouppedFavs);
        });

        /* Make the requests per group to check if items are exists */
        Object.keys(grouppedFavs).forEach(nodeId => {
            Object.keys(grouppedFavs[nodeId]).forEach(type => {
                const ids = Object.values(grouppedFavs[nodeId][type])
                    .map((fav: Favourite) => fav.id);
                itemRequests.push(this.folderActions.getExistingItems(ids, parseInt(nodeId, 10), type as ItemType));
            });
        });

        const existingList$ = forkJoin(itemRequests).toPromise();

        /* Filter the original favourites list by nodeId and id then return as a Promise */
        return existingList$.then(checkItems => {
            if (!checkItems) {
                return [];
            }

            const existingItems = checkItems.flatMap((item: ItemInNode[]) =>
                // eslint-disable-next-line @typescript-eslint/no-unsafe-call
                item.map((existingItem: any) => {
                    if ((existingItem).type === 'channel') {
                        existingItem.type = 'folder';
                    }

                    // Load existing items as an entity to the appstate
                    this.appState.dispatch(new ItemFetchingSuccessAction(plural[existingItem.type], existingItem));

                    return {
                        // Get actual nodeId if its a master, channel or localized version
                        nodeId: !existingItem.channelId ?
                            // eslint-disable-next-line no-underscore-dangle
                            (existingItem.inherited ? existingItem._checkedNodeId : existingItem.masterNodeId) :
                            existingItem.channelId,
                        type: existingItem.type,
                        id: existingItem.id,
                    };
                }),
            );

            /* Clone the original favourites list and filter existing items
             * also follow moved items if data returned by the CMS with new nodeId */
            const filteredItems = structuredClone(items).filter((fav) => {
                const existingItem = existingItems.find((item: any) => {
                    // For an item to match, its ID, type, and nodeId have to match
                    // that of the favorite. However, a node's root folder is returned by the CMS as
                    // type 'node', so we have to account for that exception.
                    return item.id === fav.id && (
                        item.type === fav.type
                        || (item.type === 'node' && fav.type === 'folder')
                    ) && item.nodeId === fav.nodeId;
                });

                if (existingItem) {
                    fav.nodeId = existingItem.nodeId;
                }

                return existingItem;
            });

            /* Remove non-existing items from favourites */
            if (!isEqual(items, filteredItems)) {
                this.saveFavourites(filteredItems);
            }

            return filteredItems;
        });
    }

    private handleError(err: Error): void {
        this.errorHandler.catch(err, { notification: false });
        this.notification.show({
            message: 'message.settings_save_error_other',
            type: 'alert',
        });
    }
}
