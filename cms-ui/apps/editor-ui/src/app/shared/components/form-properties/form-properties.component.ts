/* eslint-disable @typescript-eslint/naming-convention */
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
    OnInit,
    SimpleChanges,
} from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import {
    EditableFormData,
    EditableFormProperties,
    Form,
    FormTypeConfiguration,
    I18nString,
    ItemInNode,
    Language,
    MarkupLanguageType,
    Page,
    PageRequestOptions,
    Raw,
} from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
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
    public loadedMailTemplate: Page | null = null;

    public useInternalSuccessPage: boolean;
    public loadedSuccessPage: Page | null = null;

    public formLanguages: Language[] = [];
    public activeLanguage: Language | null = null;

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
        }
    }

    protected override initializeWithData(): void {
        super.initializeWithData();

        if (Number.isInteger(this.item?.data?.successPageId) && this.item.data.successPageId !== 0) {
            const options: PageRequestOptions = {};
            if (Number.isInteger(this.item.data?.successNodeId) && this.item.data.successNodeId !== 0) {
                options.nodeId = this.item.data.successNodeId;
            }

            this.subscriptions.push(this.client.page.get(this.item.data.successPageId, options).subscribe((res) => {
                this.loadedSuccessPage = res.page;
                this.changeDetector.markForCheck();
            }));
        }

        if (Number.isInteger(this.item?.data?.adminEmailPageId) && this.item.data.adminEmailPageId !== 0) {
            const options: PageRequestOptions = {};
            if (Number.isInteger(this.item?.data?.adminEmailNodeId) && this.item.data.adminEmailNodeId !== 0) {
                options.nodeId = this.item.data.adminEmailNodeId;
            }

            this.subscriptions.push(this.client.page.get(this.item.data.adminEmailPageId, options).subscribe((res) => {
                this.loadedMailTemplate = res.page;
                this.changeDetector.markForCheck();
            }));
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

        this.configureForm(this.form.value as any, false);
    }

    protected createForm(): FormGroup<FormProperties<FormPropertiesData>> {
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
