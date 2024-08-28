import { API_BASE_URL, throwIfAlreadyLoaded, USER_ACTION_PERMISSIONS, USER_ACTION_PERMISSIONS_DEF } from '@admin-ui/common';
import {
    BreadcrumbsService,
    ErrorHandler,
    I18nNotificationService,
    I18nService,
    LocalTranslateLoader,
    LoggingHelperService,
} from '@admin-ui/core';
import { KeycloakService } from '@admin-ui/login/providers/keycloak/keycloak.service';
import { MeshModule } from '@admin-ui/mesh';
import { SharedModule } from '@admin-ui/shared/shared.module';
import { AppStateService, StateModule } from '@admin-ui/state';
import { APP_INITIALIZER, ErrorHandler as NgErrorHandler, NgModule, Optional, SkipSelf } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { CmsComponentsModule } from '@gentics/cms-components';
import { GCMSRestClientModule, GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMS_API_BASE_URL, GCMS_API_ERROR_HANDLER, GCMS_API_SID, GcmsRestClientsAngularModule } from '@gentics/cms-rest-clients-angular';
import { MeshRestClientModule } from '@gentics/mesh-rest-client-angular';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { HotkeyModule } from 'angular2-hotkeys';
import { LoggerModule, NgxLoggerLevel } from 'ngx-logger';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
    ActivityManagerComponent,
    ChangePasswordModalComponent,
    ConfirmReloadModalComponent,
    DebugToolModalComponent,
    DiscardChangesModalComponent,
    LoggingInOverlayComponent,
    MessageBodyComponent,
    MessageInboxComponent,
    MessageListComponent,
    MessageModalComponent,
    ViewUnauthorizedComponent,
} from './components';
import { AuthGuard, DiscardChangesGuard, PermissionsGuard } from './guards';
import {
    ActivityManagerService,
    BreadcrumbResolver,
    ConstructCategoryHandlerService,
    ConstructHandlerService,
    ConstructTableLoaderService,
    ContentPackageOperations,
    ContentRepositoryFragmentOperations,
    ContentRepositoryFragmentTagmapEntryOperations,
    ContentRepositoryHandlerService,
    ContentRepositoryTableLoaderService,
    ContentRepositoryTagmapEntryOperations,
    CRFragmentTableLoaderService,
    DataSourceEntryHandlerService,
    DataSourceHandlerService,
    DataSourceTableLoaderService,
    DevToolPackageHandlerService,
    DevToolPackageManagerService,
    DevToolPackageTableLoaderService,
    EditorCloserService,
    EditorTabTrackerService,
    ElasticSearchIndexOperations,
    FeatureOperations,
    FileOperations,
    FolderOperations,
    FolderTrableLoaderService,
    FormOperations,
    GroupOperations,
    GroupTableLoaderService,
    GroupTrableLoaderService,
    ImageOperations,
    LanguageHandlerService,
    LanguageTableLoaderService,
    MarkupLanguageOperations,
    MessageService,
    NodeTableLoaderService,
    ObjectPropertyCategoryHandlerService,
    ObjectPropertyHandlerService,
    ObjectPropertyTableLoaderService,
    PageOperations,
    PermissionsService,
    PermissionsTrableLoaderService,
    RoleOperations,
    RouteEntityResolverService,
    ScheduleExecutionOperations,
    ScheduleOperations,
    ScheduleTaskOperations,
    ServerStorageService,
    TagMapEntryTableLoaderService,
    TemplateOperations,
    TemplateTableLoaderService,
    TemplateTagOperations,
    TemplateTagStatusOperations,
    UserOperations,
    UsersnapService,
    UserTableLoaderService,
} from './providers';
import { DebugToolService } from './providers/debug-tool/debug-tool.service';
import { EditorUiLocalStorageService } from './providers/editor-ui-local-storage/editor-ui-local-storage.service';
import { EntityManagerService } from './providers/entity-manager/entity-manager.service';
import { LogoutCleanupService } from './providers/logout-cleanup/logout-cleanup.service';
import { MaintenanceModeService } from './providers/maintenance-mode/maintenance-mode.service';
import { AdminOperations } from './providers/operations/admin/admin.operations';
import { AuthOperations } from './providers/operations/auth/auth.operations';
import { NodeOperations } from './providers/operations/node';
import { TraceErrorHandler } from './providers/trace-error-handler/trace-error-handler';
import { UserSettingsService } from './providers/user-settings/user-settings.service';

export const createSidObservable = (appState: AppStateService): Observable<number> => appState.select(state => state.auth.sid);

export function initializeApp(appState: AppStateService, client: GCMSRestClientService, router: Router): () => Promise<void> {
    return () => {
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        client.init({
            connection: {
                absolute: false,
                basePath: '/rest',
            },
        });
        appState.select(state => state.auth.sid).subscribe(sid => {
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            client.setSessionId(sid);
        });

        return KeycloakService.checkKeycloakAuth(router);
    };
}

const COMPONENTS: any[] = [
    ActivityManagerComponent,
    ChangePasswordModalComponent,
    ConfirmReloadModalComponent,
    DebugToolModalComponent,
    DiscardChangesModalComponent,
    LoggingInOverlayComponent,
    MessageBodyComponent,
    MessageInboxComponent,
    MessageListComponent,
    MessageModalComponent,
    ViewUnauthorizedComponent,
];

const OPERATIONS: any[] = [
    AdminOperations,
    AuthOperations,
    ContentPackageOperations,
    ContentRepositoryFragmentOperations,
    ContentRepositoryFragmentTagmapEntryOperations,
    ContentRepositoryTagmapEntryOperations,
    ElasticSearchIndexOperations,
    FileOperations,
    FeatureOperations,
    FolderOperations,
    FormOperations,
    GroupOperations,
    ImageOperations,
    MarkupLanguageOperations,
    NodeOperations,
    PageOperations,
    RoleOperations,
    ScheduleOperations,
    ScheduleExecutionOperations,
    ScheduleTaskOperations,
    TemplateOperations,
    TemplateTagOperations,
    TemplateTagStatusOperations,
    UserOperations,
];

const PROVIDERS: any[] = [
    ActivityManagerService,
    AuthGuard,
    BreadcrumbResolver,
    BreadcrumbsService,
    ConstructHandlerService,
    ConstructTableLoaderService,
    ConstructCategoryHandlerService,
    ContentRepositoryHandlerService,
    ContentRepositoryTableLoaderService,
    CRFragmentTableLoaderService,
    DataSourceHandlerService,
    DataSourceTableLoaderService,
    DataSourceEntryHandlerService,
    DebugToolService,
    DevToolPackageHandlerService,
    DevToolPackageManagerService,
    DevToolPackageTableLoaderService,
    DiscardChangesGuard,
    EditorCloserService,
    EditorTabTrackerService,
    EditorUiLocalStorageService,
    EntityManagerService,
    ErrorHandler,
    FolderTrableLoaderService,
    GroupTableLoaderService,
    GroupTrableLoaderService,
    I18nNotificationService,
    I18nService,
    LanguageHandlerService,
    LanguageTableLoaderService,
    LoggingHelperService,
    LogoutCleanupService,
    MaintenanceModeService,
    MessageService,
    NodeTableLoaderService,
    ObjectPropertyHandlerService,
    ObjectPropertyCategoryHandlerService,
    ObjectPropertyTableLoaderService,
    PermissionsGuard,
    PermissionsService,
    PermissionsTrableLoaderService,
    RouteEntityResolverService,
    ServerStorageService,
    TagMapEntryTableLoaderService,
    TemplateTableLoaderService,
    UserTableLoaderService,
    UserSettingsService,
    UsersnapService,

    ...OPERATIONS,

    { provide: USER_ACTION_PERMISSIONS, useValue: USER_ACTION_PERMISSIONS_DEF },

    // @gentics/cms-rest-clients-angular configuration
    { provide: GCMS_API_BASE_URL, useValue: API_BASE_URL },
    { provide: GCMS_API_ERROR_HANDLER, useClass: ErrorHandler },
    {
        provide: GCMS_API_SID,
        useFactory: createSidObservable,
        deps: [AppStateService],
    },
    { provide: NgErrorHandler, useClass: TraceErrorHandler },
    {
        provide: APP_INITIALIZER,
        useFactory: initializeApp,
        deps: [ AppStateService, GCMSRestClientService, Router],
        multi: true,
    },
];

@NgModule({
    id: 'admin-ui_core',
    declarations: COMPONENTS,
    imports: [
        BrowserModule,
        BrowserAnimationsModule,
        GcmsRestClientsAngularModule,
        GCMSRestClientModule,
        MeshRestClientModule,
        GenticsUICoreModule,
        CmsComponentsModule,
        HotkeyModule.forRoot(),
        TranslateModule.forRoot({
            loader: { provide: TranslateLoader, useClass: LocalTranslateLoader },
        }),
        LoggerModule.forRoot({ disableConsoleLogging: false, level: environment.production ? NgxLoggerLevel.ERROR : NgxLoggerLevel.DEBUG }),
        SharedModule,
        StateModule,
        MeshModule,
    ],
    exports: [
        TranslateModule,
        SharedModule,
        MeshModule,
        ...COMPONENTS,
    ],
    providers: PROVIDERS,
})
export class CoreModule {
    constructor(@Optional() @SkipSelf() parentModule: CoreModule) {
        throwIfAlreadyLoaded(parentModule, 'CoreModule');
    }
}
