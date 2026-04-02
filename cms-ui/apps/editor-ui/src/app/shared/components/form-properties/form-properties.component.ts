/* eslint-disable @typescript-eslint/naming-convention */
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
    OnInit,
} from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import {
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

export interface FormPropertiesData extends Omit<EditableFormProperties, 'schema' | 'uiSchema'> {
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
    public disableLanguageSelect = false;

    @Input()
    public autoUpdateFileName = true;

    @Input()
    public mode: FormPropertiesMode = FormPropertiesMode.EDIT;

    @Input()
    public isMultiLang: boolean;

    @Input()
    public showDetailProperties = false;

    public formTypeConfigurations: Record<string, FormTypeConfiguration> | null = null;
    public activeConfiguration: FormTypeConfiguration | null = null;

    public useEmailPageTemplate: boolean;
    public loadedMailTemplate: Page | null = null;
    public mailTemplateBreadcrubs = '';

    public useInternalSuccessPage: boolean;
    public loadedSuccessPage: Page | null = null;
    public successPageBreadcrumbs = '';

    constructor(
        changeDetector: ChangeDetectorRef,
        public client: GCMSRestClientService,
    ) {
        super(changeDetector);
    }

    public ngOnInit(): void {
        super.ngOnInit();

        if (this.mode === FormPropertiesMode.CREATE) {
            this.subscriptions.push(this.client.form.listConfigurations({
                nodeId: this.nodeId,
                external: false,
            }).subscribe((res) => {
                this.formTypeConfigurations = {};
                for (const config of res.items) {
                    this.formTypeConfigurations[config.type] = config;
                }
                if (this.form) {
                    this.activeConfiguration = this.formTypeConfigurations[this.form.value.formType];
                }

                this.changeDetector.markForCheck();
            }));
        }
    }

    protected override initializeWithData(): void {
        super.initializeWithData();

        if (Number.isInteger(this.item?.successPageId) && this.item.successPageId !== 0) {
            const options: PageRequestOptions = {};
            if (Number.isInteger(this.item.successNodeId) && this.item.successNodeId !== 0) {
                options.nodeId = this.item.successNodeId;
            }

            this.subscriptions.push(this.client.page.get(this.item.successPageId, options).subscribe((res) => {
                this.loadedSuccessPage = res.page;
                this.changeDetector.markForCheck();
            }));
        }

        if (Number.isInteger(this.item?.adminEmailPageId) && this.item.adminEmailPageId !== 0) {
            const options: PageRequestOptions = {};
            if (Number.isInteger(this.item.adminEmailNodeId) && this.item.adminEmailNodeId !== 0) {
                options.nodeId = this.item.adminEmailNodeId;
            }

            this.subscriptions.push(this.client.page.get(this.item.adminEmailPageId, options).subscribe((res) => {
                this.loadedMailTemplate = res.page;
                this.changeDetector.markForCheck();
            }));
        }

        // set initial value for useInternalSuccessPage and useEmailPage radio button group
        this.useInternalSuccessPage = this.isPageUsed(
            this.item?.successPageId,
            this.item?.successNodeId,
            this.item?.successUrlI18n,
        );
        this.useEmailPageTemplate = this.isPageUsed(
            this.item?.adminEmailPageId,
            this.item?.adminEmailNodeId,
            this.item?.adminEmailTemplate,
        );
    }

    protected createForm(): FormGroup<FormProperties<FormPropertiesData>> {
        return new FormGroup<FormProperties<FormPropertiesData>>({
            name: new FormControl(this.safeValue('name'), Validators.required),
            formType: new FormControl(this.safeValue('formType'), Validators.required),
            description: new FormControl(this.safeValue('description')),
            languages: new FormControl(this.safeValue('languages') || [], Validators.minLength(1)),
            fileName: new FormControl(this.safeValue('fileName')),
            flow: new FormControl(this.safeValue('flow')),
            templateContext: new FormControl(this.safeValue('templateContext')),
            successUrlI18n: new FormControl(this.safeValue('successUrlI18n')),
            successPageId: new FormControl(this.safeValue('successPageId')),
            successNodeId: new FormControl(this.safeValue('successNodeId')),
            adminEmailAddress: new FormControl(this.safeValue('adminEmailAddress')),
            adminEmailSubject: new FormControl(this.safeValue('adminEmailSubject')),
            adminEmailPageId: new FormControl(this.safeValue('adminEmailPageId')),
            adminEmailNodeId: new FormControl(this.safeValue('adminEmailNodeId')),
            adminEmailTemplate: new FormControl(this.safeValue('adminEmailTemplate')),
        });
    }

    protected configureForm(value: FormPropertiesData, loud?: boolean): void {
        setControlsEnabled(this.form, ['formType'], this.mode === FormPropertiesMode.CREATE);
        if (this.formTypeConfigurations != null) {
            this.activeConfiguration = this.formTypeConfigurations[value.formType];
        }
    }

    protected assembleValue(value: FormPropertiesData): FormPropertiesData {
        if (this.activeConfiguration?.flows?.length > 0 && value.flow == null) {
            value.flow = this.activeConfiguration.flows[0].id;
        }

        return value;
    }

    updateEmailTemplate(doUse: boolean): void {
        this.useEmailPageTemplate = doUse;
        this.form.markAsDirty();

        setControlsEnabled(this.form, ['adminEmailPageId', 'adminEmailNodeId'], doUse);
        setControlsEnabled(this.form, ['adminEmailTemplate'], !doUse);
    }

    updateInternalSuccessPage(doUse: boolean): void {
        this.useInternalSuccessPage = doUse;
        this.form.markAsDirty();

        setControlsEnabled(this.form, ['successPageId', 'successNodeId'], doUse);
        setControlsEnabled(this.form, ['successUrlI18n'], !doUse);
    }

    setSuccessPage(page: ItemInNode<Page<Raw>>): void {
        this.loadedSuccessPage = page;

        const pageId = Number.isInteger(page?.id) ? page.id : 0;
        const nodeId = Number.isInteger(page?.nodeId) ? page.nodeId : 0;

        this.form.controls.successPageId.setValue(pageId, { emitEvent: false });
        this.form.controls.successPageId.markAsTouched({ emitEvent: false });
        this.form.controls.successPageId.markAsDirty({ emitEvent: false });

        this.form.controls.successNodeId.setValue(nodeId, { emitEvent: false });
        this.form.controls.successNodeId.markAsTouched({ emitEvent: false });
        this.form.controls.successNodeId.markAsDirty({ emitEvent: false });

        this.form.updateValueAndValidity();
        this.changeDetector.markForCheck();
    }

    setEmailTemplatePage(page: ItemInNode<Page<Raw>>): void {
        this.loadedMailTemplate = page;

        const pageId = Number.isInteger(page?.id) ? page.id : 0;
        const nodeId = Number.isInteger(page?.nodeId) ? page.nodeId : 0;

        this.form.controls.adminEmailPageId.setValue(pageId, { emitEvent: false });
        this.form.controls.adminEmailPageId.markAsTouched({ emitEvent: false });
        this.form.controls.adminEmailPageId.markAsDirty({ emitEvent: false });

        this.form.controls.adminEmailNodeId.setValue(nodeId, { emitEvent: false });
        this.form.controls.adminEmailNodeId.markAsTouched({ emitEvent: false });
        this.form.controls.adminEmailNodeId.markAsDirty({ emitEvent: false });

        this.form.updateValueAndValidity();
        this.changeDetector.markForCheck();
    }

    private isPageUsed(pageId: number, nodeId: number, other: I18nString): boolean {
        return (pageId != null && pageId !== 0 && nodeId != null && nodeId !== 0) || !other;
    }
}
