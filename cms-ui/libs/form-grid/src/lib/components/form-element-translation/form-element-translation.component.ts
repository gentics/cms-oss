import { ChangeDetectionStrategy, Component, computed, effect, input, model, output, signal } from '@angular/core';
import {
    FormElement,
    FormElementConfiguration,
    FormSchemaProperty,
    FormSelectOptionValue,
    FormSettingType,
    I18nString,
} from '@gentics/cms-models';
import { FormGridEditMode } from '../../models';

@Component({
    selector: 'gtx-form-element-translation',
    templateUrl: './form-element-translation.component.html',
    styleUrls: ['./form-element-translation.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormElementTranslationComponent {

    public readonly FormGridEditMode = FormGridEditMode;

    public readonly mode = input.required<FormGridEditMode>();

    public readonly element = model.required<FormElement>();
    public readonly elementConfig = input.required<FormElementConfiguration>();
    public readonly elementSchema = model<FormSchemaProperty>();

    public readonly languages = input.required<string[]>();

    public readonly hasMissingTranslationsChange = output<boolean>();

    public readonly selectedLanguage = signal<string>('');

    public readonly translationSettings = computed(() => {
        const all = this.elementConfig().settings || [];
        return all.filter((setting) => (
            setting.type === FormSettingType.TRANSLATION
            || setting.type === FormSettingType.OPTIONS
        ));
    });

    public readonly elementSettings = computed(() => {
        return this.translationSettings().filter((setting) => !setting.backend);
    });

    public readonly schemaSettings = computed(() => {
        return this.translationSettings().filter((setting) => setting.backend);
    });

    public readonly missingByLanguage = computed<Record<string, boolean>>(() => {
        const result: Record<string, boolean> = {};
        for (const lang of this.languages()) {
            result[lang] = this.isMissingForLanguage(lang);
        }
        return result;
    });

    constructor() {
        effect(() => {
            const langs = this.languages();
            if (langs.length > 0 && !langs.includes(this.selectedLanguage())) {
                this.selectedLanguage.set(langs[0]);
            }
        });

        effect(() => {
            const missing = Object.values(this.missingByLanguage()).some(Boolean);
            this.hasMissingTranslationsChange.emit(missing);
        });
    }

    public updateSelectedLanguage(value: string | number | (string | number)[] | null): void {
        const lang = Array.isArray(value) ? value[0] : value;
        if (lang != null) {
            this.selectedLanguage.set(String(lang));
        }
    }

    public updateLabel(value: I18nString | null): void {
        this.element.update((el) => ({ ...el, label: value ?? {} }));
    }

    public updateDescription(value: I18nString | null): void {
        this.element.update((el) => ({ ...el, description: value ?? undefined }));
    }

    public updateSummary(value: I18nString | null): void {
        this.element.update((el) => ({
            ...el,
            formGridOptions: { ...el.formGridOptions, valueSummary: value ?? undefined } as any,
        }));
    }

    private isMissingForLanguage(lang: string): boolean {
        const el = this.element();
        if (!el.label?.[lang]) {
            return true;
        }
        if (el.description != null && !el.description?.[lang]) {
            return true;
        }

        const elementOptions = this.element().formGridOptions || {};
        const schemaOptions = this.elementSchema()?.formGridOptions || {};

        return (this.elementConfig().settings || []).some((setting) => {
            const effectiveOptions: Record<string, any> = setting.backend ? schemaOptions : elementOptions;
            if (setting.type === FormSettingType.OPTIONS) {
                const options: FormSelectOptionValue[] = effectiveOptions?.[setting.id] || [];
                return options.some((option) => !option?.label?.[lang]);
            } else if (setting.type === FormSettingType.TRANSLATION) {
                return !effectiveOptions[setting.id]?.[lang];
            } else {
                return false;
            }
        });
    }
}
