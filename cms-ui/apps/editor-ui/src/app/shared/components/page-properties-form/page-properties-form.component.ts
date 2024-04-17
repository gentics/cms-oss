import {
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
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { MarkObjectPropertiesAsModifiedAction } from '@editor-ui/app/state';
import {
    EditablePageProps,
    EditorPermissions,
    Feature,
    Language,
    Normalized,
    Page,
    SuggestPageFileNameRequest,
    SuggestPageFileNameResponse,
    Template,
} from '@gentics/cms-models';
import { createMultiValuePatternValidator } from '@gentics/ui-core';
import { cloneDeep } from 'lodash-es';
import { Observable, Subject, Subscription, of } from 'rxjs';
import { catchError, debounceTime, map, mergeMap, publishLast, refCount, switchMap } from 'rxjs/operators';
import { numberBetween } from '../../../common/utils/custom-validators';
import { deepEqual } from '../../../common/utils/deep-equal';
import { Api } from '../../../core/providers/api';
import { ContextMenuOperationsService } from '../../../core/providers/context-menu-operations/context-menu-operations.service';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { PermissionService } from '../../../core/providers/permissions/permission.service';
import { ApplicationStateService, FeaturesActionsService, FolderActionsService } from '../../../state';

@Component({
    selector: 'page-properties-form',
    templateUrl: './page-properties-form.tpl.html',
    styleUrls: ['./page-properties-form.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PagePropertiesForm implements OnInit, OnChanges, OnDestroy {

    @Input()
    public nodeId: number;

    @Input()
    public folderId: number;

    @Input()
    public enableFileNameSuggestion: boolean;

    @Input()
    public page?: Page;

    @Input()
    public properties: EditablePageProps = {};

    @Input()
    public templates: Template[] = [];

    @Input()
    public languages: Language[] = [];

    @Input()
    public disabled = false;

    @Input()
    public disableLanguageSelect = false;

    @Input()
    public autoUpdateFileName = true;

    @Input()
    public mode: 'create' | 'edit' = 'edit';

    @Output()
    public changes = new EventEmitter<EditablePageProps>();

    @Output()
    public templatesLoaded = new EventEmitter<void>();

    linkToTemplatesAllowed$: Observable<boolean>;

    form: UntypedFormGroup;
    subscriptions: Subscription[] = [];
    niceUrlsActivated = false;
    // This is a work-around for a bug (https://github.com/angular/angular/issues/12366) where a valueChange
    // event is emitted when enabling the niceUrl FormControl, even though emitEvent is set to false.
    niceUrlsChecked = false;
    // Note from Norbert -> This is configurable in the backend and might need to be updated
    // or checked async via an async-validator.
    urlPattern = '[\\w\\._\\-\\/]+';
    customCdateEnabled: boolean;
    customEdateEnabled: boolean;
    /** current templates */
    currentFolderTemplates$: Observable<Template<Normalized>[]>;

    private propertiesMemory: EditablePageProps;
    private suggestPageFileNameRequestSubject: Subject<SuggestPageFileNameRequest> = new Subject<SuggestPageFileNameRequest>();

    constructor(
        private api: Api,
        private changeDetector: ChangeDetectorRef,
        private folderActions: FolderActionsService,
        private errorHandler: ErrorHandler,
        private featuresActions: FeaturesActionsService,
        private appState: ApplicationStateService,
        private entityResolver: EntityResolver,
        private contextMenuOperations: ContextMenuOperationsService,
        private permissions: PermissionService,
    ) {
        this.subscriptions.push(this.suggestPageFileNameRequestSubject.asObservable().pipe(
            debounceTime(400),
            switchMap((request: SuggestPageFileNameRequest) => {
                return this.api.folders.suggestPageFileName(request).pipe(
                    map((response: SuggestPageFileNameResponse) => response.fileName),
                    catchError(error => {
                        this.errorHandler.catch(error, { notification: true });
                        return of('');
                    }),
                );
            }),
        ).subscribe((suggestedFileName: string) => {
            const requestedFileNameChanged = this.form.get('suggestedOrRequestedFileName').dirty;
            if (this.form && !requestedFileNameChanged) {
                this.form.get('suggestedOrRequestedFileName').setValue(suggestedFileName);
            }
        }));
    }

    ngOnInit(): void {
        this.form = new UntypedFormGroup({
            pageName: new UntypedFormControl(this.properties.pageName || '', Validators.required),
            /**
             * This input field will be shown to the user. As long it is pristine, file name suggestions will be put into it.
             * However, this is not the input field that is read by code that use this PagePropertiesForm.
             * There can be situations, where a user wishes to use a file name suggestion, but clicks save before it is updated
             * and thus uses an outdated suggestion. Since the backend generates the same file name when a file is saved without a file name
             * as would have been suggested, we only add a file name to the request, when it was entered by the user (and not suggested).
             * So while suggestedOrRequestedFileName contains both, suggestions and explicitly chosen file names, fileName only contains those
             * that are explicitly chosen.
             * Code that uses PagePropertiesForm reads only fileName and thus get an empty string in case a file name suggestion should be used.
             * This avoids the problem described above.
             */
            suggestedOrRequestedFileName: new UntypedFormControl(this.properties.fileName || ''),
            fileName: new UntypedFormControl(this.properties.fileName || ''),
            description: new UntypedFormControl(this.properties.description || ''),
            templateId: new UntypedFormControl(this.properties.templateId || null, Validators.required),
            niceUrl: new UntypedFormControl(this.properties.niceUrl || '', createMultiValuePatternValidator(this.urlPattern)),
            alternateUrls: new UntypedFormControl(this.properties.alternateUrls || [], createMultiValuePatternValidator(this.urlPattern)),
            language: new UntypedFormControl(this.properties.language || null),
            customCdate: new UntypedFormControl(this.getCustomCdateDisplayValue(this.properties, this.page)),
            customEdate: new UntypedFormControl(this.getCustomEdateDisplayValue(this.properties, this.page)),
            priority: new UntypedFormControl(this.properties.priority || 1, numberBetween(1, 100)),
        });
        if (this.properties.fileName) {
            this.form.get('suggestedOrRequestedFileName').markAsDirty();
        }

        this.form.get('niceUrl').disable({ emitEvent: false });
        this.form.get('alternateUrls').disable({ emitEvent: false });

        this.featuresActions.checkFeature(Feature.NICE_URLS)
            .then(active => {
                if (active) {
                    this.form.get('niceUrl').enable({ emitEvent: false });
                    this.form.get('alternateUrls').enable({ emitEvent: false });
                    this.niceUrlsActivated = true;
                }
                this.niceUrlsChecked = true;
                // trigger form change at least once to emit existing entity object
                this.form.updateValueAndValidity({ emitEvent: true });
                this.changeDetector.markForCheck();
            });

        this.updateCustomDatesEnabledStatus();

        this.linkToTemplatesAllowed$ = this.folderActions.getAllTemplatesOfNode(this.nodeId).pipe(
            map(allTemplates => Array.isArray(allTemplates) && allTemplates.length > 0),
            publishLast(),
            refCount(),
            mergeMap((hasTemplates: boolean) => this.permissions.forFolder(this.folderId, this.nodeId).pipe(
                map((perms: EditorPermissions) => [hasTemplates, perms.template && perms.template.view && perms.template.link]),
            )),
            map(([hasTemplates, hasPermissions]) => hasTemplates && hasPermissions),
        );

        const changeSub = this.form.valueChanges.subscribe((changes: EditablePageProps) => this.fireChangesEvent(changes));
        this.subscriptions.push(changeSub);

        // get current folder templates from in case they got updated while properties being open
        this.currentFolderTemplates$ = this.appState.select(state => state.folder.templates.list).pipe(
            map((templateIds) => templateIds.map(templateId => this.entityResolver.getTemplate(templateId))),
        );
        const changeTemplates = this.currentFolderTemplates$
            .subscribe((folderTemplates: Template<Normalized>[]) => {
                this.templates = folderTemplates;

                // check if currently selected template is among currently available templates
                const selectedTemplateControl = this.form.get('templateId');
                if (
                    Array.isArray(this.templates)
                    && !this.templates.find(template => template && template.id === selectedTemplateControl.value)
                ) {
                    // remove current selection
                    /**
                     * This also calls updateValueAndValidity which will also update its ancestors and
                     * is responsible for triggering the change detection.
                     */
                    selectedTemplateControl.setValue(null);
                } else {
                    /**
                     * To be safe, we also trigger change detection in the else branch.
                     *
                     * However, currently all cases that require visual change happen in the if branch.
                     * Since this may change in future, this additional check should avoid potential bugs.
                     *
                     * Alternatively, one could also make use of the async-Pipe to trigger change detections
                     * automatically. This would entail some restructuring of the template code, to avoid multiple
                     * subscriptions and is thus omitted for now.
                     */
                    this.changeDetector.markForCheck();
                }
            });
        this.subscriptions.push(changeTemplates);
    }

    ngOnChanges(changes: { [K in keyof this]: SimpleChange }): void {
        if (this.form && this.languages && this.languages.length > 0) {
            this.form.get('language').setValue(this.properties.language || this.languages[0], { emitEvent: false });
        }

        if (changes.properties) {
            this.updateForm(this.properties);
        }
        if (changes.page) {
            this.updateCustomDatesEnabledStatus();
        }
    }

    ngOnDestroy(): void {
        if (this.subscriptions) {
            this.subscriptions.forEach(s => s.unsubscribe());
        }
    }

    priorityRangeChanged(priority: number | Event): void {
        if (typeof priority === 'number') {
            const numberInput = this.form.get('priority');
            numberInput.setValue(priority, { emitEvent: false });
            numberInput.updateValueAndValidity();
        }
    }

    updateForm(properties: EditablePageProps): void {
        if (!this.form) {
            return;
        }
        properties = {
            ...properties,
            customCdate: this.getCustomCdateDisplayValue(properties, this.page),
            customEdate: this.getCustomEdateDisplayValue(properties, this.page),
        };
        this.form.reset({ ...properties, suggestedOrRequestedFileName: properties.fileName }, { onlySelf: true, emitEvent: false });
        if (this.properties.fileName) {
            this.form.get('suggestedOrRequestedFileName').markAsDirty();
        } else {
            this.form.get('suggestedOrRequestedFileName').markAsPristine();
        }
    }

    onCustomDateEnabledChange(enabledStateVar: 'customCdateEnabled' | 'customEdateEnabled', newValue: boolean): void {
        this[enabledStateVar] = newValue;
        if (this.form) {
            // The form controls are updated after this event handler, so we need to trigger a form update at the end of the event loop.
            setTimeout(() => this.form.updateValueAndValidity());
        }
    }

    async linkToTemplatesClicked(event: Event): Promise<void> {
        event.preventDefault();
        await this.contextMenuOperations.linkTemplatesToFolder(this.nodeId, this.folderId);

        this.templatesLoaded.emit();
    }

    private fireChangesEvent(changes: EditablePageProps): void {
        changes = { ...changes };
        if (!this.customCdateEnabled) {
            changes.customCdate = 0;
        }
        if (!this.customEdateEnabled) {
            changes.customEdate = 0;
        }

        const isModified = !!this.propertiesMemory && !deepEqual(this.propertiesMemory, changes);
        // notify state about entity properties validity -> relevant for `ContentFrame.modifiedObjectPropertyValid`
        this.appState.dispatch(new MarkObjectPropertiesAsModifiedAction(isModified, this.form.valid));
        this.propertiesMemory = cloneDeep(changes);

        this.changes.emit(changes);
    }

    private getCustomCdateDisplayValue(properties: EditablePageProps, page: Page): number {
        return page ? (properties.customCdate || page.cdate) : null;
    }

    private getCustomEdateDisplayValue(properties: EditablePageProps, page: Page): number {
        return page ? (properties.customEdate || page.edate) : null;
    }

    private updateCustomDatesEnabledStatus(): void {
        const oldCustomCdateEnabled = this.customCdateEnabled;
        const oldCustomEdateEnabled = this.customEdateEnabled;
        this.customCdateEnabled = !!this.properties.customCdate;
        this.customEdateEnabled = !!this.properties.customEdate;

        if (this.form && (oldCustomCdateEnabled !== this.customCdateEnabled || oldCustomEdateEnabled !== this.customEdateEnabled)) {
            this.form.updateValueAndValidity();
        }
    }

    suggestPageFileName(): void {
        const requestedFileNameChanged = this.form.get('suggestedOrRequestedFileName').dirty;
        if (!requestedFileNameChanged && this.enableFileNameSuggestion) {
            const request = this.assembleFileNameSuggestionRequest();
            request.fileName = '';
            if (request.templateId) {
                this.suggestPageFileNameRequestSubject.next(request);
            }
        }
    }

    sanitizePageFileName(): void {
        const request = this.assembleFileNameSuggestionRequest();
        if (request.templateId && request.fileName) {
            const sub = this.api.folders.suggestPageFileName(request).pipe(
                map((response: SuggestPageFileNameResponse) => response.fileName),
            ).subscribe((fileName: string) => {
                this.form.get('suggestedOrRequestedFileName').setValue(fileName);
                this.form.get('fileName').setValue(fileName);
            }, error => {
                this.errorHandler.catch(error, { notification: true });
            });
            this.subscriptions.push(sub);
        }
    }

    updateRequestedFileName(): void {
        const requestedFileName = this.form.get('suggestedOrRequestedFileName').value;
        this.form.get('fileName').setValue(requestedFileName);
    }

    assembleFileNameSuggestionRequest(): SuggestPageFileNameRequest {
        const pageName = this.form.get('pageName').value;
        const suggestedOrRequestedFileName = this.form.get('suggestedOrRequestedFileName').value;
        const templateId = this.form.get('templateId').value;
        const language = this.form.get('language').value;

        return {
            folderId: this.folderId,
            nodeId: this.nodeId,
            templateId: templateId,
            language: language,
            pageName: pageName,
            fileName: suggestedOrRequestedFileName,
        };
    }
}
