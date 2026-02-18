import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChange
} from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import {
    EditablePageProps,
    Feature,
    Language,
    Page,
    Template,
} from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import {
    cancelEvent,
    createMultiValuePatternValidator,
    FormProperties,
    generateFormProvider,
    generateValidatorProvider,
    setControlsEnabled,
} from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { combineLatest, Observable, of, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, filter, map, switchMap } from 'rxjs/operators';
import { numberBetween } from '../../../common/utils/custom-validators';
import { ContextMenuOperationsService } from '../../../core/providers/context-menu-operations/context-menu-operations.service';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { PermissionService } from '../../../core/providers/permissions/permission.service';
import { ApplicationStateService } from '../../../state';

export enum PagePropertiesMode {
    CREATE = 'create',
    EDIT = 'edit',
}

@Component({
    selector: 'gtx-page-properties',
    templateUrl: './page-properties.component.html',
    styleUrls: ['./page-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(PagePropertiesComponent),
        generateValidatorProvider(PagePropertiesComponent),
    ],
    standalone: false,
})
export class PagePropertiesComponent
    extends BasePropertiesComponent<EditablePageProps>
    implements OnInit, OnChanges {

    public readonly PagePropertiesMode = PagePropertiesMode;

    @Input()
    public nodeId: number;

    @Input()
    public folderId: number;

    @Input()
    public item?: Page;

    @Input()
    public templates: Template[] = [];

    @Input()
    public languages: Language[] = [];

    @Input()
    public disableLanguageSelect = false;

    @Input()
    public autoUpdateFileName = true;

    @Input()
    public mode: PagePropertiesMode = PagePropertiesMode.EDIT;

    /**
     * When the linking of templates to the current folder have been updated and need to be
     * re-fetched.
     */
    @Output()
    public templatesLinked = new EventEmitter<void>();

    public viewTemplatesAllowed: boolean;
    public linkToTemplatesAllowed: boolean;

    public niceUrlEnabled = false;
    // Note from Norbert -> This is configurable in the backend and might need to be updated
    // or checked async via an async-validator.
    urlPattern = '[\\w\\._\\-\\/]+';

    /** The file-name used for the last suggestion request */
    private suggestionOldPageName: string;
    /** The language used for the last suggestion request */
    private suggestionOldLanguage: string;
    /** If the file-name input is currently focused */
    public fileNameFocused = false;
    /** If the currently set fileName was generated */
    private fileNameGenerated = false;
    /** Subject which is triggered, whenever a new suggestion is needed. */
    private suggestionSubject = new Subject<boolean>();

    constructor(
        changeDetector: ChangeDetectorRef,
        private client: GCMSRestClientService,
        private appState: ApplicationStateService,
        private entityResolver: EntityResolver,
        private contextMenuOperations: ContextMenuOperationsService,
        private permissions: PermissionService,
    ) {
        super(changeDetector);
    }

    public override ngOnInit(): void {
        super.ngOnInit();

        this.subscriptions.push(this.appState.select((state) => state.features[Feature.NICE_URLS]).subscribe((enabled) => {
            this.niceUrlEnabled = enabled;

            if (this.form) {
                this.configureForm(this.form.value, false);
                this.triggerChange(this.assembleValue(this.form.value));
            }

            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.form.valueChanges.pipe(
            filter((value) =>
                // Make sure we have the required properties at least set before we attempt to request
                value?.language && value?.templateId != null,
            ),
            map((value) => ({
                pageName: value.name,
                language: value.language,
                templateId: value.templateId,
            })),
            distinctUntilChanged(isEqual),
        ).subscribe(() => {
            this.suggestNewFileName();
        }));

        this.subscriptions.push(combineLatest([
            // Here we don't need to load any template, we just need to know if any are available.
            this.client.node.listTemplates(this.nodeId, { pageSize: 0 }),
            this.permissions.forFolder(this.folderId, this.nodeId),
        ]).subscribe(([templateRes, perms]) => {
            this.viewTemplatesAllowed = perms.template?.view;
            this.linkToTemplatesAllowed = templateRes.numItems > 0 && perms.template?.view && perms.template?.link;
            this.configureForm(this.form.value);
            this.changeDetector.markForCheck();
        }));

        /* Check if the currently linked template is still available. If not, remove it from the selection */
        this.subscriptions.push(this.appState.select((state) => state.folder.templates.list).pipe(
            map((templateIds) => templateIds.map((templateId) => this.entityResolver.getTemplate(templateId))),
        ).subscribe((templates) => {
            this.templates = templates || [];

            const ctrl = this.form.controls.templateId;

            if (ctrl.value != null) {
                const found = this.templates.find((t) => t?.id === ctrl.value);

                if (!found) {
                    ctrl.setValue(null);
                }
            } else if (templates.length > 0) {
                ctrl.setValue(templates[0].id);
            }

            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.suggestionSubject.pipe(
            debounceTime(100),
            switchMap((forced) => {
                const { templateId, name, language } = this.form.value;

                let newName: Observable<string> = of('');
                if (name) {
                    newName = this.client.page.suggestFileName({
                        pageName: name,
                        folderId: this.folderId,
                        nodeId: this.nodeId,
                        language: language,
                        templateId: templateId,

                        fileName: '',
                    }).pipe(
                        map((res) => res.fileName),
                    );
                }

                return newName.pipe(
                    map((fileName) => ({
                        forced,
                        templateId,
                        pageName: name,
                        language,
                        fileName,
                    })),
                );
            }),
        ).subscribe((data) => {
            // Don't actually perform the update if the user started to focus the file name
            // in the meantime.
            if (this.fileNameFocused) {
                return;
            }

            this.form.controls.fileName.setValue(data.fileName);
            // Update the "old" properties in here, since only now they are actually "old"
            this.suggestionOldPageName = data.pageName;
            this.suggestionOldLanguage = data.language;
            // If the file name was "forced", we don't actually want to mark it as generated,
            // as new changes to the name would update it then.
            this.fileNameGenerated = this.fileNameGenerated || !data.forced;
            this.changeDetector.markForCheck();
        }));
    }

    public override ngOnChanges(changes: { [K in keyof this]: SimpleChange }): void {
        super.ngOnChanges(changes);

        if (changes.languages && this.form && this.languages && this.languages.length > 0) {
            this.form.controls.language.setValue(this.value?.language ?? this.languages[0].code);
        }
    }

    protected override onValueReset(): void {
        super.onValueReset();

        if (this.form && this.item?.fileName) {
            this.form.controls.fileName.markAsDirty();
        }
    }

    protected createForm(): FormGroup {
        return new FormGroup<FormProperties<EditablePageProps>>({
            name: new FormControl(this.safeValue('name') || '', Validators.required),
            fileName: new FormControl(this.safeValue('fileName') || ''),
            description: new FormControl(this.safeValue('description') || ''),
            templateId: new FormControl(this.safeValue('templateId') || null, Validators.required),
            niceUrl: new FormControl(this.safeValue('niceUrl') || '', createMultiValuePatternValidator(this.urlPattern)),
            alternateUrls: new FormControl(this.safeValue('alternateUrls') || [], createMultiValuePatternValidator(this.urlPattern)),
            language: new FormControl(this.safeValue('language') ?? this.languages?.[0]?.code, Validators.required),
            customCdate: new FormControl(this.safeValue('customCdate')),
            customEdate: new FormControl(this.safeValue('customEdate')),
            priority: new FormControl(this.safeValue('priority') || 1, [Validators.required, numberBetween(1, 100)]),
        });
    }

    protected configureForm(value: EditablePageProps, loud?: boolean): void {
        const options = { onlySelf: false, emitEvent: loud };
        setControlsEnabled(this.form, ['niceUrl', 'alternateUrls'], this.niceUrlEnabled, options);
        setControlsEnabled(this.form, ['language'], !this.disableLanguageSelect, options);
        setControlsEnabled(this.form, ['templateId'], this.mode === PagePropertiesMode.CREATE || this.viewTemplatesAllowed);
    }

    protected assembleValue(value: EditablePageProps): EditablePageProps {
        if (this.mode === PagePropertiesMode.EDIT) {
            return {
                ...value,
                // Either the fileName if present, or empty
                fileName: value.fileName || '',
                customCdate: value?.customCdate || 0,
                customEdate: value?.customEdate || 0,
            };
        } else {
            // eslint-disable-next-line @typescript-eslint/naming-convention, @typescript-eslint/no-unused-vars
            const { customCdate, customEdate, ...tmp } = value;
            return {
                ...tmp,
                // Either the fileName if present, or empty
                fileName: tmp.fileName || '',
            };
        }
    }

    public priorityRangeChanged(priority: number | Event): void {
        if (typeof priority === 'number') {
            const numberInput = this.form.controls.priority;
            numberInput.setValue(priority);
        }
    }

    protected override onValueChange(): void {
        if (this.form) {
            const tmpObj = {};
            Object.keys(this.form.controls).forEach((controlName: keyof EditablePageProps) => {
                if (this.value != null && this.value.hasOwnProperty(controlName)) {
                    // Edge case for custom dates - The API requires them to be not-null to not be ignored during updates.
                    // However, a `0` would still be a valid timestamp, so we check it here explicitly and mark it as null.
                    if (
                        (controlName === 'customCdate' || controlName === 'customEdate')
                        && (this.value[controlName] === 0 || this.value[controlName] == null)
                    ) {
                        tmpObj[controlName] = null;
                    } else {
                        tmpObj[controlName] = this.value[controlName];
                    }
                }
            });
            this.form.patchValue(tmpObj);
        }
    }

    protected suggestNewFileName(force: boolean = false): void {
        const { templateId, name, language, fileName } = this.form.value;

        if (
            // Don't update the file-name while it's focused, would screw up the editing
            this.fileNameFocused
            // Don't need to update, as the content is still the same
            || (fileName && name === this.suggestionOldPageName && language === this.suggestionOldLanguage)
            || (!force && (
                // Don't update if the user has changed the file-name manually
                (this.form.controls.fileName.dirty && fileName)
                // Don't override the file-name if it was already set previously
                || (this.form.controls.fileName.pristine && !this.fileNameGenerated && fileName)
            ))
            // Can't request a file-name without a template - No idea why
            || templateId == null
            // Need the folder and node id
            || this.folderId == null
            || this.nodeId == null
        ) {
            return;
        }

        this.suggestionSubject.next(force);
    }

    public onFileNameFocus(): void {
        this.fileNameFocused = true;
    }

    public onFileNameBlur(): void {
        this.fileNameFocused = false;
        this.suggestNewFileName();
    }

    public onFileNameChange(): void {
        this.fileNameGenerated = false;
    }

    public async linkToTemplatesClicked(event: Event): Promise<void> {
        cancelEvent(event);
        const templates = await this.contextMenuOperations.linkTemplatesToFolder(this.nodeId, this.folderId);
        if (templates != null) {
            this.templates = templates;
        }

        this.templatesLinked.emit();
    }
}
