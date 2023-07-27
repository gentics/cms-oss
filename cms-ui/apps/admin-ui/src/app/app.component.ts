import { ObservableStopper } from '@admin-ui/common/utils/observable-stopper/observable-stopper';
import {
    ActivityManagerService,
    AuthOperations,
    BreadcrumbsService,
    EditorUiLocalStorageService,
    EntityManagerService,
    ErrorHandler,
    FeatureOperations,
    LanguageHandlerService,
    LoggingHelperService,
    MarkupLanguageOperations,
    MessageService,
    PermissionsService,
    UserSettingsService,
    UsersnapService,
} from '@admin-ui/core';
import { ChangePasswordModalComponent } from '@admin-ui/core/components/change-password-modal/change-password-modal.component';
import { ConfirmReloadModalComponent } from '@admin-ui/core/components/confirm-reload-modal/confirm-reload-modal.component';
import { DebugToolService } from '@admin-ui/core/providers/debug-tool/debug-tool.service';
import { LogoutCleanupService } from '@admin-ui/core/providers/logout-cleanup/logout-cleanup.service';
import { MaintenanceModeService } from '@admin-ui/core/providers/maintenance-mode/maintenance-mode.service';
import { AdminOperations } from '@admin-ui/core/providers/operations/admin/admin.operations';
import { SelectState, selectLoginEventOrIsLoggedIn } from '@admin-ui/state';
import { AppStateService } from '@admin-ui/state/providers/app-state/app-state.service';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AccessControlledType, Feature, GcmsPermission, GcmsUiLanguage, GtxVersion, I18nLanguage, Normalized, User } from '@gentics/cms-models';
import { IBreadcrumbRouterLink, ModalService } from '@gentics/ui-core';
import { NGXLogger } from 'ngx-logger';
import { Observable, forkJoin, of } from 'rxjs';
import { filter, first, map, switchMap, takeUntil } from 'rxjs/operators';
import { AdminUIModuleRoutes } from './common';
import { KeycloakService } from './login/providers/keycloak/keycloak.service';
import { SetBackendLanguage } from './state/ui/ui.actions';

@Component({
    selector: 'gtx-app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
})
export class AppComponent implements OnDestroy, OnInit {

    breadcrumbs$: Observable<IBreadcrumbRouterLink[]>;

    @SelectState(state => state.auth.isLoggedIn)
    isLoggedIn$: Observable<boolean>;

    @SelectState(state => state.messages.unread.length)
    unreadMessageCount$: Observable<number>;

    activitiesCount$: Observable<number>;
    activitiesPending$: Observable<boolean>;

    showLoginSpinner$: Observable<boolean>;
    userMenuOpened = false;

    currentLanguage$: Observable<GcmsUiLanguage>;
    supportedLanguages$: Observable<I18nLanguage[]>;

    currentUser$: Observable<User<Normalized>>;

    userMenuTabIdMessages = 'messages';
    userMenuTabIdActivities = 'activities';
    userMenuActiveTab = this.userMenuTabIdMessages;

    cmpVersion$: Observable<GtxVersion>;
    uiVersion$: Observable<string>;

    featureHideManual$: Observable<boolean>;

    keycloakSignOut$: Observable<boolean>;

    private stopper = new ObservableStopper();

    constructor(
        private appState: AppStateService,
        private authOps: AuthOperations,
        private breadcrumbs: BreadcrumbsService,
        private debugToolService: DebugToolService,
        private editorUiLocalStorage: EditorUiLocalStorageService,
        private entityManager: EntityManagerService,
        private languageHandler: LanguageHandlerService,
        private features: FeatureOperations,
        private logger: NGXLogger,
        private loggingHelper: LoggingHelperService,
        private logoutCleanup: LogoutCleanupService,
        private maintenanceMode: MaintenanceModeService,
        private message: MessageService,
        private modalService: ModalService,
        public permissions: PermissionsService,
        private router: Router,
        private userSettings: UserSettingsService,
        private usersnapService: UsersnapService,
        private adminOps: AdminOperations,
        private activityManager: ActivityManagerService,
        private errorHandler: ErrorHandler,
        private keycloakService: KeycloakService,
        private markupLangOperations: MarkupLanguageOperations,
    ) {
        this.loggingHelper.init();
    }

    ngOnInit(): void {
        this.logger.debug('AppComponent.ngOnInit');

        this.logoutCleanup.init();

        this.editorUiLocalStorage.init();

        this.userSettings.init();

        this.breadcrumbs.init();
        this.breadcrumbs$ = this.breadcrumbs.breadcrumbs$;

        this.entityManager.init();

        this.showLoginSpinner$ = this.appState.select(state => state.auth).pipe(
            map(auth => auth.loggingIn || auth.loggingOut),
        );

        this.keycloakSignOut$ = this.appState.select(state => state.features.global[Feature.KEYCLOAK_SIGNOUT]);

        this.authOps.validateSessionFromLocalStorage();

        // Maintenance Mode
        this.maintenanceMode.refreshPeriodically(30000);
        this.maintenanceMode.refreshOnLogout();
        this.maintenanceMode.validateSessionWhenActivated();

        // Message Service
        this.message.onOpenInbox$.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe(() => {
            this.userMenuOpened = true;
            this.displayMessages();
        });
        this.message.poll();

        this.currentLanguage$ = this.appState.select(state => state.ui.language);

        selectLoginEventOrIsLoggedIn(this.appState).pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe(() => {
            this.onLogin();
            this.supportedLanguages$ = this.languageHandler.getBackendLanguages();
        });

        this.debugToolService.init();
        this.usersnapService.init();

        this.activitiesCount$ = this.activityManager.activities$.pipe(
            map(tasks => tasks.length),
        );

        this.activitiesPending$ = this.activityManager.activities$.pipe(
            map(tasks => tasks.some(task => task.inProgress)),
        );

        this.currentUser$ = this.appState.select(state => state.auth).pipe(
            filter(auth => auth.isLoggedIn),
            switchMap(auth =>
                this.appState.select(state => state.entity.user[auth.currentUserId]).pipe(
                    filter(user => user != null),
                    first(),
                ),
            ),
        );

        this.cmpVersion$ = this.appState.select(state => state.ui.cmpVersion);
        this.uiVersion$ = this.appState.select(state => state.ui.uiVersion);
        this.featureHideManual$ = this.appState.select(state => state.features.global[Feature.HIDE_MANUAL] || false);
    }

    ngOnDestroy(): void {
        this.logger.debug('AppComponent.ngOnDestroy');
        this.stopper.stop();
    }

    onLogoutClick(): void {
        this.keycloakSignOut$.pipe(first()).subscribe(singleSignOut => {
            this.authOps.logout(this.appState.now.auth.sid)
                .then(() => {
                    if (singleSignOut) {
                        return this.keycloakService.logout();
                    } else {
                        return Promise.resolve();
                    }
                })
                .then(() => {
                    this.userMenuOpened = false;
                    this.router.navigate([`/${AdminUIModuleRoutes.LOGIN}`]);
                });
        });
    }

    setLanguageConfirmation(language: GcmsUiLanguage): void {
        this.modalService.fromComponent(ConfirmReloadModalComponent, { closeOnOverlayClick: false })
            .then(modal => modal.open())
            .then(value => {
                if (value) {
                    this.languageHandler.setActiveUiLanguage(language).subscribe(() => {
                        this.appState.dispatch(new SetBackendLanguage(language));
                        location.reload(); // changing language requires application reload
                    });
                }
            });
    }

    displayMessages(): void {
        this.userMenuOpened = true;
        this.userMenuActiveTab = this.userMenuTabIdMessages;
    }

    displayActivities(): void {
        this.userMenuOpened = true;
        this.userMenuActiveTab = this.userMenuTabIdActivities;
    }

    onShowPasswordModal(): void {
        this.modalService.fromComponent(ChangePasswordModalComponent)
            .then(modal => modal.open())
            .catch(this.errorHandler.catch);
    }

    private onLogin(): void {
        const loginOperations: any[] = [
            // Get all features
            this.features.checkAllGlobalFeatures(),

            // Check CMS version
            this.adminOps.getCmsVersion(true),

            // Check updates if user has permission
            this.permissions.checkPermissions({
                type: AccessControlledType.AUTO_UPDATE,
                permissions: GcmsPermission.READ,
            }).pipe(
                switchMap(perm => perm ?
                    this.adminOps.getCmsUpdates(true) :
                    of(null),
                ),
            ),

            this.markupLangOperations.getAll(),
        ];

        forkJoin(loginOperations).pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe(() => {});
    }

}
