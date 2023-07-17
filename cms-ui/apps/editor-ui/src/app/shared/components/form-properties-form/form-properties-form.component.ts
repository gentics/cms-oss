import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChange,
} from '@angular/core';
import { AbstractControl, UntypedFormControl, UntypedFormGroup, ValidationErrors, Validators } from '@angular/forms';
import { Api } from '@editor-ui/app/core/providers/api/api.service';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { ApplicationStateService } from '@editor-ui/app/state';
import { MarkObjectPropertiesAsModifiedAction } from '@editor-ui/app/state';
import {
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
import { FolderApi } from '@gentics/cms-rest-clients-angular';
import {
    FormEditorConfiguration,
    FormEditorConfigurationService,
    FormEditorService,
    FormElementPropertyOptionConfiguration,
    FormPropertiesConfiguration,
} from '@gentics/form-generator';
import { UILanguage } from '@gentics/image-editor';
import { isEqual } from 'lodash';
import { BehaviorSubject, Observable, Subscription, merge } from 'rxjs';
import { distinctUntilChanged, filter, map, mergeMap, take, tap } from 'rxjs/operators';
import { RepositoryBrowserClient } from '../../providers/repository-browser-client/repository-browser-client.service';
import { SelectedItemHelper } from '../../util/selected-item-helper/selected-item-helper';

@Component({
    selector: 'form-properties-form',
    templateUrl: './form-properties-form.tpl.html',
    styleUrls: ['./form-properties-form.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormPropertiesFormComponent implements OnInit, OnChanges, OnDestroy {

    @Input()
    public nodeId: number;

    @Input()
    public folderId: number;

    @Input()
    public form: Form;

    @Input()
    public properties: EditableFormProps = {};

    @Input()
    public languages: Language[];

    @Input()
    public disabled = false;

    @Input()
    public disableLanguageSelect = false;

    @Input()
    public autoUpdateFileName = true;

    @Input()
    public mode: 'create' | 'edit' = 'edit';

    @Input()
    public isMultiLang: boolean;

    @Input()
    public showDetailProperties = false;

    @Output()
    public changes = new EventEmitter<EditableFormProps>();

    formGroup: UntypedFormGroup;
    private subscriptions: Subscription[] = [];
    formValue: EditableFormProps = {};

    formTypes: CmsFormType[] = Object.values(CmsFormType);

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
        private api: Api,
        private appState: ApplicationStateService,
        private formEditorService: FormEditorService,
        private formEditorConfigurationService: FormEditorConfigurationService,
        private repositoryBrowserClient: RepositoryBrowserClient,
        private i18n: I18nService,
        private changeDetector: ChangeDetectorRef,
    ) {}

    ngOnInit(): void {
        // fallback: transfers old successurl to new successurl_i18n
        const { successurl, successurl_i18n } = this.migrateSuccessUrl(this.properties.successurl, this.properties.successurl_i18n, this.properties.languages);
        this.properties.successurl = successurl;
        this.properties.successurl_i18n = successurl_i18n;

        this.subscriptions.push(this.formEditorConfigurationService.getConfiguration$(this.properties.type ? this.properties.type : CmsFormType.GENERIC).pipe(
            take(1),
        ).subscribe((formEditorConfiguration: FormEditorConfiguration): void => {
            this.formPropertiesConfigurationSubject.next(formEditorConfiguration.form_properties);
            this.previousFormType = this.properties.type ? this.properties.type : CmsFormType.GENERIC;
        }));

        // set up form controls
        this.formGroup = new UntypedFormGroup({
            name: new UntypedFormControl(this.properties.name || '', Validators.required),
            description: new UntypedFormControl(this.properties.description || ''),
            email: new UntypedFormControl(this.properties.email || ''),
            mailsubject_i18n: new UntypedFormControl(this.properties.mailsubject_i18n || null),
            successPageId: new UntypedFormControl(this.properties.successPageId || 0),
            successNodeId: new UntypedFormControl(this.properties.successNodeId || 0),
            successurl_i18n: new UntypedFormControl(this.properties.successurl_i18n || null),
            mailsource_pageid: new UntypedFormControl(this.properties.mailsource_pageid || null),
            mailsource_nodeid: new UntypedFormControl(this.properties.mailsource_nodeid || null),
            mailtemp_i18n: new UntypedFormControl(this.properties.mailtemp_i18n || null),
            // set default value
            languages: new UntypedFormControl(this.properties.languages || null),
            templateContext: new UntypedFormControl(this.properties.templateContext || ''),
            type: new UntypedFormControl(this.properties.type || CmsFormType.GENERIC),
            // needs to be stored here, otherwise updating data object without elements will delete whole form
            elements: new UntypedFormControl(this.properties.elements || []),
            useInternalSuccessPage: new UntypedFormControl(this.isInternalPageUsed(
                this.properties.successPageId,
                this.properties.successNodeId,
                this.properties.successurl_i18n,
            )),
            useEmailPageTemplate: new UntypedFormControl(this.isEmailTemplateUsed(
                this.properties.mailsource_pageid,
                this.properties.mailsource_nodeid,
                this.properties.mailtemp_i18n,
            )),
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

        // set initial value for useInternalSuccessPage and useEmailPage radio button group
        this.useInternalSuccessPage = this.isInternalPageUsed(
            this.properties.successPageId,
            this.properties.successNodeId,
            this.properties.successurl_i18n,
        );
        this.useEmailPageTemplate = this.isEmailTemplateUsed(
            this.properties.mailsource_pageid,
            this.properties.mailsource_nodeid,
            this.properties.mailtemp_i18n,
        );

        this.selectedInternalPageHelper = new SelectedItemHelper('page', -1, this.api.folders);
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
            this.formGroup.get('successPageId').setValue(0, { emitEvent: false });
            this.formGroup.get('successNodeId').setValue(0, { emitEvent: false });
            this.selectedInternalPageHelper.setSelectedItem(null);
        }

        this.selectedEmailTemplatePageHelper = this.initSelectedItemHelper('page', -1, this.api.folders);
        if (typeof this.properties.mailsource_pageid === 'number' && this.properties.mailsource_pageid !== 0) {
            /**
             * mailsource_pageid is not checked to display an honest representation of the data currently stored in the CMS.
             *
             * As soon as a mailsource_pageid is set, we try to select it, even if there is no valid node such that the user knows
             * that something is selected. However, we cannot guarantee with this util class to find the page the backend uses
             * without its corresponding node.
             */
            this.selectedEmailTemplatePageHelper.setSelectedItem(this.properties.mailsource_pageid, this.properties.mailsource_nodeid);
        } else {
            this.formGroup.get('mailsource_pageid').setValue(0, { emitEvent: false });
            this.formGroup.get('mailsource_nodeid').setValue(0, { emitEvent: false });
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
        this.subscriptions.push(this.formGroup.valueChanges.subscribe(changes => {
            this.properties.languages = changes.languages;
            /**
             * Here type cannot be undefined, since in the form itself it is always set.
             * However, sometimes only a partial form is emitted (this has not yet been debugged).
             * Thus, in that case we simply ignore that type is undefined and proceed as usual / as before.
             */
            if (!!changes.type && changes.type !== this.previousFormType) {
                this.subscriptions.push(this.formEditorConfigurationService.getConfiguration$(changes.type).pipe(
                    take(1),
                ).subscribe((formEditorConfiguration: FormEditorConfiguration): void => {
                    this.formPropertiesConfigurationSubject.next(formEditorConfiguration.form_properties);
                    this.previousFormType = changes.type;
                }));
            }

            // update of property languages to update i18n inputs
            this.useInternalSuccessPage = !!changes.useInternalSuccessPage;

            // remove data that is not needed based on ''useInternalSuccessPage''
            this.formValue = {
                name: changes.name,
                description: changes.description,
                email: changes.email,
                mailsubject_i18n: changes.mailsubject_i18n,
                successPageId: changes.useInternalSuccessPage ? changes.successPageId : 0, // 0 if unused
                successNodeId: changes.useInternalSuccessPage ? changes.successNodeId : 0, // 0 if unused
                successurl_i18n: !changes.useInternalSuccessPage ? changes.successurl_i18n : undefined, // undefined if unused
                mailsource_pageid: changes.useEmailPageTemplate ? changes.mailsource_pageid : 0, // 0 if unused
                mailsource_nodeid: changes.useEmailPageTemplate ? changes.mailsource_nodeid : 0, // 0 if unused
                mailtemp_i18n: !changes.useEmailPageTemplate ? changes.mailtemp_i18n: undefined, // undefined if unused
                languages: changes.languages,
                templateContext: changes.templateContext,
                type: changes.type || this.form.type || null,
                elements: changes.elements,
            };

            this.useInternalSuccessPage = changes.useInternalSuccessPage;
            this.useEmailPageTemplate = changes.useEmailPageTemplate;
            this.changeDetector.markForCheck();

            const isModified = !this.formGroup.pristine;
            // notify state about entity properties validity -> relevant for `ContentFrame.modifiedObjectPropertyValid`
            this.appState.dispatch(new MarkObjectPropertiesAsModifiedAction(isModified, this.formGroup.valid));

            this.changes.emit(this.formValue);
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

    ngOnChanges(changes: { [K in keyof this]: SimpleChange }): void {
        this.preselectSingleLanguage();
        if (this.formGroup && this.properties.languages) {
            this.formGroup.get('languages').setValue(this.properties.languages, { emitEvent: false });
        }

        const isAnotherForm = changes.form ?? changes.form?.previousValue.id !== changes.form?.currentValue.id;
        if (isAnotherForm) {
            this.updateForm(this.properties);
        }
    }

    ngOnDestroy(): void {
        if (this.subscriptions) {
            this.subscriptions.forEach(s => s.unsubscribe());
        }
    }

    initSelectedItemHelper(
        itemType: 'page' | 'folder' | 'file' | 'image' | 'form',
        defaultNodeId: number,
        folderApi: FolderApi,
    ): SelectedItemHelper<ItemInNode<Page<Raw>>> {
        return new SelectedItemHelper(itemType, defaultNodeId, folderApi);
    }

    setRadioState(input: string, value: boolean): void {
        const ctl = this.formGroup.get(input);
        if (ctl != null) {
            ctl.setValue(value);
            ctl.markAsDirty();
        }
        this.changeDetector.detectChanges();
    }

    updateForm(properties: EditableFormProps): void {
        if (!this.formGroup) {
            return;
        }
        if (!properties) {
            return;
        }
        if ((!properties.type && this.previousFormType !== CmsFormType.GENERIC) || properties.type !== this.previousFormType) {
            this.subscriptions.push(this.formEditorConfigurationService.getConfiguration$(properties.type ? properties.type : CmsFormType.GENERIC).pipe(
                take(1),
            ).subscribe((formEditorConfiguration: FormEditorConfiguration): void => {
                this.formPropertiesConfigurationSubject.next(formEditorConfiguration.form_properties);
                this.previousFormType = properties.type ? properties.type : CmsFormType.GENERIC;
            }));
        }

        // fallback: transfers old successurl to new successurl_i18n
        const { successurl, successurl_i18n } = this.migrateSuccessUrl(properties.successurl, properties.successurl_i18n, properties.languages);
        properties.successurl = successurl;
        properties.successurl_i18n = successurl_i18n;
        this.useInternalSuccessPage = !!properties.successPageId;
        if (!this.useInternalSuccessPage) {
            this.selectedInternalPageHelper.setSelectedItem(null);
        }

        // set form input values
        this.formGroup.get('name').setValue(properties.name, { emitEvent: false });
        this.formGroup.get('description').setValue(properties.description, { emitEvent: false });
        this.formGroup.get('email').setValue(properties.email, { emitEvent: false });
        this.formGroup.get('mailsubject_i18n').setValue(properties.mailsubject_i18n, { emitEvent: false });
        this.formGroup.get('successPageId').setValue(properties.successPageId, { emitEvent: false });
        this.formGroup.get('successNodeId').setValue(properties.successNodeId, { emitEvent: false });
        this.formGroup.get('successurl_i18n').setValue(properties.successurl_i18n, { emitEvent: false });
        this.formGroup.get('mailsource_pageid').setValue(properties.mailsource_pageid, { emitEvent: false });
        this.formGroup.get('mailsource_nodeid').setValue(properties.mailsource_nodeid, { emitEvent: false });
        this.formGroup.get('mailtemp_i18n').setValue(properties.mailtemp_i18n, { emitEvent: false });
        this.formGroup.get('languages').setValue(properties.languages, { emitEvent: false });
        this.formGroup.get('templateContext').setValue(properties.templateContext || '', { emitEvent: false });
        this.formGroup.get('type').setValue(properties.type || CmsFormType.GENERIC, { emitEvent: false });
        // needs to be stored here, otherwise updating data object without elements will delete whole form
        this.formGroup.get('elements').setValue(properties.elements, { emitEvent: false });
        this.formGroup.get('useInternalSuccessPage').setValue(
            this.isInternalPageUsed(properties.successPageId, properties.successNodeId, properties.successurl_i18n),
            { emitEvent: false },
        );
        this.formGroup.get('useEmailPageTemplate').setValue(
            this.isEmailTemplateUsed(properties.mailsource_pageid, properties.mailsource_nodeid, this.properties.mailtemp_i18n),
            { emitEvent: false },
        );
        this.formGroup.markAsPristine();
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
        const browserNodeId = this.formGroup.get('mailsource_nodeid').value || this.nodeId;

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
        this.formGroup.markAsDirty();
        this.formGroup.get('successPageId').setValue(pageId, { emitEvent: false });
        this.formGroup.get('successNodeId').setValue(nodeId); /* has to emit event to trigger form value changes */
    }

    setEmailTemplatePage(page: ItemInNode<Page<Raw>>): void {
        delete this.repositoryBrowserItemCache['mailsource_pageid'];
        this.selectedEmailTemplatePageHelper.setSelectedItem(page);
        const pageId = page && typeof page.id === 'number' ? page.id : 0;
        const nodeId = page && typeof page.nodeId === 'number' ? page.nodeId : 0;
        this.formGroup.get('mailsource_pageid').setValue(pageId, { emitEvent: false });
        this.formGroup.get('mailsource_nodeid').setValue(nodeId);
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
                    if (this.formGroup.get(pageIdType)) {
                        /** additional check, in case the loadingError$ Subject is changed to a BehaviorSubject in the future.
                         * This could trigger an emission before this.tagProperty is set in updateTagProperty
                         */
                        return this.i18n.translate('editor.page_not_found', { id: this.formGroup.get(pageIdType).value });
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
        const templateContextFormControl: AbstractControl | null = this.formGroup.get('templateContext');
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
