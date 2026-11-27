import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
    OnInit,
    SimpleChanges,
} from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import {
    EditableFormData,
    EditableFormProperties,
    Form,
    FormElement,
    FormElementConfiguration,
    FormSettingConfiguration,
    FormSettingType,
    FormTypeConfiguration,
    I18nString,
    ItemInNode,
    ItemRef,
    Language,
    MarkupLanguageType,
    Page,
    Raw,
} from '@gentics/cms-models';
import { getValueByPath } from '@gentics/common';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { isSettingVisible } from '@gentics/form-grid';
import {
    BaseFormPropertiesComponent,
    FormProperties,
    generateFormProvider,
    generateValidatorProvider,
    setControlsEnabled,
} from '@gentics/ui-core';

export enum FormPropertiesMode {
    CREATE = 'create',
    EDIT = 'edit',
}

export interface FormPropertiesData extends EditableFormProperties {
    formType: string;
}

/** Default property in the form data, in which the values of the form settings are stored. */
export const FORM_SETTINGS_PROPERTY = 'formSettings';

@Component({
    selector: 'gtx-form-properties',
    templateUrl: './form-properties.component.html',
    styleUrls: ['./form-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(FormPropertiesComponent),
        generateValidatorProvider(FormPropertiesComponent),
    ],
    standalone: false,
})
export class FormPropertiesComponent
    extends BaseFormPropertiesComponent<FormPropertiesData>
    implements OnInit, OnChanges {

    public readonly MarkupLanguageType = MarkupLanguageType;

    @Input()
    public nodeId: number;

    @Input()
    public folderId: number;

    @Input()
    public item: Form;

    @Input()
    public languages: Language[];

    @Input()
    public configuration: FormTypeConfiguration;

    @Input()
    public disableLanguageSelect = false;

    @Input()
    public mode: FormPropertiesMode = FormPropertiesMode.EDIT;

    @Input()
    public isMultiLang: boolean;

    public formData: FormGroup<FormProperties<Partial<EditableFormData>>>;

    public formTypeConfigurations: Record<string, FormTypeConfiguration> | null = null;
    public activeConfiguration: FormTypeConfiguration | null = null;

    public useEmailPageTemplate: boolean;
    public loadedMailTemplate: ItemRef | null = null;

    public useInternalSuccessPage: boolean;
    public loadedSuccessPage: ItemRef | null = null;

    public formLanguages: Language[] = [];
    public activeLanguage: Language | null = null;

    /**
     * The form settings of the active configuration. The `propertyPath` is always set (relative to the form data),
     * so that the generic settings components store the values in the correct place of the form data.
     */
    public formSettings: FormSettingConfiguration[] = [];
    /** Pseudo element configuration for the form settings, required by the generic settings components. */
    public formSettingsConfig: FormElementConfiguration | null = null;
    /** Copy of the form data, which is edited by the generic settings components. */
    public formSettingsData: FormElement | null = null;
    /** Context ID for the generic settings components. Changes whenever the settings data is re-initialized. */
    public formSettingsContextId = '';
    /** If any of the form settings has translatable content. */
    public hasTranslatableFormSettings = false;

    /** Top-level properties of the form data, which have been added as controls for the form settings. */
    private formSettingsControlNames: string[] = [];
    private formSettingsResetCounter = 0;

    protected override delayedSetup = true;

    constructor(
        changeDetector: ChangeDetectorRef,
        public client: GCMSRestClientService,
    ) {
        super(changeDetector);
    }

    public ngOnInit(): void {
        super.ngOnInit();

        this.subscriptions.push(this.client.form.listConfigurations({
            nodeId: this.nodeId,
            external: false,
        }).subscribe((res) => {
            this.formTypeConfigurations = {};
            for (const config of res.items) {
                this.formTypeConfigurations[config.type] = config;
            }

            this.changeDetector.markForCheck();
        }));
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.configuration) {
            this.activeConfiguration = this.configuration;
            this.setupFormSettings();
        }
    }

    protected override initializeWithData(): void {
        super.initializeWithData();

        if (Number.isInteger(this.item?.data?.successPageId) && this.item.data.successPageId !== 0) {
            this.loadedSuccessPage = {
                type: 'page',
                id: this.item.data.successPageId,
                nodeId: this.item.data.successNodeId,
            } as any;
            this.changeDetector.markForCheck();
        }

        if (Number.isInteger(this.item?.data?.adminEmailPageId) && this.item.data.adminEmailPageId !== 0) {
            this.loadedMailTemplate = {
                type: 'page',
                id: this.item.data.adminEmailPageId,
                nodeId: this.item.data.adminEmailNodeId,
            } as any;
        }

        // set initial value for useInternalSuccessPage and useEmailPage radio button group
        this.useInternalSuccessPage = this.isPageUsed(
            this.item?.data?.successPageId,
            this.item?.data?.successNodeId,
            this.item?.data?.successUrlI18n,
        );
        this.useEmailPageTemplate = this.isPageUsed(
            this.item?.data?.adminEmailPageId,
            this.item?.data?.adminEmailNodeId,
            this.item?.data?.adminEmailTemplate,
        );

        this.setupFormSettings();
        this.configureForm(this.form.value as any, false);
    }

    protected override onValueReset(): void {
        super.onValueReset();
        this.setupFormSettings();
    }

    protected createForm(): FormGroup<FormProperties<FormPropertiesData>> {
        // Clear, as the form is created from scratch
        this.formSettingsControlNames = [];

        this.formData = new FormGroup<FormProperties<Partial<EditableFormData>>>({
            formWidth: new FormControl(this.item?.data?.formWidth ?? this.safeValue(['data', 'formWidth']) ?? 12),
            flowId: new FormControl(this.item?.data?.flowId || this.safeValue(['data', 'flowId'])),
            captchaId: new FormControl(this.item?.data.flowId || this.safeValue(['data', 'captchaId'])),
            templateContext: new FormControl(this.item?.data?.templateContext || this.safeValue(['data', 'templateContext'])),
            successUrlI18n: new FormControl(this.item?.data?.successUrlI18n || this.safeValue(['data', 'successUrlI18n'])),
            successPageId: new FormControl(this.item?.data?.successPageId || this.safeValue(['data', 'successPageId'])),
            successNodeId: new FormControl(this.item?.data?.successNodeId || this.safeValue(['data', 'successNodeId'])),
            adminEmailAddress: new FormControl(this.item?.data?.adminEmailAddress || this.safeValue(['data', 'adminEmailAddress'])),
            adminEmailSubject: new FormControl(this.item?.data?.adminEmailSubject || this.safeValue(['data', 'adminEmailSubject'])),
            adminEmailPageId: new FormControl(this.item?.data?.adminEmailPageId || this.safeValue(['data', 'adminEmailPageId'])),
            adminEmailNodeId: new FormControl(this.item?.data?.adminEmailNodeId || this.safeValue(['data', 'adminEmailNodeId'])),
            adminEmailTemplate: new FormControl(this.item?.data?.adminEmailTemplate || this.safeValue(['data', 'adminEmailTemplate'])),
        }, {
            validators: (ctl) => this.validateFormSettings(ctl),
        });

        return new FormGroup<FormProperties<FormPropertiesData>>({
            name: new FormControl(this.item?.name || this.safeValue('name'), Validators.required),
            formType: new FormControl(this.item?.formType || this.safeValue('formType'), Validators.required),
            description: new FormControl(this.item?.description || this.safeValue('description')),
            languages: new FormControl(this.item?.languages || this.safeValue('languages') || [], Validators.minLength(1)),
            data: this.formData,
        });
    }

    protected configureForm(value: FormPropertiesData, loud?: boolean): void {
        // Should be enabled while it's still loading, and if the name isn't in the item (i.E. something has already been selected)
        setControlsEnabled(this.form, ['formType'], this.mode !== FormPropertiesMode.EDIT || this.configuration == null, { emitEvent: loud });

        const selectedLangs = (value?.languages || []);
        this.formLanguages = (this.languages || []).filter((lang) => selectedLangs.includes(lang.code));
        if (this.formLanguages.length > 0 && this.activeLanguage == null) {
            this.activeLanguage = this.formLanguages[0];
        }
        this.changeDetector.markForCheck();
    }

    protected assembleValue(value: FormPropertiesData): FormPropertiesData {
        if (this.activeConfiguration?.flows?.length > 0 && value.data?.flowId == null) {
            if (value.data == null) {
                value.data = {};
            }
            value.data.flowId = this.activeConfiguration.flows[0].id;
        }

        return value;
    }

    public setActiveFormType(type: string): void {
        this.activeConfiguration = this.formTypeConfigurations[type];
        this.setupFormSettings();
    }

    /**
     * Prepares the form settings of the active configuration: Creates the controls in the data form-group
     * for all top-level properties the settings are stored in, and initializes the data for the settings components.
     */
    protected setupFormSettings(): void {
        if (this.formData == null) {
            return;
        }

        const settings = this.activeConfiguration?.settings || [];
        this.formSettings = settings.map((setting) => ({
            ...setting,
            propertyPath: setting.propertyPath || `${FORM_SETTINGS_PROPERTY}.${setting.id}`,
            // There is no schema for the form, therefore all values are stored in the form data
            backend: false,
        }));
        this.formSettingsConfig = {
            labelI18n: this.activeConfiguration?.nameI18n || {},
            settings: this.formSettings,
        };
        this.hasTranslatableFormSettings = this.formSettings.some((setting) =>
            setting.type === FormSettingType.TRANSLATION || setting.type === FormSettingType.OPTIONS);

        const controlNames = Array.from(new Set(this.formSettings.map((setting) => setting.propertyPath.split('.')[0])));

        // Remove controls of settings from a previously selected form type
        for (const name of this.formSettingsControlNames) {
            if (!controlNames.includes(name)) {
                (this.formData as FormGroup).removeControl(name, { emitEvent: false });
            }
        }

        // Add controls for the properties, which aren't present yet
        const currentData = this.getCurrentFormData();
        for (const name of controlNames) {
            if (!this.formData.contains(name)) {
                (this.formData as FormGroup).addControl(name, new FormControl(structuredClone(currentData[name] ?? null)), { emitEvent: false });
                this.formSettingsControlNames.push(name);
            }
        }
        this.formSettingsControlNames = this.formSettingsControlNames.filter((name) => controlNames.includes(name));

        const settingsData: Record<string, unknown> = {};
        for (const name of controlNames) {
            settingsData[name] = structuredClone(this.formData.get(name).value ?? null);
        }
        this.formSettingsData = settingsData as any;
        this.formSettingsResetCounter++;
        this.formSettingsContextId = `${this.activeConfiguration?.type ?? ''}:${this.formSettingsResetCounter}`;

        this.formData.updateValueAndValidity({ emitEvent: false });
        this.changeDetector.markForCheck();
    }

    /**
     * Handler for when the generic settings components changed the values of the form settings.
     */
    public updateFormSettingsData(data: FormElement): void {
        this.formSettingsData = data;

        for (const name of this.formSettingsControlNames) {
            const ctl = this.formData.get(name);
            ctl.setValue(structuredClone(data?.[name] ?? null), { emitEvent: false });
            ctl.markAsDirty();
        }

        this.form.markAsDirty();
        this.formData.updateValueAndValidity();
        this.changeDetector.markForCheck();
    }

    /**
     * Validator for the data form-group, which checks if all visible and required form settings have a value.
     */
    protected validateFormSettings(ctl: AbstractControl): ValidationErrors | null {
        if (!this.formSettings?.length || ctl.disabled) {
            return null;
        }

        const data = (ctl as FormGroup).getRawValue();
        const missing = this.formSettings
            .filter((setting) => setting.required
                && isSettingVisible(setting, this.formSettingsConfig, data)
                && isEmptySettingValue(getValueByPath(data, setting.propertyPath)))
            .map((setting) => setting.id);

        return missing.length > 0 ? { formSettingsRequired: missing } : null;
    }

    /**
     * Get the current form data, from the item, or the value.
     */
    private getCurrentFormData(): Record<string, any> {
        return (this.item?.data ?? this.safeValue('data') ?? {}) as Record<string, any>;
    }

    updateEmailTemplate(doUse: boolean): void {
        this.useEmailPageTemplate = doUse;

        setControlsEnabled(this.formData, ['adminEmailPageId', 'adminEmailNodeId'], doUse, { emitEvent: false });
        setControlsEnabled(this.formData, ['adminEmailTemplate'], !doUse, { emitEvent: false });

        this.form.markAsDirty();
        this.form.updateValueAndValidity({ emitEvent: true });
        this.changeDetector.markForCheck();
    }

    updateInternalSuccessPage(doUse: boolean): void {
        this.useInternalSuccessPage = doUse;

        setControlsEnabled(this.formData, ['successPageId', 'successNodeId'], doUse, { emitEvent: false });
        setControlsEnabled(this.formData, ['successUrlI18n'], !doUse, { emitEvent: false });

        this.form.markAsDirty();
        this.form.updateValueAndValidity({ emitEvent: true });
        this.changeDetector.markForCheck();
    }

    setSuccessPage(page: ItemInNode<Page<Raw>>): void {
        this.loadedSuccessPage = page;

        const pageId = Number.isInteger(page?.id) ? page.id : 0;
        const nodeId = Number.isInteger(page?.nodeId) ? page.nodeId : 0;

        this.formData.controls.successPageId.setValue(pageId, { emitEvent: false });
        this.formData.controls.successPageId.markAsTouched({ emitEvent: false });
        this.formData.controls.successPageId.markAsDirty({ emitEvent: false });

        this.formData.controls.successNodeId.setValue(nodeId, { emitEvent: false });
        this.formData.controls.successNodeId.markAsTouched({ emitEvent: false });
        this.formData.controls.successNodeId.markAsDirty({ emitEvent: false });

        this.form.updateValueAndValidity();
        this.changeDetector.markForCheck();
    }

    setEmailTemplatePage(page: ItemInNode<Page<Raw>>): void {
        this.loadedMailTemplate = page;

        const pageId = Number.isInteger(page?.id) ? page.id : 0;
        const nodeId = Number.isInteger(page?.nodeId) ? page.nodeId : 0;

        this.formData.controls.adminEmailPageId.setValue(pageId, { emitEvent: false });
        this.formData.controls.adminEmailPageId.markAsTouched({ emitEvent: false });
        this.formData.controls.adminEmailPageId.markAsDirty({ emitEvent: false });

        this.formData.controls.adminEmailNodeId.setValue(nodeId, { emitEvent: false });
        this.formData.controls.adminEmailNodeId.markAsTouched({ emitEvent: false });
        this.formData.controls.adminEmailNodeId.markAsDirty({ emitEvent: false });

        this.form.updateValueAndValidity();
        this.changeDetector.markForCheck();
    }

    private isPageUsed(pageId: number, nodeId: number, other: I18nString | string): boolean {
        return (pageId != null && pageId !== 0 && nodeId != null && nodeId !== 0) || !other;
    }
}

function isEmptySettingValue(value: unknown): boolean {
    if (value == null || value === '') {
        return true;
    }
    if (Array.isArray(value)) {
        return value.length === 0;
    }
    if (typeof value === 'object') {
        return Object.values(value).every((inner) => inner == null || inner === '');
    }
    return false;
}
