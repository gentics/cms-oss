import { CmsI18nValue } from './cms-i18n-value';
import { RegexValidationInfo } from './tag';
import { OverviewSetting, SelectSetting, TagPartType, TagPropertyType } from './tag-part-types';
import { Overview, SelectOption } from './tag-property-values';
import { DefaultModelType, ModelType } from './type-util';

/** Possible Tagpart types
 * @see https://www.gentics.com/Content.Node/guides/restapi/json_Property.html
 */
export type TagPartProperty =
    | StringTagPartProperty
    | BooleanTagPartProperty
    | FileTagPartProperty
    | FolderTagPartProperty
    | FormTagPartProperty
    | CmsFormTagPartProperty
    | ImageTagPartProperty
    | PageTagPartProperty
    | ListTagPartProperty
    | OrderedUnorderedListTagPartProperty
    | NodeTagPartProperty
    | PageTagTagPartProperty
    | TemplateTagTagPartProperty
    | SelectTagPartProperty
    | OverviewTagPartProperty
    | DataSourceTagPartProperty
    ;

export interface BaseTagPartProperty {
    globalId?: string;
    id?: number;
    partId?: number;
    type: TagPropertyType;
}

export interface StringTagPartProperty extends BaseTagPartProperty {
    type: TagPropertyType.STRING | TagPropertyType.RICHTEXT;
    stringValue?: string;
}

export interface BooleanTagPartProperty extends BaseTagPartProperty {
    type: TagPropertyType.BOOLEAN;
    booleanValue?: boolean;
}

export interface FileTagPartProperty extends BaseTagPartProperty {
    type: TagPropertyType.FILE;

    /**
     * The ID of the referenced file in the CMS.
     */
    fileId?: number;

    /**
     * The ID of the node that contains the file.
     * This field is not always set when a FileTagProperty is loaded,
     * because it depends on the activation of a multichanneling feature.
     */
    nodeId?: number;
}

export interface FolderTagPartProperty extends BaseTagPartProperty {
    type: TagPropertyType.FOLDER;

    /**
     * The ID of the referenced folder in the CMS.
     */
    folderId?: number;

    /**
     * The ID of the node that contains the folder.
     * This field is not always set when a FileTagProperty is loaded,
     * because it depends on the activation of a multichanneling feature.
     */
    nodeId?: number;
}

export interface FormTagPartProperty extends BaseTagPartProperty {
    type: TagPropertyType.FORM;
    formId: number;
}

export interface CmsFormTagPartProperty extends BaseTagPartProperty {
    type: TagPropertyType.CMSFORM;
    formId: number;
}

export interface ImageTagPartProperty extends BaseTagPartProperty {
    type: TagPropertyType.IMAGE;

    /**
     * The ID of the referenced image in the CMS.
     */
    imageId?: number;

    /**
     * The ID of the node that contains the image.
     * This field is not always set when a FileTagProperty is loaded,
     * because it depends on the activation of a multichanneling feature.
     */
    nodeId?: number;
}

export interface PageTagPartProperty extends BaseTagPartProperty {
    type: TagPropertyType.PAGE;

    /**
     * The ID of the referenced page in the CMS.
     *
     * IMPORTANT: When referencing a page in the CMS,
     * pageId (and optionally nodeId) must be set and stringValue must not be set.
     * When referencing an external page, only stringValue must
     * be set (to the URL of the page).
     */
    pageId?: number;

    /**
     * The node ID of the referenced page in the CMS.
     *
     * IMPORTANT: When referencing a page in the CMS,
     * pageId (and optionally nodeId) must be set and stringValue must not be set.
     * When referencing an external page, only stringValue must
     * be set (to the URL of the page).
     */
    nodeId?: number;

    /**
     * The URL of the referenced external page.
     *
     * IMPORTANT: When referencing a page in the CMS,
     * pageId (and optionally nodeId) must be set and stringValue must not be set.
     * When referencing an external page, only stringValue must
     * be set (to the URL of the page).
     */
    stringValue?: string;
}

export interface ListTagPartPropertyBase extends BaseTagPartProperty {
    type: TagPropertyType.LIST | TagPropertyType.ORDEREDLIST | TagPropertyType.UNORDEREDLIST;

    /**
     * The array of list items.
     */
    stringValues: string[];
}

export interface ListTagPartProperty extends ListTagPartPropertyBase {
    type: TagPropertyType.LIST;

    /**
     * If true, this list will be rendered as an ordered list.
     * If false, this list will be rendered as an unordered list.
     */
    booleanValue: boolean;
}

export interface OrderedUnorderedListTagPartProperty extends ListTagPartPropertyBase {
    type: TagPropertyType.ORDEREDLIST | TagPropertyType.UNORDEREDLIST;
}

export interface NodeTagPartProperty extends BaseTagPartProperty {
    type: TagPropertyType.NODE;
    nodeId?: number;
}

export interface PageTagTagPartProperty extends BaseTagPartProperty {
    type: TagPropertyType.PAGETAG;
    pageId?: number;
    contentTagId?: number;
}

export interface TemplateTagTagPartProperty extends BaseTagPartProperty {
    type: TagPropertyType.TEMPLATETAG;
    templateId?: number;
    templateTagId?: number;
}

export interface SelectTagPartProperty extends BaseTagPartProperty {
    type: TagPropertyType.SELECT | TagPropertyType.MULTISELECT;

    /** The id of the dataSource */
    datasourceId?: number;

    /** Possible options */
    options: SelectOption[];

    /** Selected options */
    selectedOptions?: SelectOption[];
}

export interface OverviewTagPartProperty extends BaseTagPartProperty {
    type: TagPropertyType.OVERVIEW;

    /** The values of the overview. */
    overview: Overview;
}

export interface DataSourceTagPartProperty extends BaseTagPartProperty {
    type: TagPropertyType.DATASOURCE;

    /** The created options in this dataSource. */
    options: SelectOption[];
}

/** @see https://www.gentics.com/Content.Node/guides/restapi/json_Part.html */
export interface TagPart<T extends ModelType = DefaultModelType> {
    /** global id of the part */
    globalId?: string;
    /** Name in the current language */
    name?: string;
    /** Name of the part in ever language */
    nameI18n: CmsI18nValue;
    /** Part keyword */
    keyword: string;
    /** Markup languag edientifier */
    markupLanguageId?: number;
    /** Order index of part (legacy/ portentially to be deprecated) */
    partOrder?: number;
    /** True if the part is hidden */
    hidden: boolean;
    /** True if the part is editable */
    editable: boolean;
    /** True if the part is live (inline) editable */
    liveEditable: boolean;
    /** True if the part is mandatory */
    mandatory: boolean;
    /** Part type */
    type: TagPropertyType;
    /** Part type ID */
    typeId: TagPartType;
    /** Local ID of the part */
    id?: number;
    /** Default property */
    defaultProperty?: TagPartProperty;
    /** Regular expression definition for validation of text parttypes */
    regex?: RegexValidationInfo;
    /** Flag for hiding the part in the Tag Editor */
    hideInEditor: boolean;
    /** External editor URL */
    externalEditorUrl?: string;
    /** of SelectOption	Possible options */
    options?: SelectOption[];
    /** Overview settings (if type is OVERVIEW) */
    overviewSettings?: OverviewSetting;
    /** Selection settings (if type is SELECT or MULTISELECT) */
    selectSettings?: SelectSetting;
}
