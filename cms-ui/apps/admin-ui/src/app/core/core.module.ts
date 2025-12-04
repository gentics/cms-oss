import { NgModule, Optional, SkipSelf } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { API_BASE_URL, CmsComponentsModule } from '@gentics/cms-components';
import { GCMSRestClientModule } from '@gentics/cms-rest-client-angular';
import { GCMS_API_BASE_URL, GCMS_API_ERROR_HANDLER, GCMS_API_SID, GcmsRestClientsAngularModule } from '@gentics/cms-rest-clients-angular';
import { MeshRestClientModule } from '@gentics/mesh-rest-client-angular';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { TranslateModule } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { throwIfAlreadyLoaded, USER_ACTION_PERMISSIONS, USER_ACTION_PERMISSIONS_DEF } from '../common';
import { MeshModule } from '../mesh';
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
    AdminOperations,
    AuthOperations,
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
    EditorUiLocalStorageService,
    ElasticSearchIndexOperations,
    EntityManagerService,
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
    LogoutCleanupService,
    MaintenanceModeService,
    MarkupLanguageOperations,
    MessageService,
    NodeHandlerService,
    NodeOperations,
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
    UserSettingsService,
    UsersnapService,
    UserTableLoaderService,
} from './providers';

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
