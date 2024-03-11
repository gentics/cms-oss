import { createI18nRequiredValidator } from '@admin-ui/common';
import { ConstructCategoryHandlerService, PermissionsService } from '@admin-ui/core';
import { NodeDataService } from '@admin-ui/shared';
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
import { FormControl, FormGroup, UntypedFormControl, ValidatorFn, Validators } from '@angular/forms';
import { BasePropertiesComponent, CONTROL_INVALID_VALUE, FormProperties } from '@gentics/cms-components';
import {
    AccessControlledType,
    CmsI18nValue,
    ConstructCategory,
    EditorControlStyle,
    GcmsPermission,
    GtxI18nProperty,
    Language,
    Node,
    Normalized,
    Raw,
    TagTypeBO,
    TagTypeBase,
} from '@gentics/cms-models';
import { generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';
import { Observable, combineLatest } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';

export type ConstructPropertiesFormData = Omit<
TagTypeBase<Raw>,
'name' | 'description' | 'globalId' | 'parts' | 'creator' | 'cdate' | 'editor' | 'edata' | 'editdo'
> & {
    nodeIds?: number[];
}

export enum ConstructPropertiesMode {
    CREATE = 'create',
    UPDATE = 'update',
    COPY = 'copy',
}

/* eslint-disable @typescript-eslint/naming-convention */
const CONSTRUCT_ICONS = {
    ETC: 'etc.gif',
    STOP: 'stop.gif',
    TAB_EDIT: 'tab_edit.gif',
    FILE: 'datei.gif',
    FILE2: 'file.gif',
    TEXT: 'text.gif',
    TEXT_ITALIC: 'textit.gif',
    TEXT_BOLD: 'textbold.gif',
    IMAGE: 'bild.gif',
    IMAGE2: 'img.gif',
    LINK: 'link.gif',
    DATA_SOURCE: 'ds.gif',
    ORDERED_LIST: 'olist.gif',
    TABLE: 'table.gif',
    UNORDERED_LIST: 'uliste.gif',
    TAG: 'tag.gif',
    UNDEFINED: 'undef.gif',
    META: 'meta.gif',
    LANGUAGES: 'languages.gif',
    URL: 'url.gif',
}
/* eslint-enable @typescript-eslint/naming-convention */

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
})
export class ConstructPropertiesComponent
    extends BasePropertiesComponent<ConstructPropertiesFormData>
    implements AfterViewInit, OnChanges, OnInit {

    public readonly ConstructPropertiesMode = ConstructPropertiesMode;
    public readonly CONSTRUCT_ICONS = CONSTRUCT_ICONS;
    public readonly EditorControlStyle = EditorControlStyle;

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
        this.constructCategories$ = this.categoryHandler.listMapped().pipe(map(res => res.items));

        // Load the nodes and filter out all which do not have the required 'update' permission
        this.nodes$ = this.nodeData.watchAllEntities({ perms: true }).pipe(
            switchMap(nodes => {
                return combineLatest(nodes.map(node => {
                    return this.permissions.getInstancePermissions(AccessControlledType.NODE, node.id).pipe(
                        map(perms => perms.hasPermission(GcmsPermission.UPDATE_CONSTRUCTS) ? node : null),
                    )
                }));
            }),
            map(nodes => nodes.filter(node => node != null)),
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
                const ctl = this.form.get('nameI18n');
                ctl.setValidators(this.createNameValidator());
                ctl.updateValueAndValidity();
            }
        }
    }

    protected createForm(): FormGroup {
        return new FormGroup<FormProperties<ConstructPropertiesFormData>>({
            keyword: new FormControl<string>(null, Validators.required),
            nameI18n: new FormControl<CmsI18nValue>({}, this.createNameValidator()),
            descriptionI18n: new FormControl<CmsI18nValue>({}),
            icon: new FormControl<string>('', Validators.required),
            nodeIds: new FormControl<number[]>([], Validators.required),
            externalEditorUrl: new FormControl<string>(''),
            mayBeSubtag: new FormControl<boolean>(false),
            mayContainSubtags: new FormControl<boolean>(false),
            categoryId: new FormControl<number>(null),
            autoEnable: new FormControl<boolean>(false),
            openEditorOnInsert: new FormControl<boolean>(false),
            editorControlStyle: new FormControl<EditorControlStyle>(EditorControlStyle.ABOVE, Validators.required),
            editorControlsInside: new FormControl<boolean>(false),
        }, { updateOn: 'change' });
    }

    protected configureForm(value: Partial<TagTypeBO<Normalized>>, loud: boolean = false): void {
        const options = { emitEvent: loud };
        const nodesIdCtl = this.form.get('nodeIds');

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
        const validator = createI18nRequiredValidator((this.supportedLanguages || []).map(l => l.code), langs => {
            this.invalidLanguages = langs;
            this.changeDetector.markForCheck();
        });

        return validator;
    }

    protected onValueChange(): void {
        if ((this.value as any) === CONTROL_INVALID_VALUE) {
            return;
        }

        if (!this.form) {
            return;
        }

        const cleanedValue: ConstructPropertiesFormData = {
            nameI18n: this.value?.nameI18n ?? {},
            descriptionI18n: this.value?.descriptionI18n ?? {},
            keyword: this.value?.keyword || null,
            icon: this.value?.icon || null,
            nodeIds: this.value?.nodeIds || [],
            externalEditorUrl: this.value?.externalEditorUrl || '',
            mayBeSubtag: this.value?.mayBeSubtag || false,
            mayContainSubtags: this.value?.mayContainSubtags || null,
            categoryId: this.value?.categoryId || null,
            autoEnable: this.value?.autoEnable || false,
            openEditorOnInsert: this.value?.openEditorOnInsert ?? false,
            editorControlStyle: this.value?.editorControlStyle ?? EditorControlStyle.ABOVE,
            editorControlsInside: this.value?.editorControlsInside ?? false,
        };

        this.form.setValue(cleanedValue, { emitEvent: false });
        this.form.markAsPristine();
        this.form.updateValueAndValidity();
        this.changeDetector.markForCheck();
    }

    /**
     * Tracking function for ngFor for better performance.
     */
    identify(index: number, tagpartFormControl: UntypedFormControl): string {
        return tagpartFormControl.value['globalId'];
    }

    setActiveI18nTab(languageId: number): void {
        this.activeTabI18nLanguage = this.supportedLanguages.find(l => l.id === languageId);
    }

    activeI18nTabValueExists(languageCode: string): boolean {
        return [
            this.form.get('nameI18n')?.value as GtxI18nProperty,
            this.form.get('descriptionI18n')?.value as GtxI18nProperty,
        ].some(data => !!data?.[languageCode]);
    }
}
