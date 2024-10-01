/* eslint-disable @typescript-eslint/naming-convention */
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
    OnInit,
    SimpleChange,
} from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { ApplicationStateService } from '@editor-ui/app/state';
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
    FormPropertiesConfiguration,
} from '@gentics/form-generator';
import { UILanguage } from '@gentics/image-editor';
import { FormProperties, setControlsEnabled } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { BehaviorSubject, Observable, merge } from 'rxjs';
import { distinctUntilChanged, filter, map, mergeMap, take, tap } from 'rxjs/operators';
import { RepositoryBrowserClient } from '../../providers/repository-browser-client/repository-browser-client.service';
import { SelectedItemHelper } from '../../util/selected-item-helper/selected-item-helper';

export enum FormPropertiesMode {
    CREATE = 'create',
    EDIT = 'edit',
}

@Component({
    selector: 'gtx-form-properties',
    templateUrl: './form-properties.component.html',
    styleUrls: ['./form-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
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
    public properties: EditableFormProps = {};

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

    form: FormGroup<FormProperties<EditableFormProps>>;
    dataGroup: FormGroup<FormProperties<CmsFormData>>;

    /** Copy of the form property for easy access in the template / avoid several reevaluations due to function calls in the template */
    useInternalSuccessPage: boolean;
    useEmailPageTemplate: boolean;

    /** The helper for managing and loading the selected internal page. */
    private selectedInternalPageHelper: SelectedItemHelper<ItemInNode<Page<Raw>>>;
    private selectedEmailTemplatePageHelper: SelectedItemHelper<ItemInNode<Page<Raw>>>;

    successPageDisplayValue$: Observable<string>;
    emailTemplatePageDisplayValue$: Observable<string>;

    breadcrumbs: { [key: string]: string } = {
        internalSuccessPage: null,
        emailTemplatePage: null,
    };

    repositoryBrowserItemCache = {};

    activeFormLanguageCode$: Observable<string>;

    private activeFormLanguageCode: string;

    private previousFormType: CmsFormType | null;
    private formPropertiesConfigurationSubject: BehaviorSubject<FormPropertiesConfiguration> = new BehaviorSubject(null);
    public formPropertiesConfiguration$: Observable<FormPropertiesConfiguration> = this.formPropertiesConfigurationSubject.asObservable().pipe(
        filter((formPropertiesConfiguration: FormPropertiesConfiguration) => formPropertiesConfiguration !== null),
        distinctUntilChanged(isEqual),
        tap((formPropertiesConfiguration: FormPropertiesConfiguration) => {
            this.updateTemplateContextValidator(formPropertiesConfiguration.template_context_options);
        }),
    );

    constructor(
        changeDetector: ChangeDetectorRef,
        private client: GCMSRestClientService,
        private appState: ApplicationStateService,
        private formEditorService: FormEditorService,
        private formEditorConfigurationService: FormEditorConfigurationService,
        private repositoryBrowserClient: RepositoryBrowserClient,
        private i18n: I18nService,
    ) {
        super(changeDetector);
    }

    public override ngOnInit(): void {
        super.ngOnInit();

        // fallback: transfers old successurl to new successurl_i18n
        const { successurl, successurl_i18n } = this.migrateSuccessUrl(
            this.properties?.data?.successurl,
            this.properties?.data?.successurl_i18n,
            this.properties.languages,
        );
        if (this.properties.data == null) {
            this.properties.data = {};
        }
        this.properties.data.successurl = successurl;
        this.properties.data.successurl_i18n = successurl_i18n;

        this.subscriptions.push(this.formEditorConfigurationService.getConfiguration$(this.properties?.data?.type ?? CmsFormType.GENERIC).pipe(
            take(1),
        ).subscribe((formEditorConfiguration: FormEditorConfiguration): void => {
            this.formPropertiesConfigurationSubject.next(formEditorConfiguration.form_properties);
            this.previousFormType = this.properties?.data?.type ?? CmsFormType.GENERIC;
        }));

        // set initial value for useInternalSuccessPage and useEmailPage radio button group
        this.useInternalSuccessPage = this.isInternalPageUsed(
            this.properties.successPageId,
            this.properties.successNodeId,
            this.properties?.data?.successurl_i18n,
        );
        this.useEmailPageTemplate = this.isEmailTemplateUsed(
            this.properties?.data?.mailsource_pageid,
            this.properties?.data?.mailsource_nodeid,
            this.properties?.data?.mailtemp_i18n,
        );

        this.selectedInternalPageHelper = new SelectedItemHelper('page', -1, this.client);
        if (typeof this.properties.successPageId === 'number' && this.properties.successPageId !== 0) {
            /**
             * successNodeId is not checked to display an honest representation of the data currently stored in the CMS.
             *
             * As soon as a successPageId is set, we try to select it, even if there is no valid node such that the user knows
             * that something is selected. However, we cannot guarantee with this util class to find the page the backend uses
             * without its corresponding node.
             */
            this.selectedInternalPageHelper.setSelectedItem(this.properties.successPageId, this.properties.successNodeId);
        } else {
            this.form.controls.successPageId.setValue(0, { emitEvent: false });
            this.form.controls.successNodeId.setValue(0, { emitEvent: false });
            this.selectedInternalPageHelper.setSelectedItem(null);
        }

        this.selectedEmailTemplatePageHelper = this.initSelectedItemHelper('page', -1);
        if (typeof this.properties.data.mailsource_pageid === 'number' && this.properties.data.mailsource_pageid !== 0) {
            /**
             * mailsource_pageid is not checked to display an honest representation of the data currently stored in the CMS.
             *
             * As soon as a mailsource_pageid is set, we try to select it, even if there is no valid node such that the user knows
             * that something is selected. However, we cannot guarantee with this util class to find the page the backend uses
             * without its corresponding node.
             */
            this.selectedEmailTemplatePageHelper.setSelectedItem(this.properties.data.mailsource_pageid, this.properties.data.mailsource_nodeid);
        } else {
            this.dataGroup.controls.mailsource_pageid.setValue(0, { emitEvent: false });
            this.dataGroup.controls.mailsource_nodeid.setValue(0, { emitEvent: false });
            this.selectedEmailTemplatePageHelper.setSelectedItem(null);
        }

        this.successPageDisplayValue$ = this.trackDisplayValue(
            'successPageId',
            this.selectedInternalPageHelper,
            'internalSuccessPage');

        this.emailTemplatePageDisplayValue$ = this.trackDisplayValue(
            'mailsource_pageid',
            this.selectedEmailTemplatePageHelper,
            'emailTemplatePage',
        );

        // listen to changes made in the form
        this.subscriptions.push(this.form.valueChanges.subscribe(changes => {
            this.properties.languages = changes.languages;
            /**
             * Here type cannot be undefined, since in the form itself it is always set.
             * However, sometimes only a partial form is emitted (this has not yet been debugged).
             * Thus, in that case we simply ignore that type is undefined and proceed as usual / as before.
             */
            if (!!changes.data?.type && changes.data.type !== this.previousFormType) {
                this.subscriptions.push(this.formEditorConfigurationService.getConfiguration$(changes.data.type).pipe(
                    take(1),
                ).subscribe((formEditorConfiguration: FormEditorConfiguration): void => {
                    this.formPropertiesConfigurationSubject.next(formEditorConfiguration.form_properties);
                    this.previousFormType = changes.data.type;
                }));
            }

            this.changeDetector.markForCheck();
        }));

        this.activeFormLanguageCode$ = this.appState.select(state => state.folder.activeFormLanguage).pipe(
            mergeMap(activeLanguageId => this.appState.select(state => state.entities.language[activeLanguageId])),
            map(activeLanguage => activeLanguage && activeLanguage.code),
            tap(activeLanguageCode => {
                this.activeFormLanguageCode = activeLanguageCode;
                /**
                 * We need to set the language manually in the form editor service.
                 * (This is normally done in the form editor component. However, it is not used here).
                 */
                this.formEditorService.activeContentLanguageCode = activeLanguageCode;
            }),
        );

        this.subscriptions.push(this.appState.select(state => state.ui.language).subscribe((language: UILanguage) => {
            /**
             * We need to set the language manually in the form editor service.
             * (This is normally done in the form editor component. However, it is not used here).
             */
            this.formEditorService.activeUiLanguageCode = language;
        }));

    }

    public override ngOnChanges(changes: { [K in keyof this]: SimpleChange }): void {
        super.ngOnChanges(changes);

        this.preselectSingleLanguage();
        if (this.form && this.properties.languages) {
            this.form.controls.languages.setValue(this.properties.languages, { emitEvent: false });
        }

        const isAnotherForm = changes.item ?? changes.item?.previousValue.id !== changes.item?.currentValue.id;
        if (isAnotherForm) {
            this.updateForm(this.properties);
        }
    }

    protected createForm(): FormGroup {
        // set up form controls
        this.dataGroup = new FormGroup<FormProperties<CmsFormData>>({
            email: new FormControl(this.properties?.data?.email || ''),
            successurl_i18n: new FormControl(this.properties?.data?.successurl_i18n || null),
            mailsubject_i18n: new FormControl(this.properties?.data?.mailsubject_i18n || null),
            mailsource_pageid: new FormControl(this.properties?.data?.mailsource_pageid || null),
            mailsource_nodeid: new FormControl(this.properties?.data?.mailsource_nodeid || null),
            mailtemp_i18n: new FormControl(this.properties?.data?.mailtemp_i18n || null),
            // set default value
            templateContext: new FormControl(this.properties?.data?.templateContext || ''),
            type: new FormControl(this.properties?.data?.type || CmsFormType.GENERIC),
            // needs to be stored here, otherwise updating data object without elements will delete whole form
            elements: new FormControl(this.properties?.data?.elements || []),
        });

        return new FormGroup<FormProperties<EditableFormProps>>({
            name: new FormControl(this.properties.name || '', Validators.required),
            description: new FormControl(this.properties.description || ''),
            successPageId: new FormControl(this.properties?.successPageId || 0),
            successNodeId: new FormControl(this.properties?.successNodeId || 0),
            languages: new FormControl(this.properties?.languages || null),

            data: this.dataGroup,
        },
        /**
         * In order to check if for mdata has been modified and since `deepEqual` can't be used
         * due to asynchronous modification of `this.form`, Angular ReactiveForms API must be used.
         * However, since default `FormGroup.updateOn` won't triggered falsely thus indicating
         * `isModified` incorrectly `FormGroup.updateOn: 'blur'` will provide
         * `this.formGroup.pristine` to be the most reliable indicator of `isModified`.
         */
        { updateOn: 'blur' },
        );
    }

    protected configureForm(value: EditableFormProps, loud?: boolean): void {

    }

    protected assembleValue(value: EditableFormProps): EditableFormProps {
        return value;
    }

    initSelectedItemHelper(
        itemType: 'page' | 'folder' | 'file' | 'image' | 'form',
        defaultNodeId: number,
    ): SelectedItemHelper<ItemInNode<Page<Raw>>> {
        return new SelectedItemHelper(itemType, defaultNodeId, this.client);
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

    updateForm(properties: EditableFormProps): void {
        if (!this.form) {
            return;
        }
        if (!properties) {
            return;
        }
        if ((!properties?.data?.type && this.previousFormType !== CmsFormType.GENERIC) || properties?.data?.type !== this.previousFormType) {
            this.subscriptions.push(this.formEditorConfigurationService.getConfiguration$(properties?.data?.type ?? CmsFormType.GENERIC).pipe(
                take(1),
            ).subscribe((formEditorConfiguration: FormEditorConfiguration): void => {
                this.formPropertiesConfigurationSubject.next(formEditorConfiguration.form_properties);
                this.previousFormType = properties?.data?.type ?? CmsFormType.GENERIC;
            }));
        }

        // fallback: transfers old successurl to new successurl_i18n
        const { successurl, successurl_i18n } = this.migrateSuccessUrl(
            properties?.data?.successurl,
            properties?.data?.successurl_i18n,
            properties.languages,
        );
        if (properties.data == null) {
            properties.data = {};
        }
        properties.data.successurl = successurl;
        properties.data.successurl_i18n = successurl_i18n;
        this.useInternalSuccessPage = !!properties.successPageId;
        if (!this.useInternalSuccessPage) {
            this.selectedInternalPageHelper.setSelectedItem(null);
        }

        // set form input values
        this.form.patchValue(properties, { emitEvent: false });

        this.useInternalSuccessPage = this.isInternalPageUsed(
            properties.successPageId,
            properties.successNodeId,
            properties.data.successurl_i18n,
        );
        this.useEmailPageTemplate = this.isEmailTemplateUsed(
            properties.data.mailsource_pageid,
            properties.data.mailsource_nodeid,
            this.properties.data.mailtemp_i18n,
        );
        this.form.markAsPristine();
    }

    /**
     * Opens the repository browser to allow the user to select an internal page.
     */
    browseForPage(): void {
        this.repositoryBrowserClient.openRepositoryBrowser({ allowedSelection: 'page', selectMultiple: false, contentLanguage: this.activeFormLanguageCode })
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
        this.selectedInternalPageHelper.setSelectedItem(page);
        const pageId = page && typeof page.id === 'number' ? page.id : 0;
        const nodeId = page && typeof page.nodeId === 'number' ? page.nodeId : 0;
        this.form.markAsDirty();
        this.form.controls.successPageId.setValue(pageId, { emitEvent: false });
        this.form.controls.successNodeId.setValue(nodeId); /* has to emit event to trigger form value changes */
    }

    setEmailTemplatePage(page: ItemInNode<Page<Raw>>): void {
        delete this.repositoryBrowserItemCache['mailsource_pageid'];
        this.selectedEmailTemplatePageHelper.setSelectedItem(page);
        const pageId = page && typeof page.id === 'number' ? page.id : 0;
        const nodeId = page && typeof page.nodeId === 'number' ? page.nodeId : 0;
        this.dataGroup.controls.mailsource_pageid.setValue(pageId, { emitEvent: false });
        this.dataGroup.controls.mailsource_nodeid.setValue(nodeId);
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
        if (successurl
            && (typeof successurl_i18n !== 'object'
                || successurl_i18n === null
                || Object.keys(successurl_i18n).length === 0)) {
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

    trackDisplayValue(pageIdType: string, helper: SelectedItemHelper<ItemInNode<Page<Raw>>>, breadcrumb: string): Observable<string> {
        return merge(
            helper.selectedItem$.pipe(
                tap(page => this.breadcrumbs[breadcrumb] = this.generateBreadcrumbsPath(page)),
                map((selectedItem: Page<Raw>) => {
                    if (selectedItem) {
                        this.repositoryBrowserItemCache[pageIdType] = selectedItem;
                        return selectedItem.name;
                    } else {
                        /**
                         * null is emitted, when nothing is selected.
                         * Also, null is emitted in case a referenced page got deleted and the tag property data was refetched.
                         * (Since the pageId in tagProperty gets removed).
                         */
                        return this.i18n.translate('editor.page_no_selection');
                    }
                }),
            ),
            helper.loadingError$.pipe(
                map((error: { error: any, item: { itemId: number, nodeId?: number } }) => {
                    /**
                     * When a page that is referenced gets deleted, the pageId is kept in tagProperty.
                     * When we try to fetch the page information we get an error message.
                     * In that case we want to inform the user that the page got deleted
                     * (and thus avoid suggesting that a valid page is still selected).
                     */
                    if (this.form.get(pageIdType)) {
                        /** additional check, in case the loadingError$ Subject is changed to a BehaviorSubject in the future.
                         * This could trigger an emission before this.tagProperty is set in updateTagProperty
                         */
                        return this.i18n.translate('editor.page_not_found', { id: this.form.get(pageIdType).value });
                    } else {
                        return '';
                    }

                }),
            ),
        ).pipe(
            tap(() => this.changeDetector.markForCheck()),
        );
    }

    /**
     * checks the @input languages if only one language is available
     * sets the selected language in this.properties.language accordingly
     * deactivates isMultiLang to not render the language selection dropdown
     */
    private preselectSingleLanguage(): void {
        if (this?.languages?.length === 1 && this?.languages[0]?.code) {
            // API does expect an array as the languages value
            this.properties.languages = [this.languages[0].code];
            // as there is nothing to select, we also do not need to display language selection
            this.isMultiLang = false;
        }
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
