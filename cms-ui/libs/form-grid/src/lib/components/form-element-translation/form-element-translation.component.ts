import { ChangeDetectionStrategy, Component, computed, effect, input, model, output, signal } from '@angular/core';
import {
    FormElement,
    FormElementConfiguration,
    FormOptionsSetting,
    FormSchemaProperty,
    FormSelectOption,
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

    public element = model.required<FormElement>();
    public elementConfig = input.required<FormElementConfiguration>();
    public elementSchema = model<FormSchemaProperty | null>(null);
    public languages = input.required<string[]>();
    public mode = input.required<FormGridEditMode>();

    public hasMissingTranslationsChange = output<boolean>();

    public selectedLanguage = signal<string>('');

    public translationSettings = computed(() =>
        this.elementConfig().settings?.filter((s) => s.type === FormSettingType.TRANSLATION) ?? [],
    );

    public staticOptions = computed<FormSelectOptionValue[]>(() =>
        this.elementSchema()?.staticOptions ?? [],
    );

    public optionsSettings = computed<FormOptionsSetting[]>(() =>
        (this.elementConfig().settings || [])
            .filter((setting) => setting.type === FormSettingType.OPTIONS),
    );

    public missingByLanguage = computed<Record<string, boolean>>(() => {
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

    public updateTranslationSetting(id: string, value: I18nString | null): void {
        this.element.update((el) => {
            const copy = structuredClone(el);
            (copy.formGridOptions as any)[id] = value ?? {};
            return copy;
        });
    }

    public updateStaticOptionLabel(index: number, value: I18nString | null): void {
        const current = this.elementSchema();
        if (current == null) {
            return;
        }
        const opts: FormSelectOptionValue[] = structuredClone(current.staticOptions ?? []);
        opts[index].label = value || {};
        this.elementSchema.set({ ...current, staticOptions: opts });
    }

    public updateOptionLabel(settingId: string, index: number, value: I18nString | null): void {
        this.element.update((el) => {
            const copy = structuredClone(el);
            const opt: FormSelectOptionValue | null = (copy.formGridOptions as any)?.[settingId]?.[index];
            if (opt != null) {
                opt.label = value || {};
            }
            return copy;
        });
    }

    private isMissingForLanguage(lang: string): boolean {
        const el = this.element();
        if (!el.label?.[lang]) {
            return true;
        }
        if (el.description != null && !el.description?.[lang]) {
            return true;
        }
        if (this.optionsSettings().some((setting) => {
            const options: FormSelectOption[] = (el.formGridOptions as any)?.[setting.id] || [];
            return options.some((option) => !option.labelI18n[lang]);
        })) {
            return true;
        }
        if (this.translationSettings().some((s) => !(el.formGridOptions as any)?.[s.id]?.[lang])) {
            return true;
        }
        return false;
    }
}
