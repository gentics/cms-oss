import { HttpClientModule } from '@angular/common/http';
import { Component, EventEmitter, Input } from '@angular/core';
import { TestBed, discardPeriodicTasks, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { CmsComponentsModule, WindowRef } from '@gentics/cms-components';
import { NodeFeature } from '@gentics/cms-models';
import { GenticsUICoreModule, ModalService } from '@gentics/ui-core';
import { LangChangeEvent, TranslatePipe, TranslateService } from '@ngx-translate/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { take } from 'rxjs/operators';
import { componentTest, configureComponentTest } from '../testing';
import { AppComponent as OriginalApp } from './app.component';
import { LoggingInOverlay } from './core/components/logging-in-overlay/logging-in-overlay.component';
import { EntityResolver } from './core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from './core/providers/error-handler/error-handler.service';
import { I18nService } from './core/providers/i18n/i18n.service';
import { MaintenanceModeService } from './core/providers/maintenance-mode/maintenance-mode.service';
import { MessageService } from './core/providers/message/message.service';
import { NavigationService } from './core/providers/navigation/navigation.service';
import { PermissionService } from './core/providers/permissions/permission.service';
import { UserSettingsService } from './core/providers/user-settings/user-settings.service';
import { UsersnapService } from './core/providers/usersnap/usersnap.service';
import { EmbeddedToolsService } from './embedded-tools/providers/embedded-tools/embedded-tools.service';
import { KeycloakService } from './login/providers/keycloak/keycloak.service';
import { ChipSearchBarConfigService } from './shared/providers/chip-search-bar-config/chip-search-bar-config.service';
import { UIOverridesService } from './shared/providers/ui-overrides/ui-overrides.service';
import {
    ApplicationStateService,
    AuthActionsService,
    ContentRepositoryActionsService,
    ContentStagingActionsService,
    EditorActionsService,
    FeaturesActionsService,
    FolderActionsService,
    NodeSettingsActionsService,
    UIActionsService,
} from './state';
import { MockAppState, TestApplicationState } from './state/test-application-state.mock';
import { ActionsSelectorComponent } from './core';

// Override polling method
class App extends OriginalApp {
    pollAlertCenter(): void {}
}

@Component({
    selector: 'basic-search-bar',
    template: '',
    })
class MockBasicSearchBarComponent {}

@Component({
    selector: 'advanced-search-bar',
    template: '',
    })
class MockAdvancedSearchBarComponent {}

@Component({
    selector: 'filter-search-bar',
    template: '',
    // tslint:disable-next-line: no-inputs-metadata-property
    inputs: ['activeNode'],
    })
class MockFilterSearchBarComponent {}

@Component({
    selector: 'embedded-tools-host',
    template: '',
    })
class MockEmbeddedToolHostComponent { }

@Component({
    selector: 'tool-breadcrumb',
    template: '',
    })
class MockToolBreadcrumbComponent {}

@Component({
    selector: 'tool-selector',
    template: '',
    })
class MockToolSelectorComponent {}

@Component({
    selector: 'chip-search-bar',
    template: '',
    })
class MockChipSearchBarComponent {
    @Input()
    chipSearchBarConfig: any;
    @Input()
    loading: any;
}

@Component({
    selector: 'favourites-list',
    template: '',
    })
class MockFavouritesListComponent {}

@Component({
    selector: 'message-inbox',
    template: '',
    })
class MockMessageInboxComponent {}

@Component({
    selector: 'alert-center',
    template: '',
    })
class MockAlertCenterComponent {}

class MockEmbeddedToolsService {
    loadAvailableToolsWhenLoggedIn = jasmine.createSpy('loadAvailableToolsWhenLoggedIn');
    updateStateWhenRouteChanges = jasmine.createSpy('updateStateWhenRouteChanges');
    manageTabbedToolsWhenStateChanges = jasmine.createSpy('manageTabbedToolsWhenStateChanges');
}

class MockUIOverridesService {
    loadCustomerConfiguration = jasmine.createSpy('loadCustomerConfiguration');
}

class MockUserSettingsService {
    loadInitialSettings = jasmine.createSpy('loadInitialSettings');
    loadUserSettingsWhenLoggedIn = jasmine.createSpy('loadUserSettingsWhenLoggedIn');
    saveRecentItemsOnUpdate = jasmine.createSpy('saveRecentItemsOnUpdate');
    watchForSettingChangesInOtherTabs = jasmine.createSpy('watchForSettingChangesInOtherTabs');
}

class MockErrorHandler {}

class MockI18nService {}

class MockContentStagingActions {}

class MockMaintenanceModeService {
    refreshPeriodically = jasmine.createSpy('refreshPeriodically');
    refreshOnLogout = jasmine.createSpy('refreshOnLogout');
    validateSessionWhenActivated = jasmine.createSpy('validateSessionWhenActivated');
    displayNotificationWhenActive = jasmine.createSpy('displayNotificationWhenActive');
}

class MockMessageService {
    poll = jasmine.createSpy('poll');
    whenInboxOpens = jasmine.createSpy('whenInboxOpens');
}
class MockModalService {}

class MockPermissionService {
    wastebin$ = new Subject();
}

class MockEntityResolver {
    getNode = jasmine.createSpy('getNode');
    getLanguage = jasmine.createSpy('getLanguage');
}

class MockFolderActions {
    getNodes = jasmine.createSpy('getNodes');
}

class MockEditorActions {}

class MockNavigationService {}

class MockUIActions {
    getUiVersion = jasmine.createSpy('getUiVersion');
    getCmsVersion = jasmine.createSpy('getCmsVersion');
    getAlerts = jasmine.createSpy('getAlerts');
}

class MockFeaturesActions {
    checkAll = jasmine.createSpy('checkAll');
    loadNodeFeatures = jasmine.createSpy('loadNodeFeatures');
}

class MockAuthActions {
    validateSession = jasmine.createSpy('validateSession');
    updateAdminState = jasmine.createSpy('updateAdminState');
}

class MockNodeSettingsActions {
    loadNodeSettings(): void {}
}

class MockContentRepositoryActions {
    fetchAllContentrepositories = jasmine.createSpy('fetchAllContentrepositories');
}

class MockUsersnapService {
    init = jasmine.createSpy('init');
}

class MockTranslateService {
    onLangChange = new EventEmitter<LangChangeEvent>();
    onTranslationChange: EventEmitter<any> = new EventEmitter();
    onDefaultLangChange: EventEmitter<any> = new EventEmitter();
    get(key: string | Array<string>, interpolateParams?: object): Observable<string | any> {
        return new BehaviorSubject<string>('').asObservable();
    }
    instant = (str: string) => `translated(${str})`;
}

describe('AppComponent', () => {

    let state: TestApplicationState;
    let uiActions: MockUIActions;
    let userSettings: MockUserSettingsService;
    let featuresActions: MockFeaturesActions;
    let authActions: MockAuthActions;
    let maintenanceMode: MockMaintenanceModeService;
    let folderActions: MockFolderActions;
    let embeddedTools: MockEmbeddedToolsService;
    let contentrepositories: MockContentRepositoryActions;

    const DEBOUNCE_INTERVAL = 50;

    beforeEach(() => {
        configureComponentTest({
            imports: [
                NoopAnimationsModule,
                GenticsUICoreModule.forRoot(),
                CmsComponentsModule,
                RouterTestingModule,
                ReactiveFormsModule,
                HttpClientModule,
                FormsModule,
            ],
            declarations: [
                App,
                ActionsSelectorComponent,
                MockAdvancedSearchBarComponent,
                MockBasicSearchBarComponent,
                MockFilterSearchBarComponent,
                MockEmbeddedToolHostComponent,
                MockToolBreadcrumbComponent,
                MockToolSelectorComponent,
                LoggingInOverlay,
                MockChipSearchBarComponent,
                MockFavouritesListComponent,
                MockMessageInboxComponent,
                MockAlertCenterComponent,
                TranslatePipe,
            ],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: AuthActionsService, useClass: MockAuthActions },
                { provide: EditorActionsService, useClass: MockEditorActions },
                { provide: EmbeddedToolsService, useClass: MockEmbeddedToolsService },
                { provide: EntityResolver, useClass: MockEntityResolver },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: FeaturesActionsService, useClass: MockFeaturesActions },
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: I18nService, useClass: MockI18nService },
                { provide: MaintenanceModeService, useClass: MockMaintenanceModeService },
                { provide: MessageService, useClass: MockMessageService },
                { provide: ModalService, useClass: MockModalService },
                { provide: NavigationService, useClass: MockNavigationService },
                { provide: NodeSettingsActionsService, useClass: MockNodeSettingsActions },
                { provide: PermissionService, useClass: MockPermissionService },
                { provide: UIActionsService, useClass: MockUIActions },
                { provide: UIOverridesService, useClass: MockUIOverridesService },
                { provide: UserSettingsService, useClass: MockUserSettingsService },
                { provide: ContentRepositoryActionsService, useClass: MockContentRepositoryActions },
                { provide: UsersnapService, useClass: MockUsersnapService },
                { provide: KeycloakService },
                { provide: TranslateService, useClass: MockTranslateService },
                { provide: ContentStagingActionsService, useClass: MockContentStagingActions },
                ChipSearchBarConfigService,
                WindowRef,
            ],
        });

        uiActions = TestBed.get(UIActionsService);
        userSettings = TestBed.get(UserSettingsService);
        featuresActions = TestBed.get(FeaturesActionsService);
        authActions = TestBed.get(AuthActionsService);
        maintenanceMode = TestBed.get(MaintenanceModeService);
        state = TestBed.get(ApplicationStateService);
        folderActions = TestBed.get(FolderActionsService);
        embeddedTools = TestBed.get(EmbeddedToolsService);
        contentrepositories = TestBed.get(ContentRepositoryActionsService);
    });

    describe('initialization', () => {

        describe('before logging in', () => {

            // eslint-disable-next-line prefer-arrow/prefer-arrow-functions
            function assertMethodCalledOnInit(getMethodsToTest: () => jasmine.Spy[]): () => void {
                return componentTest(() => App, fixture => {
                    fixture.detectChanges();
                    for (const method of getMethodsToTest()) {
                        expect(method).toHaveBeenCalled();
                    }
                    discardPeriodicTasks();
                });
            }

            it('calls uiActions initialization methods', assertMethodCalledOnInit(() => ([
                uiActions.getUiVersion,
            ])));

            it('calls userSettings initialization methods', assertMethodCalledOnInit(() => ([
                userSettings.loadInitialSettings,
                userSettings.loadUserSettingsWhenLoggedIn,
                userSettings.saveRecentItemsOnUpdate,
                userSettings.watchForSettingChangesInOtherTabs,
            ])));

            it('calls authActions initialization methods', assertMethodCalledOnInit(() => ([
                authActions.validateSession,
            ])));

            it('calls embedded tools initialization methods', assertMethodCalledOnInit(() => ([
                embeddedTools.loadAvailableToolsWhenLoggedIn,
                embeddedTools.manageTabbedToolsWhenStateChanges,
                embeddedTools.updateStateWhenRouteChanges,
            ])));

            it('calls maintenanceMode initialization methods', assertMethodCalledOnInit(() => ([
                maintenanceMode.displayNotificationWhenActive,
                maintenanceMode.refreshOnLogout,
                maintenanceMode.validateSessionWhenActivated,
                maintenanceMode.displayNotificationWhenActive,
            ])));

            it('does not display the top bar items', componentTest(() => App, fixture => {
                tick(DEBOUNCE_INTERVAL); // needed because we use debounce() in the component
                fixture.detectChanges();
                const topBar = fixture.debugElement.query(By.css('.top-bar'));

                expect(topBar === null).toBe(true);
            }));

            it('initializes Usersnap', () => {
                const fixture = TestBed.createComponent(App);
                fixture.detectChanges();

                const usersnapService: MockUsersnapService = TestBed.get(UsersnapService);
                expect(usersnapService.init).toHaveBeenCalledTimes(1);
            });

        });

        describe('after logging in', () => {

            let mockInitialState: MockAppState;

            function simulateLogin(): void {
                state.mockState({
                    ...mockInitialState,
                    ...{
                        auth: {
                            isAdmin: true,
                            isLoggedIn: true,
                            currentUserId: 123,
                            loggingIn: false,
                        },
                        entities: {
                            ...mockInitialState.entities,
                            ...{
                                user: {
                                    123: {
                                        id: 123,
                                    },
                                },
                            },
                            language: {
                                1: { id: 1, code: 'en', name: 'English' },
                                2: { id: 2, code: 'de', name: 'Deutsch' },
                            },
                        },
                    },
                });
            }

            function simulateLinkChecker(featureEnabled: boolean, toolAvailable: boolean, currentAlerts: boolean, numberOfBrokenLinks: number): void {
                // simulate loading of features, tools and alerts
                const currentState = state.now;
                state.mockState({
                    ...currentState,
                    ...{
                        features: {
                            ...currentState.features,
                            ...{
                                nodeFeatures: featureEnabled ? {
                                    1: [NodeFeature.linkChecker],
                                } : {},
                            },
                        },
                        tools: {
                            ...currentState.tools,
                            ...{
                                available: toolAvailable ? [{
                                    id: 1,
                                    key: 'linkchecker',
                                    name: { de: 'Link Checker', en: 'Link Checker' },
                                    toolUrl: '/tools/link-checker/?sid=15345',
                                    newtab: false,
                                }] : [],
                            },
                        },
                        ui: {
                            ...currentState.ui,
                            ...{
                                alerts: currentAlerts ? {
                                    linkChecker: {
                                        brokenLinksCount: numberOfBrokenLinks,
                                    },
                                } : {},
                            },
                        },
                    },
                });
            }

            beforeEach(() => {
                mockInitialState = {
                    auth: { isAdmin: false, isLoggedIn: false, loggingIn: true },
                    folder: {
                        activeNode: 1,
                        searchFiltersChanging: false,
                        folders: {
                            fetching: false,
                        },
                        forms: {
                            fetching: false,
                        },
                        files: {
                            fetching: false,
                        },
                        images: {
                            fetching: false,
                        },
                        pages: {
                            fetching: false,
                        },
                    },
                    entities: {
                        node: {
                            1: { folderId: 33, id: 1 },
                        },
                    },
                };
                state.mockState(mockInitialState);
            });

            it('displays the top bar items', componentTest(() => App, fixture => {
                fixture.detectChanges();
                simulateLogin();
                fixture.detectChanges();
                tick(DEBOUNCE_INTERVAL);
                fixture.detectChanges();
                const topBar = fixture.debugElement.query(By.css('.top-bar'));

                expect(topBar).not.toBeNull();
            }));

            it('displays the alert center corner action item if link checker feature and tool are available and there are broken links', componentTest(() => App, fixture => {
                fixture.detectChanges();
                simulateLogin();
                simulateLinkChecker(true, true, true, 4);

                fixture.detectChanges();
                tick(DEBOUNCE_INTERVAL);
                fixture.detectChanges();
                const cornerAction = fixture.debugElement.query(By.css('gtx-button.alert-center'));

                expect(cornerAction).not.toBeNull();
            }));

            it('does not display the alert center corner action item if link checker feature is not enabled', componentTest(() => App, fixture => {
                fixture.detectChanges();
                simulateLogin();
                simulateLinkChecker(false, true, true, 4);

                fixture.detectChanges();
                tick(DEBOUNCE_INTERVAL);
                fixture.detectChanges();
                const cornerAction = fixture.debugElement.query(By.css('gtx-button.alert-center'));

                expect(cornerAction).toBeNull();
            }));

            it('does not display the alert center corner action item if link checker tool is not available', componentTest(() => App, fixture => {
                fixture.detectChanges();
                simulateLogin();
                simulateLinkChecker(true, false, true, 4);

                fixture.detectChanges();
                tick(DEBOUNCE_INTERVAL);
                fixture.detectChanges();
                const cornerAction = fixture.debugElement.query(By.css('gtx-button.alert-center'));

                expect(cornerAction).toBeNull();
            }));

            it('does not display the alert center corner action item if there are no alerts', componentTest(() => App, fixture => {
                fixture.detectChanges();
                simulateLogin();
                simulateLinkChecker(true, true, false, 4);

                fixture.detectChanges();
                tick(DEBOUNCE_INTERVAL);
                fixture.detectChanges();
                const cornerAction = fixture.debugElement.query(By.css('gtx-button.alert-center'));

                expect(cornerAction).toBeNull();
            }));

            it('does not display the alert center corner action item if there are 0 broken links', componentTest(() => App, fixture => {
                fixture.detectChanges();
                simulateLogin();
                simulateLinkChecker(true, true, true, 0);

                fixture.detectChanges();
                tick(DEBOUNCE_INTERVAL);
                fixture.detectChanges();
                const cornerAction = fixture.debugElement.query(By.css('gtx-button.alert-center'));

                expect(cornerAction).toBeNull();
            }));

            it('calls folderActions.getNodes()', componentTest(() => App, fixture => {
                fixture.detectChanges();
                simulateLogin();
                fixture.detectChanges();
                tick(DEBOUNCE_INTERVAL);
                fixture.detectChanges();

                expect(folderActions.getNodes).toHaveBeenCalled();
            }));

            it('calls uiActions.getCmsVersion()', componentTest(() => App, fixture => {
                fixture.detectChanges();
                simulateLogin();
                fixture.detectChanges();
                tick(DEBOUNCE_INTERVAL);
                fixture.detectChanges();

                expect(uiActions.getCmsVersion).toHaveBeenCalled();
            }));

            it('calls authActions.updateAdminState()', componentTest(() => App, fixture => {
                fixture.detectChanges();
                simulateLogin();
                fixture.detectChanges();
                tick(DEBOUNCE_INTERVAL);
                fixture.detectChanges();

                expect(authActions.updateAdminState).toHaveBeenCalled();
            }));

            it('calls featuresActions.checkAll()', componentTest(() => App, fixture => {
                fixture.detectChanges();
                simulateLogin();
                fixture.detectChanges();
                tick(DEBOUNCE_INTERVAL);
                fixture.detectChanges();

                expect(featuresActions.checkAll).toHaveBeenCalled();
            }));

            it('displays the Gentics logo', componentTest(() => App, fixture => {
                fixture.detectChanges();
                simulateLogin();
                fixture.detectChanges();

                tick(DEBOUNCE_INTERVAL);
                fixture.detectChanges();
                const logo = fixture.debugElement.query(By.css('.gentics-logo'));
                expect(logo === null).toBe(false);
            }));

            it('sets the local activeNode value', componentTest(() => App, fixture => {
                const instance = fixture.componentInstance;
                fixture.detectChanges();
                simulateLogin();
                fixture.detectChanges();

                tick(DEBOUNCE_INTERVAL);
                fixture.detectChanges();
                expect(instance.activeNode).toBeDefined();
                expect(instance.activeNode.folderId).toBeDefined();
            }));

            it('nodeRootLink$ has the correct value', componentTest(() => App, fixture => {
                const instance = fixture.componentInstance;
                fixture.detectChanges();
                simulateLogin();
                fixture.detectChanges();

                tick(DEBOUNCE_INTERVAL);
                instance.nodeRootLink$.pipe(take(1))
                    .subscribe(val => {
                        expect(val).toEqual(['/editor', {outlets: {list: ['node', 1, 'folder', 33]}}]);
                    });
            }));

            it('loads the features of a node when folderState.activeNode changes', () => {
                const fixture = TestBed.createComponent(App);
                const instance = fixture.componentInstance;
                fixture.detectChanges();
                simulateLogin();
                fixture.detectChanges();
                featuresActions.loadNodeFeatures.calls.reset();

                state.mockState({
                    folder: {
                        ...state.now.folder,
                        activeNode: 4711,
                    },
                });
                expect(featuresActions.loadNodeFeatures).toHaveBeenCalledWith(4711);
            });

            it('loads the features of a node when editorState.nodeId changes', () => {
                const fixture = TestBed.createComponent(App);
                const instance = fixture.componentInstance;
                fixture.detectChanges();
                simulateLogin();
                fixture.detectChanges();
                featuresActions.loadNodeFeatures.calls.reset();

                state.mockState({
                    editor: {
                        ...state.now.editor,
                        nodeId: 4711,
                    },
                });
                expect(featuresActions.loadNodeFeatures).toHaveBeenCalledWith(4711);
            });

            it('loads contentrepositories', () => {
                const fixture = TestBed.createComponent(App);
                fixture.detectChanges();
                simulateLogin();
                fixture.detectChanges();

                expect(contentrepositories.fetchAllContentrepositories).toHaveBeenCalled();
            });

        });
    });
});
