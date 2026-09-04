import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
    OnInit,
    SimpleChange,
} from '@angular/core';
import { FormControl, FormGroup, ValidatorFn, Validators } from '@angular/forms';
import {
    AccessControlledType,
    ConstructCategory,
    EditorControlStyle,
    GcmsPermission,
    Language,
    Node,
    Normalized,
    Raw,
    TagTypeBase,
} from '@gentics/cms-models';
import { BaseFormPropertiesComponent, FormProperties, generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';
import { Observable, combineLatest } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { createI18nRequiredValidator } from '../../../../common';
import { ConstructCategoryHandlerService, PermissionsService } from '../../../../core';
import { NodeDataService } from '../../../../shared';

export type ConstructPropertiesFormData = Omit<TagTypeBase<Raw>,
'name' | 'description' | 'globalId' | 'parts' | 'creator' | 'cdate' | 'editor' | 'edata'
> & {
    nodeIds?: number[];
};

export enum ConstructPropertiesMode {
    CREATE = 'create',
    UPDATE = 'update',
    COPY = 'copy',
}

const EDITOR_TAG_NAMES = [
    {
        value: 'div',
        label: 'construct.liveEditorTagName_type_div',
    },
    {
        value: 'span',
        label: 'construct.liveEditorTagName_type_span',
    },
];

/**
 * Defines the data editable by the `ConstructPropertiesComponent`.
 *
 * To convey the validity state of the user's input, the onChange callback will
 * be called with `null` if the form data is currently invalid.
 */
@Component({
    selector: 'gtx-construct-properties',
    templateUrl: './construct-properties.component.html',
    styleUrls: ['./construct-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(ConstructPropertiesComponent),
        generateValidatorProvider(ConstructPropertiesComponent),
    ],
    standalone: false,
})
export class ConstructPropertiesComponent
    extends BaseFormPropertiesComponent<ConstructPropertiesFormData>
    implements AfterViewInit, OnChanges, OnInit {

    public readonly ConstructPropertiesMode = ConstructPropertiesMode;
    public readonly EditorControlStyle = EditorControlStyle;
    public readonly EDITOR_TAG_NAMES = EDITOR_TAG_NAMES;

    @Input()
    public mode: ConstructPropertiesMode;

    @Input()
    public supportedLanguages: Language[];

    public constructCategories$: Observable<ConstructCategory<Normalized>[]>;
    public nodes$: Observable<Node<Raw>[]>;

    public activeTabI18nLanguage: Language;
    public invalidLanguages: string[] = [];

    constructor(
        changeDetector: ChangeDetectorRef,
        private categoryHandler: ConstructCategoryHandlerService,
        private nodeData: NodeDataService,
        private permissions: PermissionsService,
    ) {
        super(changeDetector);
    }

    ngOnInit(): void {
        super.ngOnInit();

        // load required dependencies into state
        this.constructCategories$ = this.categoryHandler.listMapped().pipe(map((res) => res.items));

        // Load the nodes and filter out all which do not have the required 'update' permission
        this.nodes$ = this.nodeData.watchAllEntities({ perms: true }).pipe(
            switchMap((nodes) => {
                return combineLatest(nodes.map((node) => {
                    return this.permissions.getInstancePermissions(AccessControlledType.NODE, node.id).pipe(
                        map((perms) => perms.hasPermission(GcmsPermission.UPDATE_CONSTRUCTS) ? node : null),
                    );
                }));
            }),
            map((nodes) => nodes.filter((node) => node != null)),
        );
    }

    ngAfterViewInit(): void {
        // Set FormGroup logic and rendering dependencies from external value
        this.form.updateValueAndValidity();
        this.changeDetector.markForCheck();
    }

    ngOnChanges(changes: Record<keyof ConstructPropertiesComponent, SimpleChange>): void {
        super.ngOnChanges(changes);

        if (changes.supportedLanguages) {
            const defaultLanguage = this.supportedLanguages?.[0];
            if (defaultLanguage) {
                this.activeTabI18nLanguage = defaultLanguage;
            }
            if (this.form) {
                const ctl = this.form.controls.nameI18n;
                ctl.setValidators(this.createNameValidator());
                ctl.updateValueAndValidity();
            }
        }
    }

    protected createForm(): FormGroup {
        return new FormGroup<FormProperties<ConstructPropertiesFormData>>({
            keyword: new FormControl(this.safeValue('keyword'), Validators.required),
            nameI18n: new FormControl(this.safeValue('nameI18n'), this.createNameValidator()),
            descriptionI18n: new FormControl(this.safeValue('descriptionI18n')),
            nodeIds: new FormControl(this.safeValue('nodeIds'), Validators.required),
            externalEditorUrl: new FormControl(this.safeValue('externalEditorUrl') ?? ''),
            mayBeSubtag: new FormControl(this.safeValue('mayBeSubtag')),
            mayContainSubtags: new FormControl(this.safeValue('mayContainSubtags')),
            categoryId: new FormControl(this.safeValue('categoryId')),
            autoEnable: new FormControl(this.safeValue('autoEnable')),
            liveEditorTagName: new FormControl(this.safeValue('liveEditorTagName') ?? 'div'),
            openEditorOnInsert: new FormControl(this.safeValue('openEditorOnInsert')),
            editorControlStyle: new FormControl(
                this.safeValue('editorControlStyle') ?? EditorControlStyle.ABOVE,
                Validators.required,
            ),
            editorControlsInside: new FormControl(this.safeValue('editorControlsInside')),
        }, { updateOn: 'change' });
    }

    protected configureForm(_value: ConstructPropertiesFormData, loud: boolean = false): void {
        const options = { emitEvent: loud };
        const nodesIdCtl = this.form.controls.nodeIds;

        nodesIdCtl.disable(options);

        // Can only be edited when we create a new construct
        if (this.mode === ConstructPropertiesMode.CREATE || this.mode === ConstructPropertiesMode.COPY) {
            nodesIdCtl.enable(options);
        }
    }

    protected assembleValue(formData: ConstructPropertiesFormData): ConstructPropertiesFormData {
        // Only add the node-ids when in creation mode
        if (this.mode === ConstructPropertiesMode.CREATE || this.mode === ConstructPropertiesMode.COPY) {
            return formData;
        } else {
            const { nodeIds, ...output } = formData;
            return output;
        }
    }

    createNameValidator(): ValidatorFn {
        const validator = createI18nRequiredValidator((this.supportedLanguages || []).map((l) => l.code), (langs) => {
            this.invalidLanguages = langs;
            this.changeDetector.markForCheck();
        });

        return validator;
    }

    setActiveI18nTab(languageId: number): void {
        this.activeTabI18nLanguage = this.supportedLanguages.find((l) => l.id === languageId);
    }

    activeI18nTabValueExists(languageCode: string): boolean {
        return [
            this.form.controls.nameI18n.value,
            this.form.controls.descriptionI18n.value,
        ].some((data) => !!data?.[languageCode]);
    }
}
