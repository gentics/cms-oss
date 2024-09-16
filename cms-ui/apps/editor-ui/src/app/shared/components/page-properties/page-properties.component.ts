import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChange,
} from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent, FormProperties } from '@gentics/cms-components';
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
    generateFormProvider,
    generateValidatorProvider,
    setControlsEnabled,
    setEnabled,
} from '@gentics/ui-core';
import { forkJoin } from 'rxjs';
import { debounceTime, filter, map, switchMap } from 'rxjs/operators';
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
    public enableFileNameSuggestion: boolean;

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

    @Output()
    public templatesLoaded = new EventEmitter<void>();

    public linkToTemplatesAllowed: boolean

    public niceUrlEnabled = false;
    // Note from Norbert -> This is configurable in the backend and might need to be updated
    // or checked async via an async-validator.
    urlPattern = '[\\w\\._\\-\\/]+';

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
    public suggestedFileName: FormControl<string>;

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

        this.suggestedFileName = new FormControl(this.value?.fileName);

        this.subscriptions.push(this.appState.select(state => state.features[Feature.NICE_URLS]).subscribe(enabled => {
            this.niceUrlEnabled = enabled;

            if (this.form) {
                this.form.updateValueAndValidity();
            }

            this.changeDetector.markForCheck();
        }));

        if (this.value?.fileName) {
            this.suggestedFileName.markAsDirty();
        } else {
            this.subscriptions.push(this.form.controls.name.valueChanges.pipe(
                debounceTime(400),
                filter(() => this.enableFileNameSuggestion && this.suggestedFileName.pristine),
                switchMap(name => this.client.page.suggestFileName({
                    pageName: name,
                    fileName: this.suggestedFileName.value,
                    language: this.form.value.language,
                    templateId: this.form.value.templateId,
                    folderId: this.folderId,
                    nodeId: this.nodeId,
                })),
            ).subscribe(res => {
                if (res.fileName && this.suggestedFileName.pristine) {
                    this.suggestedFileName.setValue(res.fileName);
                    this.changeDetector.markForCheck();
                }
            }));
        }

        this.subscriptions.push(forkJoin([
            // Here we don't need to load any template, we just need to know if any are available.
            this.client.node.listTemplates(this.nodeId, { pageSize: 0 }),
            this.permissions.forFolder(this.folderId, this.nodeId),
        ]).subscribe(([templateRes, perms]) => {
            this.linkToTemplatesAllowed = templateRes.numItems > 0 && perms.template?.view && perms.template?.link;
            this.changeDetector.markForCheck();
        }));

        /* Check if the currently linked template is still available. If not, remove it from the selection */
        this.subscriptions.push(this.appState.select(state => state.folder.templates.list).pipe(
            map((templateIds) => templateIds.map(templateId => this.entityResolver.getTemplate(templateId))),
        ).subscribe(templates => {
            this.templates = templates || [];

            const ctrl = this.form.controls.templateId;
            const found = this.templates.find(t => t?.id === ctrl.value);

            if (!found) {
                ctrl.setValue(null);
            }

            this.changeDetector.markForCheck();
        }));
    }

    public override ngOnChanges(changes: { [K in keyof this]: SimpleChange }): void {
        super.ngOnChanges(changes);

        if (changes.languages && this.form && this.languages && this.languages.length > 0) {
            this.form.controls.language.setValue(this.value?.language ?? this.languages[0].code);
        }
    }

    protected createForm(): FormGroup {
        return new FormGroup<FormProperties<EditablePageProps>>({
            name: new FormControl(this.value?.name || '', Validators.required),
            fileName: new FormControl(this.value?.fileName || ''),
            description: new FormControl(this.value?.description || ''),
            templateId: new FormControl(this.value?.templateId || null, Validators.required),
            niceUrl: new FormControl(this.value?.niceUrl || '', createMultiValuePatternValidator(this.urlPattern)),
            alternateUrls: new FormControl(this.value?.alternateUrls || [], createMultiValuePatternValidator(this.urlPattern)),
            language: new FormControl(this.value?.language || null, Validators.required),
            customCdate: new FormControl(this.value?.customCdate),
            customEdate: new FormControl(this.value?.customEdate),
            priority: new FormControl(this.value?.priority || 1, [Validators.required, numberBetween(1, 100)]),
        });
    }

    protected configureForm(value: EditablePageProps, loud?: boolean): void {
        const options = { onlySelf: loud, emitEvent: loud };
        setControlsEnabled(this.form, ['niceUrl', 'alternateUrls'], this.niceUrlEnabled, options);
        setControlsEnabled(this.form, ['language'], !this.disableLanguageSelect, options);
    }

    protected assembleValue(value: EditablePageProps): EditablePageProps {
        return {
            ...value,
            // Either the fileName if present, the suggestedFileName if the control is pristine, or empty
            fileName: value.fileName || (this.suggestedFileName.pristine && this.suggestedFileName.value) || '',
            customCdate: value?.customCdate || 0,
            customEdate: value?.customEdate || 0,
        };
    }

    protected override onDisabledChange(): void {
        if (this.suggestedFileName) {
            setEnabled(this.suggestedFileName, !this.disabled);
        }

        super.onDisabledChange();
    }

    public priorityRangeChanged(priority: number | Event): void {
        if (typeof priority === 'number') {
            const numberInput = this.form.controls.priority;
            numberInput.setValue(priority, { emitEvent: false });
            numberInput.updateValueAndValidity();
        }
    }

    protected override onValueChange(): void {
        if (this.form) {
            const tmpObj = {};
            Object.keys(this.form.controls).forEach((controlName: keyof EditablePageProps) => {
                if (this.value != null && this.value.hasOwnProperty(controlName)) {
                    // Edge case for custom dates - The API requires them to be not-null to not be ignored during updates.
                    // However, a `0` would still be a valid timestamp, so we check it here explicitly and mark it as null.
                    if ((controlName === 'customCdate' || controlName === 'customEdate')
                        && (this.value[controlName] === 0 || this.value[controlName] == null)) {
                        tmpObj[controlName] = null;
                    } else {
                        tmpObj[controlName] = this.value[controlName];
                    }
                }
            });
            this.form.patchValue(tmpObj);
        }
    }

    public async linkToTemplatesClicked(event: Event): Promise<void> {
        cancelEvent(event);
        await this.contextMenuOperations.linkTemplatesToFolder(this.nodeId, this.folderId);

        this.templatesLoaded.emit();
    }
}
