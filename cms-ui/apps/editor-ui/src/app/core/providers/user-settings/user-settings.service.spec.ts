import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ENVIRONMENT_TOKEN } from '@editor-ui/app/development/development-tools';
import { getExamplePageData } from '@gentics/cms-models/testing/test-data.mock';
import { NgxsModule } from '@ngxs/store';
import { NEVER, of } from 'rxjs';
import { first } from 'rxjs/operators';
import { ApplicationStateService, FolderActionsService, PublishQueueActionsService, STATE_MODULES, UIActionsService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { defaultUserSettings } from '../../models';
import { ErrorHandler } from '../error-handler/error-handler.service';
import { I18nNotification as NotificationService } from '../i18n-notification/i18n-notification.service';
import { I18nService } from '../i18n/i18n.service';
import { LocalStorage } from '../local-storage/local-storage.service';
import { ServerStorage } from '../server-storage/server-storage.service';
import { UserSettingsService } from './user-settings.service';

class MockFolderActions {
    navigateToDefaultNode = jasmine.createSpy('navigateToDefaultNode');
    setActiveLanguage = jasmine.createSpy('setActiveLanguage');
    setActiveFormLanguage = jasmine.createSpy('setActiveFormLanguage');
    setSorting = jasmine.createSpy('setSorting');
    setDisplayFields = jasmine.createSpy('setDisplayFields');
    setDisplayDeleted = jasmine.createSpy('setDisplayDeleted');
    setItemsPerPage = jasmine.createSpy('setItemsPerPage');
    setRepositoryBrowserDisplayFields = jasmine.createSpy('setRepositoryBrowserDisplayFields');
    setDisplayAllPageLanguages = jasmine.createSpy('setDisplayAllPageLanguages');
    setDisplayStatusIcons = jasmine.createSpy('setDisplayStatusIcons');
    setDisplayImagesGridView = jasmine.createSpy('setDisplayImagesGridView');
    setShowPath = jasmine.createSpy('setShowPath');
    getExistingItems = jasmine.createSpy('getExistingItems');
}

class MockPublishQueueActions {
    setDisplayFields = jasmine.createSpy('setDisplayFields');
}

class MockUIActions {
    setActiveUiLanguageInFrontend = jasmine.createSpy('setActiveUiLanguageInFrontend');
    setContentFrameBreadcrumbsExpanded = jasmine.createSpy('setContentFrameBreadcrumbsExpanded');
    setItemListBreadcrumbsExpanded = jasmine.createSpy('setItemListBreadcrumbsExpanded');
    setRepositoryBrowserBreadcrumbsExpanded = jasmine.createSpy('setRepositoryBrowserBreadcrumbsExpanded');
    getAvailableUiLanguages = jasmine.createSpy('getAvailableUiLanguages');
    getActiveUiLanguage = jasmine.createSpy('getActiveUiLanguage');
}

class MockI18nService {
    inferUserLanguage = jasmine.createSpy('inferUserLanguage').and.returnValue('inferred language');
    setLanguage = jasmine.createSpy('setLanguage');
}

class MockNotificationService {

}

class MockLocalStorage {
    getUiLanguage = jasmine.createSpy('getUiLanguage');
    getForUser = jasmine.createSpy('getForUser');
    getForAllUsers = jasmine.createSpy('getForAllUsers');
    setForAllUsers = jasmine.createSpy('setForAllUsers');
    setForUser = jasmine.createSpy('setForUser');
}

class MockServerStorage {
    getAll = jasmine.createSpy('getAll');
    set = jasmine.createSpy('set').and.callFake(() => Promise.resolve());
}

class MockErrorHandler {

}

describe('UserSettingsService', () => {

    let userSettings: UserSettingsService;
    let state: TestApplicationState;
    let folderActions: MockFolderActions;
    let publishQueueActions: MockPublishQueueActions;
    let uiActions: MockUIActions;
    let i18nService: MockI18nService;
    let localStorage: MockLocalStorage;
    let serverStorage: MockServerStorage;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: PublishQueueActionsService, useClass: MockPublishQueueActions },
                { provide: ENVIRONMENT_TOKEN, useValue: 'testing' },
                { provide: UIActionsService, useClass: MockUIActions },
                { provide: I18nService, useClass: MockI18nService },
                { provide: NotificationService, useClass: MockNotificationService },
                { provide: LocalStorage, useClass: MockLocalStorage },
                { provide: ServerStorage, useClass: MockServerStorage },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                UserSettingsService,
            ],
        });

        state = TestBed.inject(ApplicationStateService) as TestApplicationState;
        userSettings = TestBed.inject(UserSettingsService);
        folderActions = TestBed.inject(FolderActionsService) as any;
        publishQueueActions = TestBed.inject(PublishQueueActionsService) as any;
        uiActions = TestBed.inject(UIActionsService) as any;
        i18nService = TestBed.inject(I18nService) as any;
        localStorage = TestBed.inject(LocalStorage) as any;
        serverStorage = TestBed.inject(ServerStorage) as any;
    });

    it('gets created okay', () => {
        expect(userSettings).toBeDefined();
    });

    describe('loadInitialSettings()', () => {

        it('sets the user language from localStorage if available', () => {
            localStorage.getUiLanguage.and.returnValue('testLanguage');
            userSettings.loadInitialSettings();
            expect(i18nService.setLanguage).toHaveBeenCalledWith('testLanguage');
            expect(i18nService.inferUserLanguage).not.toHaveBeenCalled();
        });

        it('infers the user language if not stored in localStorage', () => {
            i18nService.inferUserLanguage.and.returnValue('defaultLanguage');
            localStorage.getUiLanguage.and.returnValue(null);
            userSettings.loadInitialSettings();
            expect(i18nService.inferUserLanguage).toHaveBeenCalled();
            expect(i18nService.setLanguage).toHaveBeenCalledWith('defaultLanguage');
        });

    });

    describe('loadUserSettingsWhenLoggedIn', () => {

        it('waits until the app state signals a logged-in user', () => {
            userSettings.loadUserSettingsWhenLoggedIn();
            expect(localStorage.getForUser).not.toHaveBeenCalled();
        });

        it('loads settings from localStorage and serverStorage when a user logs in', () => {
            state.mockState({
                auth: {
                    currentUserId: null,
                    loggingIn: false,
                },
                ui: {
                    nodesLoaded: true,
                },
            });
            serverStorage.getAll.and.returnValue(NEVER);
            userSettings.loadUserSettingsWhenLoggedIn();
            expect(localStorage.getForUser).not.toHaveBeenCalled();

            state.mockState({ auth: { currentUserId: 1234, loggingIn: true } });
            expect(localStorage.getForUser).toHaveBeenCalledWith(1234, jasmine.anything());
            expect(serverStorage.getAll).toHaveBeenCalled();
        });

        it('dispatches any settings in the localStorage to the application state', () => {
            state.mockState({
                auth: {
                    currentUserId: 1234,
                    isLoggedIn: true,
                },
                ui: {
                    nodesLoaded: true,
                },
            });
            serverStorage.getAll.and.returnValue(NEVER);
            localStorage.getForUser.and.callFake((userId: number, key: string): any => {
                if (key === 'activeLanguage') { return 'testLanguage'; }
                if (key === 'folderSorting') { return { sortBy: 'cdate', sortOrder: 'desc' }; }
                if (key === 'fileDisplayFields') { return ['id', 'cdate', 'filename']; }
            });

            userSettings.loadUserSettingsWhenLoggedIn();
            expect(folderActions.setActiveLanguage).toHaveBeenCalledWith('testLanguage');
            expect(folderActions.setSorting).toHaveBeenCalledWith('folder', 'cdate', 'desc');
            expect(folderActions.setDisplayFields).toHaveBeenCalledWith('file', ['id', 'cdate', 'filename']);
        });

        it('dispatches default values for settings not in the localStorage to the application state', fakeAsync(() => {
            state.mockState({
                auth: {
                    currentUserId: 1234,
                    isLoggedIn: true,
                },
                ui: {
                    nodesLoaded: true,
                },
            });

            const testServerStorage = {
                uiLanguage: 'de',
                recentItems: [ getExamplePageData() ],
                favourites: [ getExamplePageData() ],
            };
            serverStorage.getAll.and.returnValue(of(testServerStorage).pipe(first()));
            localStorage.getForUser.and.callFake((userId: number, key: string): any => {
                switch (key) {
                    case 'recentItems':
                        return testServerStorage.recentItems;
                    case 'favourites':
                        return testServerStorage.favourites;
                    default:
                        return null;
                }
            });
            folderActions.getExistingItems.and.returnValue(of(testServerStorage.favourites).pipe(first()));

            userSettings.loadUserSettingsWhenLoggedIn();
            tick();
            expect(folderActions.setActiveLanguage).toHaveBeenCalledWith(defaultUserSettings.activeLanguage);
            expect(folderActions.setRepositoryBrowserDisplayFields).toHaveBeenCalledWith('page', defaultUserSettings.pageDisplayFieldsRepositoryBrowser);

            for (const actions of [ folderActions, uiActions, publishQueueActions ]) {
                const expectedCalledActionsSpies = Object.keys(actions).filter(key => key !== 'navigateToDefaultNode');
                expectedCalledActionsSpies.forEach(key => {
                    const spy: jasmine.Spy = actions[key];
                    expect(spy).toHaveBeenCalled();
                });
            }
        }));

        it('re-loads all settings from localStorage and serverStorage when a different user logs in', () => {
            state.mockState({
                auth: {
                    currentUserId: null,
                    loggingIn: false,
                },
                ui: {
                    nodesLoaded: true,
                },
            });
            serverStorage.getAll.and.returnValue(NEVER);
            localStorage.getForUser.and.callFake((userId: number, key: string): any => {
                if (userId === 1234 && key === 'uiLanguage') { return 'language of first user'; }
                if (userId === 9876 && key === 'uiLanguage') { return 'language of second user'; }
            });

            userSettings.loadUserSettingsWhenLoggedIn();
            expect(uiActions.setActiveUiLanguageInFrontend).not.toHaveBeenCalled();

            state.mockState({ auth: { currentUserId: 1234, isLoggedIn: true } });
            expect(uiActions.setActiveUiLanguageInFrontend).toHaveBeenCalledTimes(1);
            expect(uiActions.setActiveUiLanguageInFrontend).toHaveBeenCalledWith('language of first user');
            expect(serverStorage.getAll).toHaveBeenCalledTimes(1);

            state.mockState({ auth: { currentUserId: null, isLoggedIn: false } });
            expect(uiActions.setActiveUiLanguageInFrontend).toHaveBeenCalledTimes(1);
            expect(serverStorage.getAll).toHaveBeenCalledTimes(1);

            state.mockState({ auth: { currentUserId: 9876, isLoggedIn: true } });
            expect(uiActions.setActiveUiLanguageInFrontend).toHaveBeenCalledTimes(2);
            expect(uiActions.setActiveUiLanguageInFrontend).toHaveBeenCalledWith('language of second user');
            expect(serverStorage.getAll).toHaveBeenCalledTimes(2);
        });

        describe('navigation to the fallback node', () => {

            it('occurs if neither the local storage nor the server settings contain a lastNodeId', () => {
                userSettings.loadUserSettingsWhenLoggedIn();
                localStorage.getForUser.and.callFake((userId: number, key: string): any => {
                    if (key === 'lastNodeId') { return undefined; }
                });
                serverStorage.getAll.and.returnValue(of({}));

                state.mockState({
                    auth: {
                        currentUserId: 1234,
                        isLoggedIn: true,
                    },
                    ui: {
                        nodesLoaded: true,
                    },
                    folder: {
                        nodes: {
                            list: [ 1, 2, 3, 4 ],
                        },
                        files: {
                            displayFields: [],
                        },
                        folders: {
                            displayFields: [],
                        },
                        forms: {
                            displayFields: [],
                        },
                        images: {
                            displayFields: [],
                        },
                        pages: {
                            displayFields: [],
                        },
                    },
                    entities: {
                        node: {
                            1: {
                                name: 'Node 1',
                                id: 1,
                            },
                            2: {
                                name: 'Node 2',
                                id: 2,
                            },
                            3: {
                                name: 'Node 3',
                                id: 3,
                            },
                            4: {
                                name: 'Node 4',
                                id: 4,
                            },
                        },
                    },
                });

                expect(folderActions.navigateToDefaultNode).toHaveBeenCalledTimes(1);
            });

            it('does not occur if the local storage contains a valid lastNodeId', () => {
                userSettings.loadUserSettingsWhenLoggedIn();
                localStorage.getForUser.and.callFake((userId: number, key: string): any => {
                    if (key === 'lastNodeId') { return 2; }
                });
                serverStorage.getAll.and.returnValue(of({}));

                state.mockState({
                    auth: {
                        currentUserId: 1234,
                        isLoggedIn: true,
                    },
                    ui: {
                        nodesLoaded: true,
                    },
                    folder: {
                        nodes: {
                            list: [ 1, 2, 3, 4 ],
                        },
                        files: {
                            displayFields: [],
                        },
                        folders: {
                            displayFields: [],
                        },
                        forms: {
                            displayFields: [],
                        },
                        images: {
                            displayFields: [],
                        },
                        pages: {
                            displayFields: [],
                        },
                    },
                    entities: {
                        node: {
                            1: {
                                name: 'Node 1',
                                id: 1,
                            },
                            2: {
                                name: 'Node 2',
                                id: 2,
                            },
                            3: {
                                name: 'Node 3',
                                id: 3,
                            },
                            4: {
                                name: 'Node 4',
                                id: 4,
                            },
                        },
                    },
                });

                expect(folderActions.navigateToDefaultNode).not.toHaveBeenCalled();
            });

            it('does not occur if the server settings contain a valid lastNodeId', () => {
                userSettings.loadUserSettingsWhenLoggedIn();
                localStorage.getForUser.and.callFake((userId: number, key: string): any => {
                    if (key === 'lastNodeId') { return undefined; }
                });
                serverStorage.getAll.and.returnValue(of({
                    lastNodeId: 2,
                }));

                state.mockState({
                    auth: {
                        currentUserId: 1234,
                        isLoggedIn: true,
                    },
                    ui: {
                        nodesLoaded: true,
                    },
                    folder: {
                        activeNode: 2,
                        nodes: {
                            list: [ 1, 2, 3, 4 ],
                        },
                        files: {
                            displayFields: [],
                        },
                        folders: {
                            displayFields: [],
                        },
                        forms: {
                            displayFields: [],
                        },
                        images: {
                            displayFields: [],
                        },
                        pages: {
                            displayFields: [],
                        },
                    },
                    entities: {
                        node: {
                            1: {
                                name: 'Node 1',
                                id: 1,
                            },
                            2: {
                                name: 'Node 2',
                                id: 2,
                            },
                            3: {
                                name: 'Node 3',
                                id: 3,
                            },
                            4: {
                                name: 'Node 4',
                                id: 4,
                            },
                        },
                    },
                });

                expect(folderActions.navigateToDefaultNode).not.toHaveBeenCalled();
            });

            it('occurs if the local storage and the server settings both contain an invalid lastNodeId', () => {
                userSettings.loadUserSettingsWhenLoggedIn();
                localStorage.getForUser.and.callFake((userId: number, key: string): any => {
                    if (key === 'lastNodeId') { return 5; }
                });
                serverStorage.getAll.and.returnValue(of({
                    lastNodeId: 5,
                }));

                state.mockState({
                    auth: {
                        currentUserId: 1234,
                        isLoggedIn: true,
                    },
                    ui: {
                        nodesLoaded: true,
                    },
                    folder: {
                        nodes: {
                            list: [ 1, 2, 3, 4 ],
                        },
                        files: {
                            displayFields: [],
                        },
                        folders: {
                            displayFields: [],
                        },
                        forms: {
                            displayFields: [],
                        },
                        images: {
                            displayFields: [],
                        },
                        pages: {
                            displayFields: [],
                        },
                    },
                    entities: {
                        node: {
                            1: {
                                name: 'Node 1',
                                id: 1,
                            },
                            2: {
                                name: 'Node 2',
                                id: 2,
                            },
                            3: {
                                name: 'Node 3',
                                id: 3,
                            },
                            4: {
                                name: 'Node 4',
                                id: 4,
                            },
                        },
                    },
                });

                expect(folderActions.navigateToDefaultNode).toHaveBeenCalledTimes(1);
            });

        });

    });

});
