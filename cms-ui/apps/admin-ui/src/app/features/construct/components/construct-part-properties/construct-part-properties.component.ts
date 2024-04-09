import {
    createBlacklistValidator,
    createI18nRequiredValidator,
} from '@admin-ui/common';
import { I18nService } from '@admin-ui/core';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChange,
} from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import {
    CmsI18nValue,
    DataSource,
    Language,
    MarkupLanguage,
    OverviewSetting,
    Raw,
    RegexValidationInfo,
    SelectSetting,
    TagPartProperty,
    TagPartType,
    TagPartTypePropertyType,
    TagPartValidatorConfigs,
    TagPartValidatorId,
    TagPropertyType
} from '@gentics/cms-models';
import { generateFormProvider, generateValidatorProvider, setControlsEnabled } from '@gentics/ui-core';

export interface TagPartPropertiesFormData {
    globalId?: string;
    id?: string;
    name?: string;
    type?: TagPropertyType;

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

/** Using a symbol so the tag-part can be safely converted to JSON without extra handling. */
const TRANSLATED_NAME_PROP = Symbol('translated-name');

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
    providers: [
        generateFormProvider(ConstructPartPropertiesComponent),
        generateValidatorProvider(ConstructPartPropertiesComponent),
    ],
})
export class ConstructPartPropertiesComponent
    extends BasePropertiesComponent<TagPartPropertiesFormData>
    implements OnChanges {

    public readonly VIABLE_CONSTRUCT_PART_TYPES = VIABLE_CONSTRUCT_PART_TYPES;
    public readonly REMOVED_CONSTRUCT_PART_TYPES = REMOVED_CONSTRUCT_PART_TYPES;
    public readonly TRANSLATED_NAME_PROP = TRANSLATED_NAME_PROP;
    public readonly TagPartTypePropertyType = TagPartTypePropertyType;
    public readonly ConstructPartPropertiesMode = ConstructPartPropertiesMode;

    public readonly SORTED_VALIDATOR_CONFIGS: (RegexValidationInfo & { [TRANSLATED_NAME_PROP]: string })[];

    @Input()
    public mode: ConstructPartPropertiesMode;

    @Input()
    public supportedLanguages: Language[];

    @Input()
    public markupLanguages: MarkupLanguage<Raw>[] = [];

    @Input()
    public dataSources: DataSource<Raw>[] = [];

    @Input()
    public keywordBlacklist: string[];

    @Input()
    public orderBlacklist: number[];

    @Output()
    public isValidChange = new EventEmitter<boolean>();

    public activeTabI18nLanguage: Language;
    public invalidLanguages: string[] = [];

    protected override delayedSetup = true;

    constructor(
        changeDetector: ChangeDetectorRef,
        private i18n: I18nService,
    ) {
        super(changeDetector);

        this.SORTED_VALIDATOR_CONFIGS = Object.values(TagPartValidatorConfigs)
        // Translate the name once, so we don't have to do it everytime while sorting and then in the template as well.
            .map(config => ({
                ...config,
                [TRANSLATED_NAME_PROP]: i18n.instant('construct.' + config.name),
            }))
            .sort((a, b) => a[TRANSLATED_NAME_PROP].localeCompare(b[TRANSLATED_NAME_PROP]));
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

        if (changes.mode && this.form) {
            // Disable the keyword control if it's not creating a new one
            setControlsEnabled(this.form, ['keyword'], this.mode === ConstructPartPropertiesMode.CREATE, { onlySelf: true, emitEvent: false });
        }
    }

    protected createForm(): UntypedFormGroup {
        return new UntypedFormGroup({
            // Noop controls
            globalId: new UntypedFormControl(this.value?.globalId),
            id: new UntypedFormControl(this.value?.id),

            // text
            keyword: new UntypedFormControl(this.value?.keyword ?? null, [
                Validators.required,
                createBlacklistValidator(() => this.keywordBlacklist),
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
                createBlacklistValidator(() => this.orderBlacklist),
            ]),
            // select
            typeId: new UntypedFormControl(null, [Validators.required, createBlacklistValidator(() => REMOVED_CONSTRUCT_PART_TYPES)]),

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
            defaultProperty: new UntypedFormControl(null),

            // ///// TYPE-DEPENDANT:

            // ///// ONLY for HTML/Text inputs
            // select
            markupLanguageId: new UntypedFormControl(null),
            // select
            regex: new UntypedFormControl(null),

            // ///// ONLY for Select inputs
            // // select-part-settings
            selectSettings: new UntypedFormControl(null),

            // ///// ONLY for Overview inputs
            // // overview-part-settings
            overviewSettings: new UntypedFormControl(null),
        });
    }

    protected configureForm(value: TagPartPropertiesFormData, loud: boolean = false): void {
        // Don't do stuff if it's the initial change. Only perform the changes on item init
        if (value === undefined) {
            return;
        }

        const options = { emitEvent: loud };

        let markupEnabled = false;
        let regexEnabled = false;
        let selectSettingsEnabled = false;
        let overviewSettingsEnabled = false;
        let defaultPropertyEnabled = false;

        switch (value?.typeId) {
            case TagPartType.SelectSingle:
            case TagPartType.SelectMultiple:
                defaultPropertyEnabled = true;
                selectSettingsEnabled = true;
                break;

            case TagPartType.DataSource:
                defaultPropertyEnabled = true;
                break;

            case TagPartType.List:
            case TagPartType.ListUnordered:
            case TagPartType.ListOrdered:
                // legacy
                break;

            case TagPartType.Overview:
                defaultPropertyEnabled = true;
                overviewSettingsEnabled = true;
                break;

            case TagPartType.TextHtml:
            case TagPartType.TextHtmlLong:
            case TagPartType.Html:
            case TagPartType.HtmlLong:
                defaultPropertyEnabled = true;
                markupEnabled = true;

            // eslint-disable-next-line no-fallthrough
            case TagPartType.Text:
            case TagPartType.TextShort:
                defaultPropertyEnabled = true;
                regexEnabled = true;
                break;

            default:
                if (value?.typeId) {
                    defaultPropertyEnabled = true;
                }
                break;
        }

        setControlsEnabled(this.form, ['markupLanguageId'], markupEnabled, options);
        setControlsEnabled(this.form, ['regex'], regexEnabled, options);
        setControlsEnabled(this.form, ['selectSettings'], selectSettingsEnabled, options);
        setControlsEnabled(this.form, ['overviewSettings'], overviewSettingsEnabled, options);
        setControlsEnabled(this.form, ['defaultProperty'], defaultPropertyEnabled, options);
    }

    protected assembleValue(formData: TagPartPropertiesFormData): TagPartPropertiesFormData {
        if (formData == null) {
            return null;
        }

        const { globalId: _globalId, id: _id, keyword: _keyword, ...output } = formData;

        if (this.mode === ConstructPartPropertiesMode.UPDATE) {
            (output as TagPartPropertiesFormData).globalId = this.value?.globalId ?? formData.globalId;
            (output as TagPartPropertiesFormData).id = this.value?.id ?? formData.id;
            (output as TagPartPropertiesFormData).keyword = this.value?.keyword ?? this.form.get('keyword').value;
            output.name = this.value?.name;
            output.type = this.value?.type;
        } else {
            (output as TagPartPropertiesFormData).keyword = formData.keyword;
        }

        return output as TagPartPropertiesFormData;
    }
}
