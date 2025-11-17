/* eslint-disable @typescript-eslint/naming-convention */
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
    OnInit,
} from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import {
    CmsFormData,
    CmsFormElementI18nValue,
    CmsFormType,
    EditableFormProps,
    Form,
    ItemInNode,
    Language,
    MarkupLanguageType,
    Page,
    Raw,
} from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import {
    FormEditorConfiguration,
    FormEditorConfigurationService,
    FormEditorService,
    FormElementPropertyOptionConfiguration,
} from '@gentics/form-generator';
import { ChangesOf, FormProperties, generateFormProvider, generateValidatorProvider, setControlsEnabled } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { merge, of } from 'rxjs';
import { distinctUntilChanged, filter, map, mergeMap, switchMap, tap } from 'rxjs/operators';
import { ApplicationStateService } from '../../../state';
import { RepositoryBrowserClient } from '../../providers/repository-browser-client/repository-browser-client.service';

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
    standalone: false
})
export class FormPropertiesComponent
    extends BasePropertiesComponent<EditableFormProps>
    implements OnInit, OnChanges {

    public readonly CMS_FORM_TYPES: CmsFormType[] = Object.values(CmsFormType);

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

    public dataGroup: FormGroup<FormProperties<CmsFormData>>;

    repositoryBrowserItemCache = {};

    public activeFormLanguageCode: string | null = null;
    public formConfigLoaded = false;
    public formConfig: FormEditorConfiguration = null;

    public useEmailPageTemplate: boolean;
    public loadedMailTemplate: Page | null = null;
    public mailTemplateBreadcrubs = '';

    public useInternalSuccessPage: boolean;
    public loadedSuccessPage: Page | null = null;
    public successPageBreadcrumbs = '';

    private previousFormType: CmsFormType | null;

    constructor(
        changeDetector: ChangeDetectorRef,
        private client: GCMSRestClientService,
        private appState: ApplicationStateService,
        private formEditorService: FormEditorService,
        private formEditorConfigurationService: FormEditorConfigurationService,
        private repositoryBrowserClient: RepositoryBrowserClient,
    ) {
        super(changeDetector);
    }

    public override ngOnInit(): void {
        super.ngOnInit();

        this.subscriptions.push(this.appState.select(state => state.folder.activeFormLanguage).pipe(
            mergeMap(activeLanguageId => this.appState.select(state => state.entities.language[activeLanguageId])),
            map(activeLanguage => activeLanguage && activeLanguage.code),
        ).subscribe(activeLanguageCode => {
            /**
             * We need to set the language manually in the form editor service.
             * (This is normally done in the form editor component. However, it is not used here).
             */
            this.formEditorService.activeContentLanguageCode = activeLanguageCode;

            this.activeFormLanguageCode = activeLanguageCode;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.appState.select(state => state.ui.language).subscribe(language => {
            /**
             * We need to set the language manually in the form editor service.
             * (This is normally done in the form editor component. However, it is not used here).
             */
            this.formEditorService.activeUiLanguageCode = language;
        }));
    }

    public override ngOnChanges(changes: ChangesOf<this>): void {
        super.ngOnChanges(changes);

        if (changes.disableLanguageSelect && this.form) {
            setControlsEnabled(this.form, ['languages'], !this.disableLanguageSelect);
        }
    }

    protected override initializeWithData(): void {
        this.subscriptions.push(merge([
            of(this.value?.data?.type ?? CmsFormType.GENERIC),
            this.dataGroup.controls.type.valueChanges,
        ]).pipe(
            switchMap(type => type),
            filter(type => type != null),
            distinctUntilChanged(isEqual),
            filter(type => type !== this.previousFormType),
            tap(type => {
                this.previousFormType = type;
            }),
            switchMap(type => this.formEditorConfigurationService.getConfiguration$(type)),
            filter(config => config != null),
        ).subscribe({
            next: config => {
                this.formConfig = config;
                this.formConfigLoaded = true;
                this.updateTemplateContextValidator(config?.form_properties?.template_context_options);
                this.changeDetector.markForCheck();
            },
            error: err => {
                console.error('Error while loading form configuration', err);
                this.formConfig = null;
                this.formConfigLoaded = true;
                this.updateTemplateContextValidator(this.formConfig?.form_properties?.template_context_options);
                this.changeDetector.markForCheck();
            },
        }));

        if (typeof this.value?.successPageId === 'number' && this.value?.successPageId !== 0) {
            this.subscriptions.push(this.client.page.get(this.value?.successPageId, { nodeId: this.value?.successNodeId }).subscribe(res => {
                this.loadedSuccessPage = res.page;
                this.successPageBreadcrumbs = this.generateBreadcrumbsPath(res.page as any);
                this.changeDetector.markForCheck();
            }));
        }

        if (typeof this.value?.data?.mailsource_pageid === 'number' && this.value?.data?.mailsource_pageid !== 0) {
            this.subscriptions.push(this.client.page.get(this.value.data.mailsource_pageid, { nodeId: this.value.data.mailsource_nodeid }).subscribe(res => {
                this.loadedMailTemplate = res.page;
                this.mailTemplateBreadcrubs = this.generateBreadcrumbsPath(res.page as any);
                this.changeDetector.markForCheck();
            }));
        }

        // set initial value for useInternalSuccessPage and useEmailPage radio button group
        this.useInternalSuccessPage = this.isInternalPageUsed(
            this.value?.successPageId,
            this.value?.successNodeId,
            this.value?.data?.successurl_i18n,
        );
        this.useEmailPageTemplate = this.isEmailTemplateUsed(
            this.value?.data?.mailsource_pageid,
            this.value?.data?.mailsource_nodeid,
            this.value?.data?.mailtemp_i18n,
        );
    }

    protected createForm(): FormGroup {
        const migratedSuccessUrl = this.migrateSuccessUrl(
            this.value?.data?.successurl,
            this.value?.data?.successurl_i18n,
            (this.languages || []).map(lang => lang.code),
        ).successurl_i18n;

        // set up form controls
        this.dataGroup = new FormGroup<FormProperties<CmsFormData>>({
            email: new FormControl(this.safeValue(['data', 'email']) || ''),
            successurl_i18n: new FormControl(migratedSuccessUrl || {}),
            mailsubject_i18n: new FormControl(this.safeValue(['data', 'mailsubject_i18n']) || {}),
            mailsource_pageid: new FormControl(this.safeValue(['data', 'mailsource_pageid']) || 0),
            mailsource_nodeid: new FormControl(this.safeValue(['data', 'mailsource_nodeid']) || 0),
            mailtemp_i18n: new FormControl(this.safeValue(['data', 'mailtemp_i18n']) || {}),
            // set default value
            templateContext: new FormControl(this.safeValue(['data', 'templateContext']) || ''),
            type: new FormControl(this.safeValue(['data', 'type']) || CmsFormType.GENERIC, Validators.required),
            // needs to be stored here, otherwise updating data object without elements will delete whole form
            elements: new FormControl(this.safeValue(['data', 'elements']) || []),
        });

        return new FormGroup<FormProperties<EditableFormProps>>({
            name: new FormControl(this.safeValue('name') || '', Validators.required),
            description: new FormControl(this.safeValue('description') || ''),
            successPageId: new FormControl(this.safeValue('successPageId') || 0),
            successNodeId: new FormControl(this.safeValue('successNodeId') || 0),
            languages: new FormControl({
                value: this.safeValue('languages') || null,
                disabled: this.disableLanguageSelect,
            }),

            data: this.dataGroup,
        });
    }

    protected configureForm(value: EditableFormProps, loud?: boolean): void {

    }

    protected assembleValue(value: EditableFormProps): EditableFormProps {
        return value;
    }

    updateEmailTemplate(doUse: boolean): void {
        this.useEmailPageTemplate = doUse;
        this.form.markAsDirty();

        setControlsEnabled(this.dataGroup, ['mailsource_nodeid', 'mailsource_pageid'], doUse);
        setControlsEnabled(this.dataGroup, ['mailtemp_i18n'], !doUse);
    }

    updateInternalSuccessPage(doUse: boolean): void {
        this.useInternalSuccessPage = doUse;
        this.form.markAsDirty();

        setControlsEnabled(this.form, ['successPageId', 'successNodeId'], doUse);
        setControlsEnabled(this.dataGroup, ['successurl_i18n'], !doUse);
    }

    /**
     * Opens the repository browser to allow the user to select an internal page.
     */
    browseForPage(): void {
        this.repositoryBrowserClient.openRepositoryBrowser({
            allowedSelection: 'page',
            selectMultiple: false,
            contentLanguage: this.activeFormLanguageCode,
        })
            .then((selectedPage: ItemInNode<Page<Raw>>) => {
                this.setSuccessPage(selectedPage);
            });
    }

    browseForEmailTemplatePage(): void {
        let startFolder;
        const browserNodeId = this.dataGroup.controls.mailsource_nodeid.value || this.nodeId;

        if (this.repositoryBrowserItemCache['mailsource_pageid']?.folderId) {
            startFolder = this.repositoryBrowserItemCache['mailsource_pageid'].folderId;
        }

        this.repositoryBrowserClient.openRepositoryBrowser({
            allowedSelection: 'page',
            selectMultiple: false,
            contentLanguage: this.activeFormLanguageCode,
            includeMlId: [MarkupLanguageType.FormsEmailTemplate],
            title: 'modal.repository_browser_title_forms_email_template_single',
            startNode: browserNodeId,
            startFolder,
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

        this.dataGroup.controls.mailsource_pageid.setValue(pageId, { emitEvent: false });
        this.dataGroup.controls.mailsource_pageid.markAsTouched({ emitEvent: false });
        this.dataGroup.controls.mailsource_pageid.markAsDirty({ emitEvent: false });

        this.dataGroup.controls.mailsource_nodeid.setValue(nodeId, { emitEvent: false });
        this.dataGroup.controls.mailsource_nodeid.markAsTouched({ emitEvent: false });
        this.dataGroup.controls.mailsource_nodeid.markAsDirty({ emitEvent: false });

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

    private migrateSuccessUrl(
        successurl: string,
        successurl_i18n: CmsFormElementI18nValue<string>,
        languages: string[],
    ): { successurl: string, successurl_i18n: CmsFormElementI18nValue<string> } {
        if (
            successurl && (
                typeof successurl_i18n !== 'object'
                || successurl_i18n == null
                || Object.keys(successurl_i18n).length === 0
            )
        ) {
            successurl_i18n = {};
            if (languages) {
                languages.forEach(languageCode => {
                    successurl_i18n[languageCode] = successurl;
                });
            }
            successurl = undefined;
        }
        return { successurl, successurl_i18n };

    }

    private isInternalPageUsed(successPageId: number, successNodeId: number, successurl_i18n: CmsFormElementI18nValue<string>): boolean {
        return (successPageId != null && successPageId !== 0 && successNodeId != null && successNodeId !== 0) || !successurl_i18n;
    }

    private isEmailTemplateUsed(mailsource_pageid: number, mailsource_nodeid: number, mailtemp_i18n: CmsFormElementI18nValue<string>): boolean {
        return (mailsource_pageid != null && mailsource_pageid !== 0 && mailsource_nodeid != null && mailsource_nodeid !== 0) || mailtemp_i18n === undefined;
    }

    /**
     * Since the options in the templateContext dropdown can change (on form type change),
     * there can be situations where a previously selected value becomes invalid.
     * Since the select itself does not conduct this check, we do it manually.
     * Hence, this method is needed to update the form controls validators according
     * to the newest allowed options.
     *
     * (i18n-select does this check automatically, thus, we do not have to take care of it.)
     *
     * @param templateContextOptions the values allowed for selection
     */
    private updateTemplateContextValidator(templateContextOptions: FormElementPropertyOptionConfiguration[] | null): void {
        const templateContextFormControl: AbstractControl | null = this.dataGroup.controls.templateContext;
        if (!templateContextFormControl) {
            return;
        }
        if (!templateContextOptions) {
            templateContextFormControl.clearValidators();
            templateContextFormControl.updateValueAndValidity();
            return;
        }

        const allowed: string[] = templateContextOptions.map(
            (templateContextOption: FormElementPropertyOptionConfiguration) => templateContextOption.key,
        );
        templateContextFormControl.setValidators((control: AbstractControl): ValidationErrors | null => {
            return allowed.includes(control.value) ? null : { invalidSelection: { value: control.value } } ;
        });
        templateContextFormControl.updateValueAndValidity();
    }
}
