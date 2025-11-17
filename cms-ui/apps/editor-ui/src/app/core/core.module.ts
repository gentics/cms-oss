import { HashLocationStrategy, LocationStrategy } from '@angular/common';
import { inject, NgModule, Optional, provideAppInitializer, SkipSelf } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import {
    CmsComponentsModule,
    GCMS_UI_SERVICES_PROVIDER,
    I18nDatePickerFormatService,
    KeycloakService,
} from '@gentics/cms-components';
import { GcmsUiLanguage } from '@gentics/cms-integration-api-models';
import { GCMSRestClientModule, GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMS_API_BASE_URL, GCMS_API_ERROR_HANDLER, GCMS_API_SID, GcmsRestClientsAngularModule } from '@gentics/cms-rest-clients-angular';
import { DateTimePickerFormatProvider, GenticsUICoreModule } from '@gentics/ui-core';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import DE_TRANSLATIONS from '../../../public/i18n/de.json';
import EN_TRANSLATIONS from '../../../public/i18n/en.json';
import { API_BASE_URL } from '../common/utils/base-urls';
import { throwIfAlreadyLoaded } from '../common/utils/module-import-guard';
import { EmbeddedToolsModule } from '../embedded-tools/embedded-tools.module';
import { EmbeddedToolsService } from '../embedded-tools/providers/embedded-tools/embedded-tools.service';
import { ExposedUIAPI } from '../embedded-tools/providers/exposed-ui-api/exposed-ui-api.service';
import { ToolApiChannelService } from '../embedded-tools/providers/tool-api-channel/tool-api-channel.service';
import { ToolMessagingChannelFactory } from '../embedded-tools/providers/tool-messaging-channel/tool-messaging-channel.factory';
import { SharedModule } from '../shared/shared.module';
import { ApplicationStateService } from '../state';
import { StateModule } from '../state/state.module';
import { TagEditorModule } from '../tag-editor';
import {
    ActionsSelectorComponent,
    AlertCenterComponent,
    AssignPageModal,
    ChangePasswordModal,
    ConfirmReloadModal,
    ContentPackageListComponent,
    ContentStagingModal,
    FavouritesListComponent,
    FileNameConflictModal,
    LoggingInOverlay,
    MessageBody,
    MessageInboxComponent,
    MessageList,
    MessageModal,
    NoNodesComponent,
    ProjectEditorComponent,
    PublishQueueList,
    PublishQueueModal,
    SearchLabel,
    TagEditorRouteComponent,
    UsersList,
    WastebinModal,
} from './components';
import { Api } from './providers/api/api.service';
import { ContextMenuOperationsService } from './providers/context-menu-operations/context-menu-operations.service';
import { DecisionModalsService } from './providers/decision-modals/decision-modals.service';
import { EntityResolver } from './providers/entity-resolver/entity-resolver';
import { ErrorHandler } from './providers/error-handler/error-handler.service';
import { FavouritesService } from './providers/favourites/favourites.service';
import { GcmsUiServices } from './providers/gcms-ui-services/gcms-ui-services.service';
import { AuthGuard } from './providers/guards/auth-guard';
import { OpenModalGuard } from './providers/guards/open-modal-guard';
import { ListSearchService } from './providers/list-search/list-search.service';
import { LocalStorage } from './providers/local-storage/local-storage.service';
import { LocalizationsService } from './providers/localizations/localizations.service';
import { MaintenanceModeService } from './providers/maintenance-mode/maintenance-mode.service';
import { MessageService } from './providers/message/message.service';
import { NavigationService } from './providers/navigation/navigation.service';
import { NodeHierarchyBuilder } from './providers/node-hierarchy-builder/node-hierarchy-builder.service';
import { PermissionService } from './providers/permissions/permission.service';
import { QuickJumpService } from './providers/quick-jump/quick-jump.service';
import { ResourceUrlBuilder } from './providers/resource-url-builder/resource-url-builder';
import { ServerStorage } from './providers/server-storage/server-storage.service';
import { UploadConflictService } from './providers/upload-conflict/upload-conflict.service';
import { UserSettingsService } from './providers/user-settings/user-settings.service';
import { UsersnapService } from './providers/usersnap/usersnap.service';

export const getSidFromAppState = (appState: ApplicationStateService): Observable<number> =>
    appState.select((state) => state.auth.sid);

export const createLanguageObservable = (appState: ApplicationStateService): Observable<GcmsUiLanguage> =>
    appState.select((state) => state.ui.language);

const COMPONENTS = [
    ActionsSelectorComponent,
    AlertCenterComponent,
    ContentPackageListComponent,
    FavouritesListComponent,
    LoggingInOverlay,
    NoNodesComponent,
    MessageBody,
    MessageInboxComponent,
    MessageList,
    ProjectEditorComponent,
    PublishQueueList,
    TagEditorRouteComponent,

    SearchLabel,
    UsersList,
    LoggingInOverlay,
];

const ENTRY_COMPONENTS = [
    AssignPageModal,
    ChangePasswordModal,
    ConfirmReloadModal,
    ContentStagingModal,
    FileNameConflictModal,
    MessageModal,
    PublishQueueModal,
    WastebinModal,
];

const PROVIDERS = [
    Api,
    AuthGuard,
    ContextMenuOperationsService,
    DecisionModalsService,
    EmbeddedToolsService,
    EntityResolver,
    ErrorHandler,
    ExposedUIAPI,
    FavouritesService,
    ListSearchService,
    LocalStorage,
    LocalizationsService,
    MaintenanceModeService,
    MessageService,
    NavigationService,
    NodeHierarchyBuilder,
    OpenModalGuard,
    PermissionService,
    QuickJumpService,
    ResourceUrlBuilder,
    ServerStorage,
    ToolApiChannelService,
    ToolMessagingChannelFactory,
    UploadConflictService,
    UserSettingsService,
    UsersnapService,
    { provide: DateTimePickerFormatProvider, useClass: I18nDatePickerFormatService },
    { provide: LocationStrategy, useClass: HashLocationStrategy },
    { provide: GCMS_API_BASE_URL, useValue: API_BASE_URL },
    { provide: GCMS_API_ERROR_HANDLER, useClass: ErrorHandler },
    { provide: GCMS_UI_SERVICES_PROVIDER, useClass: GcmsUiServices },
    {
        provide: GCMS_API_SID,
        useFactory: getSidFromAppState,
        deps: [ApplicationStateService],
    },
    provideAppInitializer(() => {
        const client = inject(GCMSRestClientService);
        const appState = inject(ApplicationStateService);
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
    imports: [
        BrowserModule,
        BrowserAnimationsModule,
        EmbeddedToolsModule,
        StateModule,
        SharedModule,
        GCMSRestClientModule,
        GcmsRestClientsAngularModule,
        GenticsUICoreModule.forRoot({
            dropDownPageMargin: 20,
        }),
        CmsComponentsModule.forRoot(),
        TagEditorModule,
        TranslateModule.forRoot({
            fallbackLang: 'en',
        }),
    ],
    exports: [
        BrowserModule,
        EmbeddedToolsModule,
        SharedModule,
        GenticsUICoreModule,
        CmsComponentsModule,
        COMPONENTS,
    ],
    declarations: [...COMPONENTS, ...ENTRY_COMPONENTS],
    providers: PROVIDERS,
})
export class CoreModule {
    constructor(@Optional() @SkipSelf() parentModule: CoreModule) {
        throwIfAlreadyLoaded(parentModule, 'CoreModule');
    }
}
