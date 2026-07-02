import { ChangeDetectionStrategy, Component, computed, effect, HostBinding, input, model } from '@angular/core';
import {
    FormElement,
    FormElementConfiguration,
    FormSchemaProperty,
    FormSettingConfiguration,
    I18nString,
} from '@gentics/cms-models';
import { getValueByPath, setByPath } from '@gentics/common';
import { DefaultableFormSelectOptionValue } from '../../models';
import { isSettingVisible } from '../../utils/conditions';

@Component({
    selector: 'gtx-dynamic-form-translations',
    templateUrl: './dynamic-form-translations.component.html',
    styleUrls: ['./dynamic-form-translations.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class DynamicFormTranslationsComponent {

    public readonly settings = input.required<FormSettingConfiguration[]>();

    public readonly element = model.required<FormElement>();
    public readonly elementConfig = input.required<FormElementConfiguration>();
    public readonly elementSchema = model<FormSchemaProperty>();

    public readonly languages = input.required<string[]>();
    public readonly activeLanguage = input.required<string>();

    public readonly disabled = input.required<boolean>();

    public readonly visibleSettings = computed(() => {
        const elConf = this.elementConfig();
        const el = this.element();
        const elSchema = this.elementSchema();

        return this.settings().filter((setting) => isSettingVisible(setting, elConf, el, elSchema));
    });

    @HostBinding('class.has-settings')
    public hasVisibleSettings = false;

    constructor() {
        // Ugly workaround, as HostBindings don't work with signals
        effect(() => {
            this.hasVisibleSettings = this.visibleSettings().length > 0;
        });
    }

    public updateData(setting: FormSettingConfiguration, valueProvider: (original: unknown) => unknown): void {
        const path = setting.propertyPath
            ? setting.propertyPath
            : ['formGridOptions', setting.id];

        if (setting.backend) {
            this.elementSchema.update((data) => {
                const copy = structuredClone(data);
                const innerVal = getValueByPath(copy, path);
                setByPath(copy, path, valueProvider(innerVal));
                return copy;
            });
        } else {
            this.element.update((data) => {
                const copy = structuredClone(data);
                const innerVal = getValueByPath(copy, path);
                setByPath(copy, path, valueProvider(innerVal));
                return copy;
            });
        }
    }

    public updateTranslationSetting(setting: FormSettingConfiguration, value: I18nString | null): void {
        this.updateData(setting, () => value);
    }

    public updateAlohaValue(setting: FormSettingConfiguration, value: string | null): void {
        this.updateData(setting, (data) => {
            if (data == null || typeof data !== 'object') {
                data = {};
            }
            (data as Record<string, string>)[this.activeLanguage()] = value;
            return data;
        });
    }

    public updateOptionLabel(setting: FormSettingConfiguration, index: number, value: I18nString | null): void {
        this.updateData(setting, (data) => {
            if (!Array.isArray(data)) {
                return data;
            }
            const opt: DefaultableFormSelectOptionValue | null = data?.[index];
            if (opt != null) {
                opt.label = value || {};
                // eslint-disable-next-line no-underscore-dangle
                if (Array.isArray(opt._defaulted)) {
                    // Always from active-language as we only update one lang at a time
                    // eslint-disable-next-line no-underscore-dangle
                    const idx = opt._defaulted.indexOf(this.activeLanguage());
                    if (idx > -1) {
                        // eslint-disable-next-line no-underscore-dangle
                        opt._defaulted.splice(idx, 1);
                    }
                }
            }
            return data;
        });
    }
}
