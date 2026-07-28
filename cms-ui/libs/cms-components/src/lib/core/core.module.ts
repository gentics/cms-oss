import { CommonModule } from '@angular/common';
import { NgModule, PipeTransform, Provider, Type } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { TranslateModule } from '@ngx-translate/core';
import {
    AppVersionLabelComponent,
    BasePropertiesComponent,
    BrowseBoxComponent,
    FormBrowseBoxComponent,
    I18nCheckboxComponent,
    I18nInputComponent,
    I18nPanelGroupComponent,
    I18nSelectComponent,
    KeyI18nValueListInputComponent,
    LinkToManualComponent,
    PasswordConfirmInputComponent,
    RichContentEditorComponent,
    RichContentLinkPropertiesComponent,
    RichContentModal,
    StringListComponent,
    UserMenuComponent,
    UserMenuToggleComponent,
    VersionModalComponent,
} from './components';
import {
    ClickOutsideDirective,
} from './directives';
import {
    EntriesPipe,
    FilterPipe,
    I18nDatePipe,
    I18nDurationPipe,
    I18nNumberPipe,
    I18nPipe,
    I18nRelativeDatePipe,
    SafePipe,
    StripRichContentPipe,
    ValuesPipe,
} from './pipes';
import {
    I18nDatePickerFormatService,
    I18nNotificationService,
    I18nRelativeDateService,
    I18nService,
    WindowRef,
} from './providers';

const COMPONENTS: any[] = [
    BasePropertiesComponent,
    AppVersionLabelComponent,
    LinkToManualComponent,
    StringListComponent,
    UserMenuComponent,
    UserMenuToggleComponent,
    I18nCheckboxComponent,
    I18nInputComponent,
    I18nPanelGroupComponent,
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

const DIRECTIVES: Type<any>[] = [
    ClickOutsideDirective,
];

const PIPES: Type<PipeTransform>[] = [
    EntriesPipe,
    FilterPipe,
    I18nPipe,
    I18nDatePipe,
    I18nDurationPipe,
    I18nNumberPipe,
    I18nRelativeDatePipe,
    StripRichContentPipe,
    SafePipe,
    ValuesPipe,
];

const DECLARATIONS: any[] = [
    ...COMPONENTS,
    ...DIRECTIVES,
    ...PIPES,
];

const PROVIDERS: Provider[] = [
    I18nRelativeDateService,
    I18nNotificationService,
    I18nDatePickerFormatService,
    I18nService,
    WindowRef,
    ...PIPES,

];

@NgModule({
    declarations: DECLARATIONS,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        GenticsUICoreModule,
        TranslateModule,
    ],
    exports: [
        ...DECLARATIONS,
        TranslateModule,
    ],
    providers: PROVIDERS,
})
export class CoreModule { }
