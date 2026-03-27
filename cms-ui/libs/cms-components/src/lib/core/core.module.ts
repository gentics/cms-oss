import { CommonModule } from '@angular/common';
import { NgModule, PipeTransform, Provider, Type } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { TranslateModule } from '@ngx-translate/core';
import {
    AppVersionLabelComponent,
    BrowseBoxComponent,
    FormBrowseBoxComponent,
    I18nInputComponent,
    I18nPanelGroupComponent,
    I18nSelectComponent,
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
    I18nObjectPipe,
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
    AppVersionLabelComponent,
    BrowseBoxComponent,
    FormBrowseBoxComponent,
    I18nInputComponent,
    I18nPanelGroupComponent,
    I18nSelectComponent,
    LinkToManualComponent,
    PasswordConfirmInputComponent,
    RichContentEditorComponent,
    RichContentLinkPropertiesComponent,
    RichContentModal,
    StringListComponent,
    UserMenuComponent,
    UserMenuToggleComponent,
    VersionModalComponent,
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
    I18nObjectPipe,
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
