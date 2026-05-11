import { CommonModule } from '@angular/common';
import { inject, ModuleWithProviders, NgModule, provideAppInitializer } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { CmsComponentsModule } from '@gentics/cms-components';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { TranslateService } from '@ngx-translate/core';
import { provideStates } from '@ngxs/store';
import * as DE_TRANSLATIONS from '../../public/i18n/de.json';
import * as EN_TRANSLATIONS from '../../public/i18n/en.json';
import {
    AlohaAttributeButtonRendererComponent,
    AlohaAttributeToggleButtonRendererComponent,
    AlohaButtonRendererComponent,
    AlohaCheckboxRendererComponent,
    AlohaColorPickerRendererComponent,
    AlohaComponentRendererComponentImpl,
    AlohaContextButtonRendererComponent,
    AlohaContextToggleButtonRendererComponent,
    AlohaDateTimePickerRendererComponent,
    AlohaIFrameRendererComponent,
    AlohaInputRendererComponent,
    AlohaLinkTargetRendererComponent,
    AlohaSelectMenuRendererComponentImpl,
    AlohaSelectRendererComponent,
    AlohaSplitButtonRendererComponent,
    AlohaSymbolGridRendererComponent,
    AlohaSymbolSearchGridRendererComponent,
    AlohaTableSizeSelectRendererComponent,
    AlohaTextEditorComponent,
    AlohaToggleButtonRendererComponent,
    AlohaToggleSplitButtonRendererComponent,
    DynamicDropdownComponent,
    DynamicFormModal,
    SymbolGridComponent,
    TableSizeInputComponent,
    TableSizeSelectComponent,
} from './components';
import { ALOHA_OVERLAY_TOKEN } from './models';
import {
    AlohaIntegrationService,
    AlohaOverlayService,
} from './providers';
import { AlohaStateModule } from './state';

const COMPONENTS = [
    AlohaAttributeButtonRendererComponent,
    AlohaAttributeToggleButtonRendererComponent,
    AlohaButtonRendererComponent,
    AlohaCheckboxRendererComponent,
    AlohaColorPickerRendererComponent,
    AlohaContextButtonRendererComponent,
    AlohaContextToggleButtonRendererComponent,
    AlohaDateTimePickerRendererComponent,
    AlohaIFrameRendererComponent,
    AlohaInputRendererComponent,
    AlohaLinkTargetRendererComponent,
    AlohaSelectRendererComponent,
    AlohaSplitButtonRendererComponent,
    AlohaSymbolGridRendererComponent,
    AlohaSymbolSearchGridRendererComponent,
    AlohaTableSizeSelectRendererComponent,
    AlohaToggleButtonRendererComponent,
    AlohaToggleSplitButtonRendererComponent,

    // Hacky workaround components
    AlohaComponentRendererComponentImpl,
    AlohaSelectMenuRendererComponentImpl,

    DynamicDropdownComponent,
    DynamicFormModal,
    SymbolGridComponent,
    TableSizeInputComponent,
    TableSizeSelectComponent,

    AlohaTextEditorComponent,
];

const PROVIDERS = [
    AlohaIntegrationService,
    AlohaOverlayService,
    { provide: ALOHA_OVERLAY_TOKEN, useClass: AlohaOverlayService },
];

@NgModule({
    imports: [
        CommonModule,
        ReactiveFormsModule,
        CmsComponentsModule,
        GenticsUICoreModule,
    ],
    declarations: [...COMPONENTS],
    providers: [...PROVIDERS],
    exports: [...COMPONENTS],
})
export class AlohaModule {
    static forRoot(): ModuleWithProviders<AlohaModule> {
        return {
            ngModule: AlohaModule,
            providers: [
                provideAppInitializer(() => {
                    const translations = inject(TranslateService);
                    translations.setTranslation('de', DE_TRANSLATIONS, true);
                    translations.setTranslation('en', EN_TRANSLATIONS, true);
                }),
                provideStates([AlohaStateModule]),
            ],
        };
    }
}
