import { NO_ERRORS_SCHEMA, provideZoneChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CmsComponentsModule } from '@gentics/cms-components';
import {
    FormElement,
    FormElementConfiguration,
    FormSchemaProperty,
    FormSelectSetting,
    FormSettingType,
    FormTypeConfiguration,
} from '@gentics/cms-models';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { provideTranslateService } from '@ngx-translate/core';
import { FormGridModule } from '../../form-grid.module';
import { DynamicFormSettingsComponent } from './dynamic-form-settings.component';

const MAIL_TEMPLATE_SETTING: FormSelectSetting = {
    type: FormSettingType.SELECT,
    id: 'mailtemplate',
    labelI18n: { en: 'Email Template' },
    backend: true,
    options: [
        { value: 'useremail', labelI18n: { en: 'Default' } },
    ],
};

const ELEMENT: FormElement = {
    id: 'usermail_1',
    type: 'property',
} as FormElement;

const ELEMENT_CONFIG: FormElementConfiguration = {
    labelI18n: { en: 'User EMail Field' },
};

const CONFIG: FormTypeConfiguration = {
    type: 'generic',
    pluginName: 'forms',
    controls: {},
    flows: [],
};

function createSchema(): FormSchemaProperty {
    return {
        type: 'usermail',
        formGridOptions: {
            mailtemplate: 'useremail',
        },
    } as FormSchemaProperty;
}

describe('DynamicFormSettingsComponent', () => {
    let fixture: ComponentFixture<DynamicFormSettingsComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                FormsModule,
                ReactiveFormsModule,
                GenticsUICoreModule.forRoot(),
                CmsComponentsModule,
                FormGridModule,
            ],
            providers: [
                provideTranslateService(),
                provideZoneChangeDetection(),
            ],
            schemas: [NO_ERRORS_SCHEMA],
        });
    });

    function render(setting: FormSelectSetting): void {
        fixture = TestBed.createComponent(DynamicFormSettingsComponent);
        fixture.componentRef.setInput('config', CONFIG);
        fixture.componentRef.setInput('contextId', 'ctx');
        fixture.componentRef.setInput('settings', [setting]);
        fixture.componentRef.setInput('element', ELEMENT);
        fixture.componentRef.setInput('elementConfig', ELEMENT_CONFIG);
        fixture.componentRef.setInput('elementSchema', createSchema());
        fixture.componentRef.setInput('disabled', false);
        fixture.componentRef.setInput('languages', ['en']);
        fixture.detectChanges();
    }

    function getClearButton(settingId: string): HTMLButtonElement | null {
        return (fixture.nativeElement as HTMLElement)
            .querySelector(`gtx-select[data-control="${settingId}"] [data-action="clear"]`);
    }

    it('should allow clearing the value of a non-required select setting', () => {
        render(MAIL_TEMPLATE_SETTING);

        const clearButton = getClearButton('mailtemplate');
        expect(clearButton).not.toBeNull();

        clearButton.click();
        fixture.detectChanges();

        expect(fixture.componentInstance.form.get('mailtemplate').value).toBeNull();
        expect((fixture.componentInstance.elementSchema() as any).formGridOptions.mailtemplate).toBeNull();
    });

    it('should not allow clearing the value of a required select setting', () => {
        render({ ...MAIL_TEMPLATE_SETTING, required: true });

        expect(getClearButton('mailtemplate')).toBeNull();
    });
});
