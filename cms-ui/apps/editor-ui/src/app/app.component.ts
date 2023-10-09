import { ChangeDetectionStrategy, ChangeDetectorRef, Component, HostBinding, OnInit } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { WindowRef } from '@gentics/cms-components';
import {
    EditMode,
    GcmsUiLanguage,
    GtxVersion,
    I18nLanguage,
    Node,
    NodeFeature,
    Normalized,
    User,
} from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { isEqual } from'lodash-es'
import {
    BehaviorSubject,
    NEVER,
    Observable,
    Subject,
    combineLatest,
    merge,
    of,
    timer,
} from 'rxjs';
import {
    debounceTime,
    distinctUntilChanged,
    filter,
    first,
    map,
    mergeMap,
    shareReplay, switchMap,
    take,
    takeWhile,
    tap,
} from 'rxjs/operators';
import { GtxChipSearchConfig, UIState } from './common/models';
import {
    ChangePasswordModal,
    ConfirmReloadModal,
    ContentStagingModal,
    WastebinModal,
} from './core';
import { EntityResolver } from './core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from './core/providers/error-handler/error-handler.service';
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
    CloseEditorAction,
    ContentRepositoryActionsService,
    EditorActionsService,
    FeaturesActionsService,
    FolderActionsService,
    NodeSettingsActionsService,
    SetHideExtrasAction,
    UIActionsService,
} from './state';

@Component({
    selector: 'gtx-app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppComponent implements OnInit {

    hideExtras$: Observable<boolean> = of(false);
    alertCenterCounter$ = new BehaviorSubject<number>(null);
    loggingIn$: Observable<boolean>;
    loggedIn$: Observable<boolean>;
    showToolsSub = new BehaviorSubject<boolean>(false);
    showingTools$ = this.showToolsSub.asObservable().pipe(
        distinctUntilChanged(isEqual),
        debounceTime(50),
    );
    uiState$: Observable<UIState>;
    currentUser$: Observable<User<Normalized>>;
    unreadMessageCount$: Observable<number>;
    nodeRootLink$: Observable<any>;
    keycloakSignOut$: Observable<boolean>;
    toolLinkcheckerAvailable$: Observable<boolean>;

    userSid: number;
    activeNode: Node;
    userMenuOpened = false;

    reloadOnLanguageChange$ = new Subject<boolean>();

    @HostBinding('class.maintenance-mode')
    maintenanceModeActive: boolean;

    @HostBinding('class.focus-mode')
    focusMode: boolean;

    @HostBinding('attr.lang')
    language: string;

    currentUiLanguage$: Observable<GcmsUiLanguage>;
    supportedUiLanguages$: Observable<I18nLanguage[]>;

    userMenuTabIdFavourites = 'favourites';
    userMenuTabIdMessages = 'messages';
    userMenuTabIdAlerts = 'alerts';
    userMenuActiveTab: string;

    cmpVersion$: Observable<GtxVersion>;
    uiVersion$: Observable<string>;

    canUseInbox$: Observable<boolean>;
    featureLinkcheckerEnabled$: Observable<boolean>;
    featureHideManual$: Observable<boolean>;

    /** filter properties to choose from when creating a new filter chip */
    chipSearchBarConfig: GtxChipSearchConfig;

    /** Is TRUE, if any search-triggered entity loading is in progress. */
    searchInProgress$: Observable<boolean>;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        // State Logger needs to be injected to get initialized
        private appState: ApplicationStateService,
        private authActions: AuthActionsService,
        private editorActions: EditorActionsService,
        private embeddedTools: EmbeddedToolsService,
        private entityResolver: EntityResolver,
        private errorHandler: ErrorHandler,
        private featuresActions: FeaturesActionsService,
        private folderActions: FolderActionsService,
        private maintenanceMode: MaintenanceModeService,
        private modalService: ModalService,
        private navigationService: NavigationService,
        private nodeSettingsActions: NodeSettingsActionsService,
        public permissions: PermissionService,
        private router: Router,
        private uiActions: UIActionsService,
        private uiOverrides: UIOverridesService,
        private userSettings: UserSettingsService,
        private contentRepositoryActions: ContentRepositoryActionsService,
        private usersnapService: UsersnapService,
        private windowRef: WindowRef,
        private keycloakService: KeycloakService,
        private chipSearchBarConfigService: ChipSearchBarConfigService,
        private messageService: MessageService,
    ) {}

    ngOnInit(): void {
        this.uiActions.getUiVersion();
        this.userSettings.loadInitialSettings();

        // Check if the user is already logged in (e.g. after page refresh)
        this.authActions.validateSession();

        // Once the user is logged in, populate rest of state from local storage
        this.userSettings.loadUserSettingsWhenLoggedIn();
        this.userSettings.watchForSettingChangesInOtherTabs();
        this.userSettings.saveRecentItemsOnUpdate();

        // Load customer-specific UI overrides
        this.uiOverrides.loadCustomerConfiguration();

        this.maintenanceMode.refreshPeriodically(30000);
        this.maintenanceMode.refreshOnLogout();
        this.maintenanceMode.validateSessionWhenActivated();
        this.maintenanceMode.displayNotificationWhenActive();

        this.embeddedTools.loadAvailableToolsWhenLoggedIn();
        this.embeddedTools.updateStateWhenRouteChanges();
        this.embeddedTools.manageTabbedToolsWhenStateChanges();

        this.usersnapService.init();

        this.appState.select(state => state.maintenanceMode.active).subscribe(active => {
            this.maintenanceModeActive = active;
            this.changeDetector.markForCheck();
        });
        combineLatest([
            this.appState.select(state => state.editor.focusMode),
            this.appState.select(state => state.editor.editMode),
            this.appState.select(state => state.editor.editorIsOpen),
            this.appState.select(state => state.editor.editorIsFocused),
        ]).subscribe(([active, editMode, open, focused]) => {
            this.focusMode = active && editMode === EditMode.EDIT && open && focused;
            this.changeDetector.markForCheck();
        });

        this.appState.select(state => state.ui.language).pipe(
            // needed to prevent angular from throwing a 'changed after checked' error
            debounceTime(50),
        ).subscribe(language => {
            this.language = language;
            this.changeDetector.markForCheck();
        });

        this.currentUser$ = this.appState.select(state => state.auth).pipe(
            filter(auth => auth.isLoggedIn),
            switchMap(auth => {
                // get all content repositories when user is authenticated
                this.contentRepositoryActions.fetchAllContentrepositories();

                return this.appState.select(state => state.entities.user[auth.currentUserId]).pipe(
                    filter(user => user != null),
                    take(1),
                );
            }),
        );

        this.uiState$ = this.appState.select(state => state.ui);
        this.hideExtras$ = this.uiState$.pipe(
            map(state => state.hideExtras),
        );

        this.keycloakSignOut$ = this.appState.select(state => state.features.keycloak_signout);

        this.unreadMessageCount$ = this.appState.select(state => state.messages.unread.length);
        this.loggingIn$ = this.appState.select(state => state.auth.loggingIn);
        this.loggedIn$ = this.appState.select(state => state.auth.isLoggedIn);

        this.supportedUiLanguages$ = this.appState.select(state => state.ui.availableUiLanguages);
        this.currentUiLanguage$ = this.appState.select(state => state.ui.language);

        this.appState.select(state => state.auth.sid).subscribe(sid => {
            this.userSid = sid;
            this.changeDetector.markForCheck();
        });

        const onLogin$ = this.appState.select(state => state.auth).pipe(
            distinctUntilChanged(isEqual, state => state.currentUserId),
            filter(state => state.isLoggedIn === true),
        );

        // Upon login, load all available nodes.
        onLogin$.subscribe(() => {
            this.folderActions.getNodes();
            // CMS version is only available once logged in.
            this.uiActions.getCmsVersion();
            this.authActions.updateAdminState();

            this.featuresActions.checkAll();
            this.changeDetector.markForCheck();
        });

        // When the user is logged in and the nodes & folders are loaded,
        // we navigate them to the active node & folder.
        onLogin$.pipe(
            switchMap(() => this.appState.select(state => state.folder.activeNode)),
            filter(activeNode => activeNode != null),
            takeWhile(() => this.router.url === '/login' || this.router.url === '/' || this.router.url === ''),
        )
            .subscribe(activeNode => {
                const node = this.entityResolver.getNode(activeNode);
                if (node) {
                    this.navigationService.list(node.id, node.folderId).navigate();
                }
            },
            error => this.errorHandler.catch(error));

        // Whenever the active node or editor node changes, load that node's features if necessary.
        merge(
            this.appState.select(state => state.folder.activeNode).pipe(
                filter(nodeId => Number.isInteger(nodeId)),
            ),
            this.appState.select(state => state.editor.nodeId).pipe(
                filter(nodeId => Number.isInteger(nodeId)),
            ),
        ).pipe(
            distinctUntilChanged(isEqual),
        ).subscribe(nodeId => {
            if (this.appState.now.features.nodeFeatures[nodeId] == null) {
                this.featuresActions.loadNodeFeatures(nodeId);
            }
            if (this.appState.now.nodeSettings.node[nodeId] == null) {
                this.nodeSettingsActions.loadNodeSettings(nodeId);
            }
        });

        this.router.events.pipe(
            filter(event => event instanceof NavigationEnd),
            map(() => this.router.isActive('/tools', false)),
        ).subscribe(this.showToolsSub);

        this.appState.select(state => state.ui.alerts).pipe(
            filter(alerts => Object.values(alerts).length > 0),
        ).subscribe(alerts => {
            const alertCount = Object.values(alerts)
                .map(alertType => (alertType === Object(alertType) ? Object.values(alertType) : alertType) || [])
                // eslint-disable-next-line @typescript-eslint/restrict-plus-operands
                .reduce((acc, val) => acc + val);

            this.alertCenterCounter$.next(alertCount);
        });

        this.messageService.whenInboxOpens(() => this.userMenuOpened = true);
        this.messageService.poll();

        // Poll alert center
        this.pollAlertCenter();

        // Update the routerLink to always point to the root folder of the current node.
        this.nodeRootLink$ = this.appState.select(state => state.folder.activeNode).pipe(
            // prevent change after checked error
            debounceTime(50),
            filter(nodeId => !!nodeId),
            switchMap(nodeId =>
                this.appState.select(state => state.entities.node[nodeId]).pipe(
                    filter(node => !!node),
                    take(1),
                ),
            ),
            tap(node => this.activeNode = node),
            map(node => ['/editor', { outlets: { list: ['node', node.id, 'folder', node.folderId] } }]),
            shareReplay(1),
        );

        this.canUseInbox$ = this.permissions.viewInbox$;

        this.featureLinkcheckerEnabled$ = this.appState.select(state => state.features.nodeFeatures).pipe(
            filter(nodeFeatures => nodeFeatures instanceof Object),
            // check if at least one node has NodeFeature.linkChecker activated
            map(nodeFeatures => {
                return Object.values(nodeFeatures).some(nodeFeaturesOfNode => {
                    return nodeFeaturesOfNode.find(feature => feature === NodeFeature.LINK_CHECKER) ? true : false;
                });
            }),
        );

        this.cmpVersion$ = this.appState.select(state => state.ui.cmpVersion);
        this.uiVersion$ = this.appState.select(state => state.ui.uiVersion);
        this.featureHideManual$ = this.appState.select(state => state.features.hide_manual || false);
        this.toolLinkcheckerAvailable$ = this.appState.select(state => state.tools.available).pipe(
            // Check for link checker tool as done in the alert center component
            map((availableTools) => availableTools.map(tool => tool.key).includes('linkchecker')),
        );

        this.chipSearchBarConfigService.chipSearchBarConfig$.subscribe(config => {
            this.chipSearchBarConfig = config;
            this.changeDetector.markForCheck();
        });

        this.initializeSearchInProgressIndicator();
    }

    pollAlertCenter(): void {
        // Load Alert Center after login and then every 15 minutes
        this.appState.select(state => state.auth).pipe(
            switchMap(state => state.isLoggedIn ? timer(0, 15 * 60 * 1000) : NEVER),
        ).subscribe(() => {
            // Load Alert Center alerts
            this.uiActions.getAlerts();
        });
    }

    logoClick(): void {
        this.folderActions.setSearchTerm('');
    }

    handleActionClick({ action, event }): void {
        switch (action) {
            case 'publish-queue':
                this.displayPublishQueue();
                break;
            case 'wastebin':
                this.displayWastebin();
                break;
            case 'content-staging':
                this.displayContentStaging();
                break;
        }
    }

    displayPublishQueue(): void {
        this.navigationService.modalByType('publishQueue').navigate();
    }

    async displayWastebin(): Promise<void> {
        try {
            const dialog = await this.modalService.fromComponent(WastebinModal, {
                width: '1000px',
            }, {
                nodeId: this.activeNode.id,
            });
            await dialog.open();
        } catch (error) {
            this.errorHandler.catch(error);
        }
    }

    async displayContentStaging(): Promise<void> {
        try {
            const dialog = await this.modalService.fromComponent(ContentStagingModal);
            await dialog.open();
        } catch (error) {
            this.errorHandler.catch(error);
        }
    }

    displayAlertCenter(): void {
        this.userMenuOpened = true;
        this.userMenuActiveTab = this.userMenuTabIdAlerts;
    }

    displayMessages(): void {
        this.userMenuOpened = true;
        this.userMenuActiveTab = this.userMenuTabIdMessages;
    }

    async onShowPasswordModal(): Promise<void> {
        try {
            const dialog = await this.modalService.fromComponent(ChangePasswordModal);
            await dialog.open();
        } catch (error) {
            this.errorHandler.catch(error);
        }
    }

    async setLanguageConfirmation(language: GcmsUiLanguage): Promise<void> {
        try {
            const dialog = await this.modalService.fromComponent(ConfirmReloadModal, {
                closeOnOverlayClick: false,
            });
            const value = await dialog.open();

            if (value) {
                // this.userSettings.setUiLanguage(language);
                this.uiActions.setActiveUiLanguageInBackend(
                    language,
                    // changing language requires application reload
                    () => this.windowRef.nativeWindow.location.reload(),
                );
            }
        } catch (error) {
            this.errorHandler.catch(error);
        }
    }

    onLogoutClick(): void {
        this.keycloakSignOut$.pipe(first()).subscribe(singleSignOut => {
            this.authActions.logout(this.userSid)
                .then(() => {
                    if (singleSignOut) {
                        return this.keycloakService.logout();
                    } else {
                        return Promise.resolve();
                    }
                })
                .then(() => {
                    this.userMenuOpened = false;
                    this.appState.dispatch(new CloseEditorAction());
                    this.router.navigate(['/login']);
                })
                .catch(this.errorHandler.catch);
        });
    }

    resetHideExtras() {
        this.appState.dispatch(new SetHideExtrasAction(false));
    }

    /** Initialize if search is in progress indicator for `ChipSearchBarComponent.loading`-property. */
    private initializeSearchInProgressIndicator(): void {
        this.searchInProgress$ = this.appState.select(state => state.folder.searchFiltersChanging).pipe(
            distinctUntilChanged(isEqual),
            mergeMap(() =>
                combineLatest([
                    this.appState.select(state => state.folder.folders.fetching),
                    this.appState.select(state => state.folder.forms.fetching),
                    this.appState.select(state => state.folder.files.fetching),
                    this.appState.select(state => state.folder.images.fetching),
                    this.appState.select(state => state.folder.pages.fetching),
                ]).pipe(
                    distinctUntilChanged(isEqual),
                    // does change anything?
                    // eslint-disable-next-line @typescript-eslint/no-unsafe-call
                    map(fetchingStates => fetchingStates.some(state => !!state)),
                ),
            ),
        );
    }

}
