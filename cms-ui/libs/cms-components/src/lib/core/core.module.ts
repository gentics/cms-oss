import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { GenticsUICoreModule } from '@gentics/ui-core';
import {
    BasePropertiesComponent,
    BrowseBoxComponent,
    FormBrowseBoxComponent,
    RichContentEditorComponent,
    GtxAppVersionLabelComponent,
    GtxLinkToManualComponent,
    GtxUserMenuComponent,
    GtxUserMenuToggleComponent,
    I18nCheckboxComponent,
    I18nInputComponent,
    I18nSelectComponent,
    KeyI18nValueListInputComponent,
    PasswordConfirmInputComponent,
    StringListComponent,
    VersionModalComponent,
    RichContentModal,
    RichContentLinkPropertiesComponent,
} from './components';
import {
    ClickOutsideDirective,
} from './directives';
import {
    DurationPipe,
    EntriesPipe,
    FilterPipe,
    GtxI18nDatePipe,
    GtxI18nPipe,
    GtxI18nRelativeDatePipe,
    GtxI18nRelativeDateService,
    StripRichContentPipe,
    SafePipe,
    ValuesPipe,
} from './pipes';
import {
    I18nService,
    KeycloakService,
    LocalTranslateLoader,
    WindowRef,
} from './providers';

const COMPONENTS: any[] = [
    BasePropertiesComponent,
    GtxAppVersionLabelComponent,
    GtxLinkToManualComponent,
    StringListComponent,
    GtxUserMenuComponent,
    GtxUserMenuToggleComponent,
    I18nCheckboxComponent,
    I18nInputComponent,
    FormBrowseBoxComponent,
    I18nSelectComponent,
    KeyI18nValueListInputComponent,
    VersionModalComponent,
    BrowseBoxComponent,
    PasswordConfirmInputComponent,
    RichContentEditorComponent,
    RichContentModal,
    RichContentLinkPropertiesComponent,
];

const ENTRY_COMPONENTS = [
];

const DIRECTIVES = [
    ClickOutsideDirective,
];

const PIPES: any[] = [
    DurationPipe,
    EntriesPipe,
    FilterPipe,
    GtxI18nDatePipe,
    GtxI18nPipe,
    GtxI18nRelativeDatePipe,
    StripRichContentPipe,
    SafePipe,
    ValuesPipe,
];

const DECLARATIONS: any[] = [
    ...COMPONENTS,
    ...ENTRY_COMPONENTS,
    ...DIRECTIVES,
    ...PIPES,
];

const PROVIDERS: any[] = [
    I18nService,
    LocalTranslateLoader,
    {
        provide: GtxI18nRelativeDateService,
        deps: [ I18nService ],
    },
    WindowRef,
    KeycloakService,
    ...PIPES,
];

@NgModule({
    declarations: DECLARATIONS,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        GenticsUICoreModule,
    ],
    exports: [
        ...DECLARATIONS,
    ],
    providers: PROVIDERS,
})
export class CoreModule { }
