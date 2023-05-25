import { createI18nRequiredValidator } from '@admin-ui/common';
import { PermissionsService } from '@admin-ui/core';
import { ConstructCategoryDataService, NodeDataService } from '@admin-ui/shared';
import {
    AfterViewInit,
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
import { UntypedFormControl, UntypedFormGroup, ValidatorFn, Validators } from '@angular/forms';
import { BasePropertiesComponent, CONTROL_INVALID_VALUE } from '@gentics/cms-components';
import {
    AccessControlledType,
    CmsI18nValue,
    ConstructCategoryBO,
    GcmsPermission,
    GtxI18nProperty,
    Language,
    Node,
    Normalized,
    Raw,
    TagPart,
    TagTypeBO,
} from '@gentics/cms-models';
import { generateFormProvider } from '@gentics/ui-core';
import { Observable, combineLatest } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';

export interface ConstructPropertiesFormData {
    nameI18n?: CmsI18nValue;
    descriptionI18n?: CmsI18nValue;
    keyword: string;
    icon: string;
    nodeIds: number[];
    parts?: TagPart[],
    externalEditorUrl?: string;
    mayBeSubtag?: boolean;
    mayContainSubtags?: boolean;
    categoryId?: number;
    autoEnable?: boolean;
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
    providers: [generateFormProvider(ConstructPropertiesComponent)],
})
export class ConstructPropertiesComponent
    extends BasePropertiesComponent<ConstructPropertiesFormData>
    implements AfterViewInit, OnChanges, OnInit {

    // tslint:disable-next-line: variable-name
    readonly ConstructPropertiesMode = ConstructPropertiesMode;
    readonly CONSTRUCT_ICONS = CONSTRUCT_ICONS;

    @Input()
    public mode: ConstructPropertiesMode;

    @Input()
    public supportedLanguages: Language[];

    @Output()
    public isValidChange = new EventEmitter<boolean>();

    public constructCategories$: Observable<ConstructCategoryBO<Normalized>[]>;
    public nodes$: Observable<Node<Raw>[]>;

    public activeTabI18nLanguage: Language;
    public invalidLanguages: string[] = [];

    constructor(
        changeDetector: ChangeDetectorRef,
        private categoryData: ConstructCategoryDataService,
        private nodeData: NodeDataService,
        private permissions: PermissionsService,
    ) {
        super(changeDetector);
    }

    ngOnInit(): void {
        super.ngOnInit();

        // load required dependencies into state
        this.constructCategories$ = this.categoryData.watchAllEntities();

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

    protected createForm(): UntypedFormGroup {
        return new UntypedFormGroup({
            keyword: new UntypedFormControl(null, Validators.required),
            nameI18n: new UntypedFormControl({}, this.createNameValidator()),
            descriptionI18n: new UntypedFormControl({}),
            icon: new UntypedFormControl('', Validators.required),
            nodeIds: new UntypedFormControl([], Validators.required),
            externalEditorUrl: new UntypedFormControl(''),
            mayBeSubtag: new UntypedFormControl(false),
            mayContainSubtags: new UntypedFormControl(false),
            categoryId: new UntypedFormControl(null),
            autoEnable: new UntypedFormControl(false),
        }, { updateOn: 'change' });
    }

    protected configureForm(value: Partial<TagTypeBO<Normalized>>, loud: boolean = false): void {
        const options = { emitEvent: loud };
        // const categorySortCtl = this.form.get('categorySortorder');
        const keywordCtl = this.form.get('keyword');
        const nodesIdCtl = this.form.get('nodeIds');

        // categorySortCtl.disable(options);
        keywordCtl.disable(options);
        nodesIdCtl.disable(options);

        // Only show the sorting, once a category has been selected
        // if (this.mode !== ConstructPropertiesMode.COPY && typeof value?.categoryId === 'number') {
        //     categorySortCtl.enable(options);
        // }

        // Can only be edited when we create a new construct
        if (this.mode === ConstructPropertiesMode.CREATE || this.mode === ConstructPropertiesMode.COPY) {
            keywordCtl.enable(options);
            nodesIdCtl.enable(options);
        }
    }

    protected assembleValue(formData: ConstructPropertiesFormData): ConstructPropertiesFormData {
        let output: Partial<ConstructPropertiesFormData> = {
            nameI18n: formData.nameI18n,
            descriptionI18n: formData.descriptionI18n,
            keyword: formData.keyword,
            icon: formData.icon,
            externalEditorUrl: formData.externalEditorUrl,
            mayBeSubtag: formData.mayBeSubtag,
            mayContainSubtags: formData.mayContainSubtags,
            categoryId: formData.categoryId,
            autoEnable: formData.autoEnable,
        };

        // Only add the node-ids when in creation mode
        if (this.mode === ConstructPropertiesMode.CREATE || this.mode === ConstructPropertiesMode.COPY) {
            output = {
                nodeIds: formData.nodeIds,
                ...output,
            }
        }

        return output as ConstructPropertiesFormData;
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

        if (this.form) {
            const cleanedValue: ConstructPropertiesFormData = {
                nameI18n: this.value?.nameI18n ?? {},
                descriptionI18n: this.value?.descriptionI18n ?? {},
                keyword: this.value?.keyword || null,
                icon: this.value?.icon || null,
                nodeIds: this.value?.nodeIds || [],
                externalEditorUrl: this.value?.externalEditorUrl || null,
                mayBeSubtag: this.value?.mayBeSubtag || false,
                mayContainSubtags: this.value?.mayContainSubtags || null,
                categoryId: this.value?.categoryId || null,
                autoEnable: this.value?.autoEnable || false,
            };

            this.form.setValue(cleanedValue, { emitEvent: false });
            this.form.markAsPristine();
            this.form.updateValueAndValidity();
            this.changeDetector.markForCheck();
        }
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
