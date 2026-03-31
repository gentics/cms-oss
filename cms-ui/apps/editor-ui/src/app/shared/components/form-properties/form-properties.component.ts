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
import { RepositoryBrowserClient } from '../../providers';

export enum FormPropertiesMode {
    CREATE = 'create',
    EDIT = 'edit',
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
    extends BaseFormPropertiesComponent<Omit<EditableFormProperties, 'schema' | 'uiSchema'>>
    implements OnInit, OnChanges {

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
        public repoBrowser: RepositoryBrowserClient,
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
            if (this.form) {
                this.activeConfiguration = this.formTypeConfigurations[this.form.value.formType];
            }

            this.changeDetector.markForCheck();
        }));

        if (typeof this.value?.successPageId === 'number' && this.value?.successPageId !== 0) {
            this.subscriptions.push(this.client.page.get(this.value?.successPageId, {
                nodeId: this.value?.successNodeId,
            }).subscribe((res) => {
                this.loadedSuccessPage = res.page;
                this.successPageBreadcrumbs = this.generateBreadcrumbsPath(res.page as any);
                this.changeDetector.markForCheck();
            }));
        }

        if (Number.isInteger(this.value?.adminEmailPageId) && this.value.adminEmailPageId !== 0) {
            this.subscriptions.push(this.client.page.get(this.value.adminEmailPageId, {
                nodeId: this.value.adminEmailNodeId,
            }).subscribe((res) => {
                this.loadedMailTemplate = res.page;
                this.mailTemplateBreadcrubs = this.generateBreadcrumbsPath(res.page as any);
                this.changeDetector.markForCheck();
            }));
        }

        // set initial value for useInternalSuccessPage and useEmailPage radio button group
        this.useInternalSuccessPage = this.isPageUsed(
            this.value?.successPageId,
            this.value?.successNodeId,
            this.value?.successUrlI18n,
        );
        this.useEmailPageTemplate = this.isPageUsed(
            this.value?.adminEmailPageId,
            this.value?.adminEmailNodeId,
            this.value?.adminEmailTemplate,
        );
    }

    protected createForm(): FormGroup<FormProperties<Omit<EditableFormProperties, 'schema' | 'uiSchema'>>> {
        return new FormGroup<FormProperties<Omit<EditableFormProperties, 'schema' | 'uiSchema'>>>({
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

    protected configureForm(value: Omit<EditableFormProperties, 'schema' | 'uiSchema'>, loud?: boolean): void {
        if (this.formTypeConfigurations != null) {
            this.activeConfiguration = this.formTypeConfigurations[value.formType];
        }
    }

    protected assembleValue(value: Omit<EditableFormProperties, 'schema' | 'uiSchema'>): Omit<EditableFormProperties, 'schema' | 'uiSchema'> {
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

    /**
     * Opens the repository browser to allow the user to select an internal page.
     */
    browseForPage(): void {
        this.repoBrowser.openRepositoryBrowser({
            allowedSelection: 'page',
            selectMultiple: false,
        })
            .then((selectedPage: ItemInNode<Page<Raw>>) => {
                this.setSuccessPage(selectedPage);
            });
    }

    browseForEmailTemplatePage(): void {
        const browserNodeId = this.form.controls.adminEmailNodeId.value || this.nodeId;

        this.repoBrowser.openRepositoryBrowser({
            allowedSelection: 'page',
            selectMultiple: false,
            includeMlId: [MarkupLanguageType.FormsEmailTemplate],
            title: 'modal.repository_browser_title_forms_email_template_single',
            startNode: browserNodeId,
        })
            .then((selectedTemplatePage: ItemInNode<Page<Raw>>) => {
                this.setEmailTemplatePage(selectedTemplatePage);
            });
    }

    setSuccessPage(page: ItemInNode<Page<Raw>>): void {
        this.loadedSuccessPage = page;
        this.successPageBreadcrumbs = this.generateBreadcrumbsPath(page);

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
        this.mailTemplateBreadcrubs = this.generateBreadcrumbsPath(page);

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

    /**
     * @returns A string with the breadcrumbs path of the specified Page.
     */
    private generateBreadcrumbsPath(page: ItemInNode<Page<Raw>>): string {
        let breadcrumbsPath = '';
        if (page && page.path) {
            breadcrumbsPath = page.path.replace('/', '');
            if (breadcrumbsPath.length > 0 && breadcrumbsPath.charAt(breadcrumbsPath.length - 1) === '/') {
                breadcrumbsPath = breadcrumbsPath.substring(0, breadcrumbsPath.length - 1);
            }
            breadcrumbsPath = breadcrumbsPath.split('/').join(' > ');
        }
        return breadcrumbsPath;
    }

    private isPageUsed(pageId: number, nodeId: number, other: I18nString): boolean {
        return (pageId != null && pageId !== 0 && nodeId != null && nodeId !== 0) || !other;
    }
}
