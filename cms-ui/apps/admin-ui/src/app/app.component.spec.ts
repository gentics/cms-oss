import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, EventEmitter, Injectable, NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { TestBed, tick, waitForAsync } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { CmsComponentsModule } from '@gentics/cms-components';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { GenticsUICoreModule, IBreadcrumbRouterLink, ModalService } from '@gentics/ui-core';
import { LangChangeEvent, TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AngularSvgIconModule } from 'angular-svg-icon';
import { LoggerTestingModule } from 'ngx-logger/testing';
import { BehaviorSubject, NEVER, Observable, Subject, of } from 'rxjs';
import { componentTest } from '../testing';
import { AppComponent } from './app.component';
import { USER_ACTION_PERMISSIONS, USER_ACTION_PERMISSIONS_DEF } from './common';
import { InterfaceOf } from './common/utils/util-types/util-types';
import {
    ActivityManagerService,
    EditorUiLocalStorageService,
    EntityManagerService,
    ErrorHandler,
    FeatureOperations,
    GtxActivityManagerActivity,
    LanguageHandlerService,
    MarkupLanguageOperations,
    MessageService,
    NodeOperations,
    PermissionsService,
    UserSettingsService,
    UsersnapService,
} from './core';
import { ActivityManagerComponent } from './core/components/activity-manager';
import { LoggingInOverlayComponent } from './core/components/logging-in-overlay/logging-in-overlay.component';
import { MessageBodyComponent } from './core/components/message-body';
import { MessageInboxComponent } from './core/components/message-inbox/message-inbox.component';
import { MessageListComponent } from './core/components/message-list/message-list.component';
import { BreadcrumbsService } from './core/providers/breadcrumbs/breadcrumbs.service';
import { DebugToolService } from './core/providers/debug-tool/debug-tool.service';
import { I18nService } from './core/providers/i18n/i18n.service';
import { MockI18nServiceWithSpies } from './core/providers/i18n/i18n.service.mock';
import { LoggingHelperService } from './core/providers/logging-helper/logging-helper.service';
import { LogoutCleanupService } from './core/providers/logout-cleanup/logout-cleanup.service';
import { MaintenanceModeService } from './core/providers/maintenance-mode/maintenance-mode.service';
import { AdminOperations } from './core/providers/operations/admin/admin.operations';
import { AuthOperations } from './core/providers/operations/auth';
import { KeycloakService } from './login/providers/keycloak/keycloak.service';
import { GenericRouterOutletComponent } from './shared/components/generic-router-outlet/generic-router-outlet.component';
import { IconCheckboxComponent } from './shared/components/icon-checkbox/icon-checkbox.component';
import { ActionAllowedDirective } from './shared/directives/action-allowed/action-allowed.directive';
import { AppStateService } from './state';
import { TEST_APP_STATE, TestAppState, assembleTestAppStateImports } from './state/utils/test-app-state';
import { NodeListRequestOptions, Node, ModelType } from '@gentics/cms-models';

class MockApiBase {
    get: any = jasmine.createSpy('ApiBase.get').and.returnValue(NEVER);
    post: any = jasmine.createSpy('ApiBase.post').and.returnValue(NEVER);
    upload: any = jasmine.createSpy('ApiBase.upload').and.returnValue(NEVER);
}

class MockAuthOperations {
    validateSessionFromLocalStorage = jasmine.createSpy('validateSessionFromLocalStorage').and.stub();
}

abstract class InitializableService {
    init = jasmine.createSpy('init').and.stub();
}

class MockBreadcrumbsService extends InitializableService {
    breadcrumbs$ = new BehaviorSubject<IBreadcrumbRouterLink[]>([]);
}

class MockEditorUiLocalStorageService extends InitializableService {}

class MockEntityManager extends InitializableService {}

class MockFeatureOperations implements Partial<InterfaceOf<FeatureOperations>> {
    checkAllGlobalFeaturesSubj$ = new Subject<any>();
    checkAllGlobalFeatures = jasmine.createSpy('checkAllGlobalFeatures').and.returnValue(this.checkAllGlobalFeaturesSubj$);
}

class MockAdminOperations implements Partial<InterfaceOf<AdminOperations>> {
    getCmsVersion = jasmine.createSpy('getCmsVersion').and.returnValue(of({}));
    getCmsUpdates = jasmine.createSpy('getCmsUpdates').and.returnValue(of({}));
}

class MockLoggingHelperService extends InitializableService { }
class MockLanguageHandlerService implements Partial<InterfaceOf<LanguageHandlerService>> {
    getBackendLanguages = jasmine.createSpy('getBackendLanguages').and.stub();
}

class MockLogoutCleanupService extends InitializableService { }

class MockMaintenanceModeService implements Partial<InterfaceOf<MaintenanceModeService>> {
    refreshPeriodically = jasmine.createSpy('refreshPeriodically').and.stub();
    refreshOnLogout = jasmine.createSpy('refreshOnLogout').and.stub();
    validateSessionWhenActivated = jasmine.createSpy('validateSessionWhenActivated').and.stub();
    displayNotificationWhenActive = jasmine.createSpy('displayNotificationWhenActive').and.stub();
}

class MockPermissionsService {
    getTypePermissions = jasmine.createSpy('getTypePermissions').and.returnValue(of());
    checkPermissions = jasmine.createSpy('checkPermissions').and.returnValue(of(true));
}

class MockUserSettingsService implements Partial<InterfaceOf<UserSettingsService>> {
    init = jasmine.createSpy('init').and.stub();
}

class MockUsersnapService extends InitializableService {}

class MockDebugToolService extends InitializableService {}

class MockNodeOperations implements Partial<InterfaceOf<NodeOperations>> {
    getAll(options?: NodeListRequestOptions): Observable<Node<ModelType.Raw>[]> {
        return of([]);
    }
}

class MockActivityManagerService {
    get activities$(): Observable<GtxActivityManagerActivity[]> {
        return of([]);
    }
}

class MockMarkupLanguageOperations extends InitializableService {
    getAll = jasmine.createSpy('getAll').and.returnValue(of([]));
}

@Injectable()
class MockErrorHandler {
    catch = jasmine.createSpy('ErrorHandler.catch');
}

@Injectable()
class MockModalService {
    fromComponent = jasmine.createSpy('ModalService.fromComponent')
        .and.returnValue(new Promise(neverResolve => {}));
}

@Injectable()
class MockTranslateService {
    onLangChange = new EventEmitter<LangChangeEvent>();
    onTranslationChange: EventEmitter<any> = new EventEmitter();
    onDefaultLangChange: EventEmitter<any> = new EventEmitter();
    get(key: string | Array<string>, interpolateParams?: object): Observable<string | any> {
        return new BehaviorSubject<string>('').asObservable();
    }
    instant = (str: string) => `translated(${str})`;
}

@Pipe({ name: 'i18n' })
class MockI18nPipe implements PipeTransform {
    transform(key: string, params: object): string {
        return key + (params ? ':' + JSON.stringify(params) : '');
    }
}

@Component({
    template: `<gtx-app-root></gtx-app-root>`,
})
class TestComponent { }

describe('AppComponent', () => {

    let appState: TestAppState;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [
                GenticsUICoreModule.forRoot(),
                assembleTestAppStateImports(),
                NoopAnimationsModule,
                CmsComponentsModule,
                LoggerTestingModule,
                RouterTestingModule,
                HttpClientTestingModule,
                AngularSvgIconModule.forRoot(),
            ],
            declarations: [
                ActionAllowedDirective,
                ActivityManagerComponent,
                AppComponent,
                GenericRouterOutletComponent,
                IconCheckboxComponent,
                LoggingInOverlayComponent,
                MessageBodyComponent,
                MessageInboxComponent,
                MessageListComponent,
                TestComponent,
                TranslatePipe,
            ],
            providers: [
                TEST_APP_STATE,
                { provide: GcmsApi, useClass: MockApiBase },
                { provide: PermissionsService, useClass: MockPermissionsService },
                { provide: USER_ACTION_PERMISSIONS, useValue: USER_ACTION_PERMISSIONS_DEF },
                { provide: TranslateService, useClass: MockTranslateService },
                { provide: I18nService, useClass: MockI18nServiceWithSpies },

                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: AuthOperations, useClass: MockAuthOperations },
                { provide: BreadcrumbsService, useClass: MockBreadcrumbsService },
                { provide: DebugToolService, useClass: MockDebugToolService },
                { provide: EditorUiLocalStorageService, useClass: MockEditorUiLocalStorageService },
                { provide: EntityManagerService, useClass: MockEntityManager },
                { provide: FeatureOperations, useClass: MockFeatureOperations },
                { provide: I18nService, useClass: MockI18nServiceWithSpies },
                { provide: LanguageHandlerService, useClass: MockLanguageHandlerService },
                { provide: LoggingHelperService, useClass: MockLoggingHelperService },
                { provide: LogoutCleanupService, useClass: MockLogoutCleanupService },
                { provide: MaintenanceModeService, useClass: MockMaintenanceModeService },
                MessageService,
                { provide: UserSettingsService, useClass: MockUserSettingsService },
                { provide: UsersnapService, useClass: MockUsersnapService },
                { provide: AdminOperations, useClass: MockAdminOperations },
                { provide: ActivityManagerService, useClass: MockActivityManagerService },
                { provide: KeycloakService },
                { provide: ModalService, useClass: MockModalService },
                { provide: ActivityManagerService, useClass: MockActivityManagerService },
                { provide: MarkupLanguageOperations, useClass: MockMarkupLanguageOperations },
                { provide: NodeOperations, useClass: MockNodeOperations },
            ],
            schemas: [NO_ERRORS_SCHEMA],
        }).compileComponents();

        appState = TestBed.get(AppStateService);

        appState.mockState({
            auth: {
                isLoggedIn: false,
            },
        });
    }));

    it('initializes services that require initialization',
        componentTest(() => TestComponent, (fixture, instance) => {
            const breadcrumbs: MockBreadcrumbsService = TestBed.get(BreadcrumbsService);
            const debugTool: MockDebugToolService = TestBed.get(DebugToolService);
            const editorUiLocalStorage: MockEditorUiLocalStorageService = TestBed.get(EditorUiLocalStorageService);
            const entityManager: MockEntityManager = TestBed.get(EntityManagerService);
            const loggingHelper: MockLoggingHelperService = TestBed.get(LoggingHelperService);
            const logoutCleanup: MockLogoutCleanupService = TestBed.get(LogoutCleanupService);
            const maintenanceMode: MockMaintenanceModeService = TestBed.get(MaintenanceModeService);
            const messageService: MessageService = TestBed.get(MessageService);
            const userSettings: MockUserSettingsService = TestBed.get(UserSettingsService);
            const usersnapService: MockUsersnapService = TestBed.get(UsersnapService);

            const spyOnMessageServicePoll = spyOn(messageService, 'poll').and.callThrough();
            messageService.openInbox();

            fixture.detectChanges();
            tick();

            expect(loggingHelper.init).toHaveBeenCalledTimes(1);
            expect(logoutCleanup.init).toHaveBeenCalledTimes(1);
            expect(userSettings.init).toHaveBeenCalledTimes(1);
            expect(breadcrumbs.init).toHaveBeenCalledTimes(1);
            expect(editorUiLocalStorage.init).toHaveBeenCalledTimes(1);
            expect(entityManager.init).toHaveBeenCalledTimes(1);

            expect(maintenanceMode.refreshOnLogout).toHaveBeenCalledTimes(1);
            expect(maintenanceMode.refreshPeriodically).toHaveBeenCalledTimes(1);
            expect(maintenanceMode.validateSessionWhenActivated).toHaveBeenCalledTimes(1);
            expect(debugTool.init).toHaveBeenCalledTimes(1);
            expect(usersnapService.init).toHaveBeenCalledTimes(1);

            expect(spyOnMessageServicePoll).toHaveBeenCalledTimes(1);
            messageService.onOpenInbox$.subscribe(data => {
                expect(data).not.toBeUndefined();
            });
        }),
    );

    it('executes the required tasks on login',
        componentTest(() => TestComponent, (fixture, instance) => {
            const featureOps: MockFeatureOperations = TestBed.get(FeatureOperations);
            fixture.detectChanges();
            tick();

            // Make sure that the operations are not executed before the login.
            expect(featureOps.checkAllGlobalFeatures).not.toHaveBeenCalled();

            appState.mockState({
                auth: {
                    isLoggedIn: true,
                },
            });

            fixture.detectChanges();
            tick();

            expect(featureOps.checkAllGlobalFeatures).toHaveBeenCalledTimes(1);
            tick();
            expect(featureOps.checkAllGlobalFeaturesSubj$.observers.length).toBe(1);
        }),
    );

});
