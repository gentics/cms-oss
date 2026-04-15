import { ChangeDetectionStrategy, Component, computed, HostBinding, input, model } from '@angular/core';
import { FormSelectOptionValue, FormSettingConfiguration, I18nString } from '@gentics/cms-models';

@Component({
    selector: 'gtx-dynamic-form-translations',
    templateUrl: './dynamic-form-translations.component.html',
    styleUrls: ['./dynamic-form-translations.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class DynamicFormTranslationsComponent {

    public readonly settings = input.required<FormSettingConfiguration[]>();
    public readonly data = model.required<Record<string, any>>();

    public readonly languages = input.required<string[]>();
    public readonly selectedLanguage = input.required<string>();

    public readonly disabled = input.required<boolean>();

    public readonly visibleSettings = computed(() => {
        // TODO: Filter conditions
        return this.settings();
    });

    @HostBinding('class.has-settings')
    public readonly hasVisibleSettings = computed(() => this.visibleSettings().length > 0);

    public updateTranslationSetting(id: string, value: I18nString | null): void {
        this.data.update((data) => {
            return {
                ...(data || {}),
                [id]: value,
            };
        });
    }

    public updateOptionLabel(settingId: string, index: number, value: I18nString | null): void {
        this.data.update((data) => {
            const copy = structuredClone(data) || {};
            const opt: FormSelectOptionValue | null = copy?.[settingId]?.[index];
            if (opt != null) {
                opt.label = value || {};
            }
            return copy;
        });
    }
}
