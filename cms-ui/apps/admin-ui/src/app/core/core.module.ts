import { MeshModule } from '@admin-ui/mesh';
import { inject, NgModule, Optional, provideAppInitializer, SkipSelf } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { CmsComponentsModule, KeycloakService } from '@gentics/cms-components';
import { GCMSRestClientModule, GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMS_API_BASE_URL, GCMS_API_ERROR_HANDLER, GCMS_API_SID, GcmsRestClientsAngularModule } from '@gentics/cms-rest-clients-angular';
import { MeshRestClientModule } from '@gentics/mesh-rest-client-angular';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import DE_TRANSLATIONS from '../../../public/i18n/de.json';
import EN_TRANSLATIONS from '../../../public/i18n/en.json';
import { API_BASE_URL, throwIfAlreadyLoaded, USER_ACTION_PERMISSIONS, USER_ACTION_PERMISSIONS_DEF } from '../common';
import { SharedModule } from '../shared/shared.module';
import { AppStateService, StateModule } from '../state';
import {
    ActivityManagerComponent,
    ChangePasswordModalComponent,
    ConfirmReloadModalComponent,
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
    AdminHandlerService,
    BreadcrumbResolver,
    BreadcrumbsService,
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
    ErrorHandler,
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
    NodeHandlerService,
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
    ScheduleHandlerService,
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
import { EditorUiLocalStorageService } from './providers/editor-ui-local-storage/editor-ui-local-storage.service';
import { EntityManagerService } from './providers/entity-manager/entity-manager.service';
import { LogoutCleanupService } from './providers/logout-cleanup/logout-cleanup.service';
import { MaintenanceModeService } from './providers/maintenance-mode/maintenance-mode.service';
import { AdminOperations } from './providers/operations/admin/admin.operations';
import { AuthOperations } from './providers/operations/auth/auth.operations';
import { NodeOperations } from './providers/operations/node';
import { UserSettingsService } from './providers/user-settings/user-settings.service';

export const createSidObservable = (appState: AppStateService): Observable<number> => appState.select((state) => state.auth.sid);

const COMPONENTS: any[] = [
    ActivityManagerComponent,
    ChangePasswordModalComponent,
    ConfirmReloadModalComponent,
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
    AdminHandlerService,
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
    LanguageHandlerService,
    LanguageTableLoaderService,
    LogoutCleanupService,
    MaintenanceModeService,
    MessageService,
    NodeHandlerService,
    NodeTableLoaderService,
    ObjectPropertyHandlerService,
    ObjectPropertyCategoryHandlerService,
    ObjectPropertyTableLoaderService,
    PermissionsGuard,
    PermissionsService,
    PermissionsTrableLoaderService,
    RouteEntityResolverService,
    ScheduleHandlerService,
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
    provideAppInitializer(() => {
        const appState = inject(AppStateService);
        const client = inject(GCMSRestClientService);
        const keycloak = inject(KeycloakService);
        const translations = inject(TranslateService);

        translations.setTranslation('de', DE_TRANSLATIONS, true);
        translations.setTranslation('en', EN_TRANSLATIONS, true);

        client.init({
            connection: {
                absolute: false,
                basePath: '/rest',
            },
        });
        appState.select((state) => state.auth.sid).subscribe((sid) => {
            client.setSessionId(sid);
        });

        return keycloak.checkKeycloakAuth();
    }),
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
        CmsComponentsModule.forRoot(),
        TranslateModule.forRoot({
            fallbackLang: 'en',
        }),
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
