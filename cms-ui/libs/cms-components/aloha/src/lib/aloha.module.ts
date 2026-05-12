import { CommonModule } from '@angular/common';
import { inject, ModuleWithProviders, NgModule, provideAppInitializer } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { AlohaCoreComponentNames } from '@gentics/aloha-models';
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
    AlohaComponentRendererComponent,
    AlohaContextButtonRendererComponent,
    AlohaContextToggleButtonRendererComponent,
    AlohaDateTimePickerRendererComponent,
    AlohaIFrameRendererComponent,
    AlohaInputRendererComponent,
    AlohaLinkTargetRendererComponent,
    AlohaSelectMenuRendererComponent,
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
import {
    AlohaComponentResolverService,
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
    AlohaComponentRendererComponent,
    AlohaContextButtonRendererComponent,
    AlohaContextToggleButtonRendererComponent,
    AlohaDateTimePickerRendererComponent,
    AlohaIFrameRendererComponent,
    AlohaInputRendererComponent,
    AlohaLinkTargetRendererComponent,
    AlohaSelectRendererComponent,
    AlohaSelectMenuRendererComponent,
    AlohaSplitButtonRendererComponent,
    AlohaSymbolGridRendererComponent,
    AlohaSymbolSearchGridRendererComponent,
    AlohaTableSizeSelectRendererComponent,
    AlohaToggleButtonRendererComponent,
    AlohaToggleSplitButtonRendererComponent,

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
                AlohaComponentResolverService,
                provideAppInitializer(() => {
                    const translations = inject(TranslateService);
                    translations.setTranslation('de', DE_TRANSLATIONS, true);
                    translations.setTranslation('en', EN_TRANSLATIONS, true);

                    const resolver = inject(AlohaComponentResolverService);
                    resolver.registerComponent(AlohaCoreComponentNames.ATTRIBUTE_BUTTON, AlohaAttributeButtonRendererComponent);
                    resolver.registerComponent(AlohaCoreComponentNames.ATTRIBUTE_TOGGLE_BUTTON, AlohaAttributeToggleButtonRendererComponent);
                    resolver.registerComponent(AlohaCoreComponentNames.BUTTON, AlohaButtonRendererComponent);
                    resolver.registerComponent(AlohaCoreComponentNames.CHECKBOX, AlohaCheckboxRendererComponent);
                    resolver.registerComponent(AlohaCoreComponentNames.COLOR_PICKER, AlohaColorPickerRendererComponent);
                    resolver.registerComponent(AlohaCoreComponentNames.CONTEXT_BUTTON, AlohaContextButtonRendererComponent);
                    resolver.registerComponent(AlohaCoreComponentNames.CONTEXT_TOGGLE_BUTTON, AlohaContextToggleButtonRendererComponent);
                    resolver.registerComponent(AlohaCoreComponentNames.DATE_TIME_PICKER, AlohaDateTimePickerRendererComponent);
                    resolver.registerComponent(AlohaCoreComponentNames.IFRAME, AlohaIFrameRendererComponent);
                    resolver.registerComponent(AlohaCoreComponentNames.INPUT, AlohaInputRendererComponent);
                    resolver.registerComponent(AlohaCoreComponentNames.LINK_TARGET, AlohaLinkTargetRendererComponent);
                    resolver.registerComponent(AlohaCoreComponentNames.SELECT, AlohaSelectRendererComponent);
                    resolver.registerComponent(AlohaCoreComponentNames.SELECT_MENU, AlohaSelectMenuRendererComponent);
                    resolver.registerComponent(AlohaCoreComponentNames.SPLIT_BUTTON, AlohaSplitButtonRendererComponent);
                    resolver.registerComponent(AlohaCoreComponentNames.SYMBOL_GRID, AlohaSymbolGridRendererComponent);
                    resolver.registerComponent(AlohaCoreComponentNames.SYMBOL_SEARCH_GRID, AlohaSymbolSearchGridRendererComponent);
                    resolver.registerComponent(AlohaCoreComponentNames.TABLE_SIZE_SELECT, AlohaTableSizeSelectRendererComponent);
                    resolver.registerComponent(AlohaCoreComponentNames.TOGGLE_BUTTON, AlohaToggleButtonRendererComponent);
                    resolver.registerComponent(AlohaCoreComponentNames.TOGGLE_SPLIT_BUTTON, AlohaToggleSplitButtonRendererComponent);
                }),
                provideStates([AlohaStateModule]),
            ],
        };
    }
}
