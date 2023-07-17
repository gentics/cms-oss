import {
    blacklistValidator,
    createI18nRequiredValidator,
} from '@admin-ui/common';
import { DataSourceDataService, MarkupLanguageDataService } from '@admin-ui/shared';
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
import {
    UntypedFormControl,
    UntypedFormGroup, Validators,
} from '@angular/forms';
import { BasePropertiesComponent, CONTROL_INVALID_VALUE, createNestedControlValidator } from '@gentics/cms-components';
import {
    AnyModelType,
    CmsI18nValue,
    DataSourceBO,
    Language,
    MarkupLanguage,
    Normalized,
    OverviewSetting,
    Raw,
    SelectSetting,
    TagPart,
    TagPartProperty,
    TagPartType,
    TagPartTypePropertyType,
    TagPartValidatorConfigs,
    TagPartValidatorId,
} from '@gentics/cms-models';
import { generateFormProvider } from '@gentics/ui-core';
import { Observable } from 'rxjs';

export interface TagPartPropertiesFormData {
    /** Part keyword */
    keyword: string;
    /** Name in the current language */
    nameI18n?: CmsI18nValue;

    /** Order index of part (legacy/ portentially to be deprecated) */
    partOrder: number;
    /** Part type ID */
    typeId: TagPartType;
    /** Markup languag edientifier */
    markupLanguageId: number;

    /** True if the part is editable */
    editable: boolean;
    /** True if the part is mandatory */
    mandatory: boolean;
    /** True if the part is hidden */
    hidden: boolean;
    /** True if the part is live (inline) editable */
    liveEditable: boolean;
    /** Flag for hiding the part in the Tag Editor */
    hideInEditor: boolean;

    /** External editor URL */
    externalEditorUrl: string;

    /** Regular expression definition for validation of text parttypes */
    regex?: TagPartValidatorId;
    /** Overview settings (if type is OVERVIEW) */
    overviewSettings?: OverviewSetting;
    /** Selection settings (if type is SELECT or MULTISELECT) */
    selectSettings?: SelectSetting;

    /** FROM TAG EDITOR */
    defaultProperty: TagPartProperty;
}

export enum ConstructPartPropertiesMode {
    CREATE = 'create',
    UPDATE = 'update',
}

export const VIABLE_CONSTRUCT_PART_TYPES: TagPartType[] = [
    TagPartType.Text,
    TagPartType.HtmlLong,
    TagPartType.Checkbox,
    TagPartType.UrlFolder,
    TagPartType.UrlPage,
    TagPartType.UrlImage,
    TagPartType.UrlFile,
    TagPartType.Node,
    TagPartType.CmsForm,
    TagPartType.DataSource,
    TagPartType.Overview,
    TagPartType.SelectSingle,
    TagPartType.SelectMultiple,
    TagPartType.Velocity,
];

export const REMOVED_CONSTRUCT_PART_TYPES: TagPartType[] = [
    TagPartType.TextShort,
    TagPartType.TextHtml,
    TagPartType.TextHtmlLong,
    TagPartType.Html,
    TagPartType.TagPage,
    TagPartType.List,
    TagPartType.ListUnordered,
    TagPartType.ListOrdered,
    TagPartType.TagTemplate,
    TagPartType.Breadcrumb,
    TagPartType.Navigation,
    TagPartType.HTMLCustomForm,
    TagPartType.TextCustomForm,
    TagPartType.FileUpload,
    TagPartType.FolderUpload,
    TagPartType.Form,
];

/**
 * Defines the data editable by the `ConstructPartPropertiesComponentMode`.
 *
 * To convey the validity state of the user's input, the onChange callback will
 * be called with `null` if the form data is currently invalid.
 */
@Component({
    selector: 'gtx-construct-part-properties',
    templateUrl: './construct-part-properties.component.html',
    styleUrls: ['./construct-part-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(ConstructPartPropertiesComponent)],
})
export class ConstructPartPropertiesComponent
    extends BasePropertiesComponent<TagPart>
    implements OnInit, OnChanges {

    public readonly VIABLE_CONSTRUCT_PART_TYPES = VIABLE_CONSTRUCT_PART_TYPES;
    public readonly REMOVED_CONSTRUCT_PART_TYPES = REMOVED_CONSTRUCT_PART_TYPES;
    // tslint:disable-next-line: variable-name
    public readonly TagPartTypePropertyType = TagPartTypePropertyType;
    // tslint:disable-next-line: variable-name
    public readonly ConstructPartPropertiesComponentMode = ConstructPartPropertiesMode;
    // tslint:disable-next-line: variable-name
    public readonly TagPartValidatorConfigs = TagPartValidatorConfigs;

    @Input()
    public mode: ConstructPartPropertiesMode;

    @Input()
    public supportedLanguages: Language[];

    @Input()
    public keywordBlacklist: string[];

    @Input()
    public orderBlacklist: number[];

    @Output()
    public isValidChange = new EventEmitter<boolean>();

    /** entity relation */
    public markupLanguages$: Observable<MarkupLanguage<Raw>[]>

    /** entity relation */
    public dataSources$: Observable<DataSourceBO<Raw>[]>

    public activeTabI18nLanguage: Language;
    public invalidLanguages: string[] = [];

    protected override delayedSetup = true;

    constructor(
        changeDetector: ChangeDetectorRef,
        private dataSourceDataService: DataSourceDataService,
        private markupLanguageData: MarkupLanguageDataService,
    ) {
        super(changeDetector);
    }

    ngOnInit(): void {
        super.ngOnInit();

        // fetch dependencies
        this.markupLanguages$ = this.markupLanguageData.watchAllEntities();
        this.dataSources$ = this.dataSourceDataService.watchAllEntities();
    }

    ngOnChanges(changes: Record<keyof ConstructPartPropertiesComponent, SimpleChange>): void {
        super.ngOnChanges(changes);

        if (changes.supportedLanguages) {
            const defaultLanguage = this.supportedLanguages?.[0];
            if (defaultLanguage) {
                this.activeTabI18nLanguage = defaultLanguage;
            }
            if (this.form) {
                this.form.get('nameI18n').updateValueAndValidity();
            }
        }

        if (changes.keywordBlacklist) {
            if (this.form) {
                this.form.get('keyword').updateValueAndValidity();
            }
        }

        if (changes.orderBlacklist) {
            if (this.form) {
                this.form.get('partOrder').updateValueAndValidity();
            }
        }
    }

    protected createForm(): UntypedFormGroup {
        return new UntypedFormGroup({
            // text
            keyword: new UntypedFormControl(this.value?.keyword ?? null, [
                Validators.required,
                blacklistValidator(() => this.keywordBlacklist),
            ]),
            // i18n Object
            nameI18n: new UntypedFormControl({}, createI18nRequiredValidator(
                () => {
                    return (this.supportedLanguages || []).map(l => l.code);
                },
                langs => {
                    this.invalidLanguages = langs;
                    this.changeDetector.markForCheck();
                }),
            ),

            // number
            partOrder: new UntypedFormControl(null, [
                Validators.required,
                Validators.min(1),
                Validators.max(999),
                blacklistValidator(() => this.orderBlacklist),
            ]),
            // select
            typeId: new UntypedFormControl(null, [Validators.required, blacklistValidator(() => REMOVED_CONSTRUCT_PART_TYPES)]),

            // checkbox
            editable: new UntypedFormControl(false),
            // checkbox
            mandatory: new UntypedFormControl(false),
            // checkbox
            hidden: new UntypedFormControl(false),
            // checkbox
            liveEditable: new UntypedFormControl(false),
            // checkbox
            hideInEditor: new UntypedFormControl(false),

            // text
            externalEditorUrl: new UntypedFormControl(null),
            // Tag-Editor
            defaultProperty: new UntypedFormControl(null, createNestedControlValidator()),

            // ///// TYPE-DEPENDANT:

            // ///// ONLY for HTML/Text inputs
            // select
            markupLanguageId: new UntypedFormControl(null),
            // select
            regex: new UntypedFormControl(null),

            // ///// ONLY for Select inputs
            // // select-part-settings
            selectSettings: new UntypedFormControl(null, createNestedControlValidator()),

            // ///// ONLY for Overview inputs
            // // overview-part-settings
            overviewSettings: new UntypedFormControl(null, createNestedControlValidator()),
        });
    }

    protected configureForm(value: TagPart<AnyModelType>, loud: boolean = false): void {
        // Don't do stuff if it's the initial change. Only perform the changes on item init
        if (value === undefined) {
            return;
        }

        const options = { emitEvent: loud };
        // Disable the keyword control if it's not creating a new one
        const keywordCtl = this.form.get('keyword');

        if (this.mode === ConstructPartPropertiesMode.CREATE) {
            keywordCtl.enable(options);
        } else {
            keywordCtl.disable(options);
        }

        // Text/HTML Controls
        const markupCtl = this.form.get('markupLanguageId');
        const regexCtl = this.form.get('regex');

        // Select Controls
        const selectSettingsCtl = this.form.get('selectSettings');

        // Overview Controls
        const overviewSettingsCtl = this.form.get('overviewSettings');

        // Default properties Control
        const defaultPropertyCtl = this.form.get('defaultProperty');

        // Disable specific controls first, and enable them once they are required below
        markupCtl.disable(options);
        regexCtl.disable(options);
        selectSettingsCtl.disable(options);
        overviewSettingsCtl.disable(options);
        defaultPropertyCtl.disable(options);

        switch (value?.typeId) {
            case TagPartType.SelectSingle:
            case TagPartType.SelectMultiple:
                defaultPropertyCtl.enable(options);
                selectSettingsCtl.enable(options);
                break;

            case TagPartType.DataSource:
                defaultPropertyCtl.enable(options);
                break;

            case TagPartType.List:
            case TagPartType.ListUnordered:
            case TagPartType.ListOrdered:
                // legacy
                break;

            case TagPartType.Overview:
                overviewSettingsCtl.enable(options);
                break;

            case TagPartType.TextHtml:
            case TagPartType.TextHtmlLong:
            case TagPartType.Html:
            case TagPartType.HtmlLong:
                defaultPropertyCtl.enable(options);
                markupCtl.enable(options);

            // eslint-disable-next-line no-fallthrough
            case TagPartType.Text:
            case TagPartType.TextShort:
                defaultPropertyCtl.enable(options);
                regexCtl.enable(options);
                break;

            default:
                if (value?.typeId) {
                    defaultPropertyCtl.enable(options);
                }
                break;
        }
    }

    protected assembleValue(formData: any): TagPart<Normalized> {
        if (formData == null) {
            return null;
        }

        const { globalId: _globalId, id: _id, keyword: _keyword, ...output } = formData;

        if (this.mode === ConstructPartPropertiesMode.UPDATE) {
            output.globalId = this.value?.globalId;
            output.id = this.value?.id;
            output.keyword = this.value?.keyword;
            output.name = this.value?.name;
            output.type = this.value?.type;
        } else {
            output.keyword = formData.keyword;
        }

        return output;
    }

    protected onValueChange(): void {
        if (this.form && this.value && (this.value as any) !== CONTROL_INVALID_VALUE) {
            this.form.setValue({
                keyword: this.value?.keyword ?? null,
                nameI18n: this.value?.nameI18n ?? null,
                partOrder: this.value?.partOrder ?? null,
                typeId: this.value?.typeId ?? null,
                editable: this.value?.editable ?? null,
                mandatory: this.value?.mandatory ?? null,
                hidden: this.value?.hidden ?? null,
                liveEditable: this.value?.liveEditable ?? null,
                hideInEditor: this.value?.hideInEditor ?? null,
                externalEditorUrl: this.value?.externalEditorUrl ?? null,
                defaultProperty: this.value?.defaultProperty ?? null,
                markupLanguageId: this.value?.markupLanguageId ?? null,
                regex: this.value?.regex ?? null,
                selectSettings: this.value?.selectSettings ?? null,
                overviewSettings: this.value?.overviewSettings ?? null,
            });
        }
    }
}
