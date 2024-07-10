import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { GenticsUICoreModule } from '@gentics/ui-core';
import {
    FormBrowseBoxComponent,
    I18nCheckboxComponent,
    I18nInputComponent,
    I18nSelectComponent,
    KeyI18nValueListInputComponent,
} from '../form-controls';
import {
    BasePropertiesComponent,
    BrowseBoxComponent,
    GtxAppVersionLabelComponent,
    GtxLinkToManualComponent,
    GtxUserMenuComponent,
    GtxUserMenuToggleComponent,
    PasswordConfirmInputComponent,
    StringListComponent,
    VersionModalComponent,
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
    SafePipe,
    ValuesPipe,
} from './pipes';
import {
    I18nService,
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
