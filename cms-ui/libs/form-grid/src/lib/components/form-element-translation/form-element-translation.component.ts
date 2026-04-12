import { ChangeDetectionStrategy, Component, computed, effect, input, model, output } from '@angular/core';
import {
    FormElement,
    FormElementConfiguration,
    FormSchemaProperty,
    FormSettingType,
    FormStaticOption,
    I18nString,
} from '@gentics/cms-models';

@Component({
    selector: 'gtx-form-element-translation',
    templateUrl: './form-element-translation.component.html',
    styleUrls: ['./form-element-translation.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormElementTranslationComponent {

    public element = model.required<FormElement>();
    public elementConfig = input.required<FormElementConfiguration>();
    public schema = input<Partial<FormSchemaProperty> | null>(null);
    public languages = input.required<string[]>();
    public restricted = input.required<boolean>();

    public hasMissingTranslationsChange = output<boolean>();
    public schemaChange = output<Partial<FormSchemaProperty>>();

    public effectiveDefaultLanguage = computed(() =>
        this.element().defaultLanguage ?? this.languages()[0] ?? null,
    );

    public translationSettings = computed(() =>
        this.elementConfig().settings?.filter((s) => s.type === FormSettingType.TRANSLATION) ?? [],
    );

    public staticOptions = computed<FormStaticOption[]>(() =>
        this.schema()?.staticOptions ?? [],
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
            const missing = Object.values(this.missingByLanguage()).some(Boolean);
            this.hasMissingTranslationsChange.emit(missing);
        });
    }

    public updateDefaultLanguage(value: string | number | (string | number)[] | null): void {
        const lang = Array.isArray(value) ? value[0] : value;
        this.element.update((el) => ({ ...el, defaultLanguage: lang != null ? String(lang) : undefined }));
    }

    public updateFallbackText(value: string | number | null): void {
        this.element.update((el) => ({ ...el, fallbackText: value != null ? String(value) : undefined }));
    }

    public updateUseFallbackLanguage(checked: boolean): void {
        this.element.update((el) => ({ ...el, useFallbackLanguage: checked }));
    }

    public updateLabelForLanguage(lang: string, value: string | number | null): void {
        this.element.update((el) => ({
            ...el,
            label: { ...el.label, [lang]: value != null ? String(value) : '' },
        }));
    }

    public updateDescriptionForLanguage(lang: string, value: string | number | null): void {
        this.element.update((el) => ({
            ...el,
            description: { ...el.description, [lang]: value != null ? String(value) : '' },
        }));
    }

    public updateTranslationSettingForLanguage(id: string, lang: string, value: string | number | null): void {
        this.element.update((el) => {
            const copy = structuredClone(el);
            const current: I18nString = (copy.formGridOptions as any)?.[id] ?? {};
            (copy.formGridOptions as any)[id] = { ...current, [lang]: value != null ? String(value) : '' };
            return copy;
        });
    }

    public updateStaticOptionLabelForLanguage(index: number, lang: string, value: string | number | null): void {
        const current = this.schema();
        if (current == null) {
            return;
        }
        const opts = structuredClone(current.staticOptions ?? []);
        opts[index] = { ...opts[index], label: { ...opts[index].label, [lang]: value != null ? String(value) : '' } };
        this.schemaChange.emit({ ...current, staticOptions: opts });
    }

    private isMissingForLanguage(lang: string): boolean {
        const el = this.element();
        if (!el.label?.[lang]) {
            return true;
        }
        if (el.description != null && !el.description?.[lang]) {
            return true;
        }
        if (this.staticOptions().some((opt) => !opt.label?.[lang])) {
            return true;
        }
        if (this.translationSettings().some((s) => !(el.formGridOptions as any)?.[s.id]?.[lang])) {
            return true;
        }
        return false;
    }
}
