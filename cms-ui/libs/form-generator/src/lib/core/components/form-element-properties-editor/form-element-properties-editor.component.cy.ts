/* eslint-disable @typescript-eslint/naming-convention */
import { Pipe, PipeTransform } from '@angular/core';
import { CoreModule } from '@gentics/cms-components';
import { CmsFormElementI18nValue, CmsFormElementPropertyDefault, CmsFormElementPropertyType } from '@gentics/cms-models';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { mockPipe } from '@gentics/ui-core/testing/mock-pipe';
import { Observable, of } from 'rxjs';
import {
    FormEditorConfiguration,
    FormEditorConfigurationType,
    FormElementConfiguration,
    FormElementPropertyConfigurationDefault,
    FormElementPropertyTypeConfiguration,
} from '../../../common/models/form-editor-configuration';
import { I18nFgPipe, I18nFgSource } from '../../pipes';
import { FormEditorConfigurationService, FormEditorService } from '../../providers';
import { FormElementPropertiesEditorComponent } from './form-element-properties-editor.component';

const DEFAULT_I18N_TEXT = {
    en: 'test',
    de: 'test',
};

const DEFAULT_STRING_PROP: CmsFormElementPropertyDefault = {
    name: 'example',
    type: CmsFormElementPropertyType.STRING,
    label_i18n_ui: DEFAULT_I18N_TEXT,
};

const DEFAULT_STRING_CONF: FormElementPropertyConfigurationDefault = {
    name: 'example',
    type: FormElementPropertyTypeConfiguration.STRING,
    label_i18n_ui: DEFAULT_I18N_TEXT,
};

const DEFAULT_ELEMENT: FormElementConfiguration = {
    type: CmsFormElementPropertyType.STRING,
    is_container: false,
    description_i18n_ui: DEFAULT_I18N_TEXT,
    type_label_i18n_ui: DEFAULT_I18N_TEXT,
    properties: [DEFAULT_STRING_CONF],
}

class MockFormEditorConfigurationService implements Partial<FormEditorConfigurationService> {
    getConfiguration$?: (type: FormEditorConfigurationType) => Observable<FormEditorConfiguration> = () => {
        return of({
            elements: [
                DEFAULT_ELEMENT,
            ],
            form_properties: {
                admin_mail_options: null,
                template_context_options: null,
            },
        })
    };
}

@Pipe({
    name: 'i18nfg$',
    standalone: false
})
class MockI18nFgPipe implements Partial<I18nFgPipe>, PipeTransform {
    transform(labelI18n: CmsFormElementI18nValue<string | number | boolean>, source: I18nFgSource, fallbackLangCode?: string): Observable<any> {
        return of(labelI18n);
    }
}

describe('FormElementPropertiesEditorComponent', () => {

    it('should render the provided properties correctly', () => {
        cy.mount(FormElementPropertiesEditorComponent, {
            imports: [
                CoreModule,
                GenticsUICoreModule.forRoot(),
            ],
            declarations: [
                // eslint-disable-next-line @typescript-eslint/no-unsafe-call
                mockPipe('i18n'),
                MockI18nFgPipe,
            ],
            providers: [
                { provide: FormEditorService, useClass: MockFormEditorConfigurationService },
            ],
            componentProperties: {
                properties: [DEFAULT_STRING_PROP],
            },
        });

        cy.get('[data-cy="string-input"]')
            .should('exist');
    });
});
