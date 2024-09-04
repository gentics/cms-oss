import { SelectOption } from './tag-property-values';

/** Possible Tagpart types
 * @see https://www.gentics.com/Content.Node/cmp8/guides/restapi/json_TagpartType.html
 */
export enum TagPropertyType {
    STRING = 'STRING',
    RICHTEXT = 'RICHTEXT',
    PAGE = 'PAGE',
    IMAGE = 'IMAGE',
    FILE = 'FILE',
    PAGETAG = 'PAGETAG',
    OVERVIEW = 'OVERVIEW',
    LIST = 'LIST',
    UNORDEREDLIST = 'UNORDEREDLIST',
    ORDEREDLIST = 'ORDEREDLIST',
    TEMPLATETAG = 'TEMPLATETAG',
    FOLDER = 'FOLDER',
    SELECT = 'SELECT',
    MULTISELECT = 'MULTISELECT',
    BOOLEAN = 'BOOLEAN',
    DATASOURCE = 'DATASOURCE',
    VELOCITY = 'VELOCITY',
    BREADCRUMB = 'BREADCRUMB',
    NAVIGATION = 'NAVIGATION',
    NODE = 'NODE',
    /** @deprecated Use `CMSFORM` instead. */
    FORM = 'FORM',
    CMSFORM = 'CMSFORM',
}

/** Markup Language types
 * @see https://www.gentics.com/Content.Node/cmp8/guides/restapi/json_MarkupLanguage.html
 */
export enum MarkupLanguageType {
    HTML = 1,
    PHP = 2,
    CGIScript = 4,
    PerlScript = 5,
    ASP = 6,
    JSP = 7,
    WML = 8,
    CSS = 9,
    JavaScript = 10,
    XML = 11,
    XSL = 12,
    Text = 13,
    XHTML = 14,
    ASPNET = 15,
    HTM = 16,
    RSS = 17,
    INC = 18,
    JSON = 19,
    Generic = 20,
    FormsEmailTemplate = 21,
}

/**
 * Defines the concrete type of a TagPart.
 * @see https://www.gentics.com/Content.Node/cmp8/guides/gcn_part_types.html
 */
export enum TagPartType {
    /** Used for normal text (without HTML), which is entered using the tagfill dialog. Linebreaks will be transformed into <br> tags while rendering. */
    Text = 1,
    /**
     * Used for text containing HTML tags, which is entered using the tagfill dialog. Linebreaks will be transformed into <br> tags while rendering,
     * unless they occur within HTML tags or immediately follow an HTML tag.
     */
    TextHtml = 2,
    /** Used for text containing HTML tags, which is entered using the tagfill dialog or Aloha Editor. No conversion will be done while rendering. */
    Html = 3,
    /** The URL (page) part type provides an element to select a page within any accessible folder. */
    UrlPage = 4,
    /** Reference an image. */
    UrlImage = 6,
    /**	Reference a file. */
    UrlFile = 8,
    /**
     * Used for short text (no linebreaks), that does not contain HTML. The tagfill dialog will contain an input field.
     * No conversion will be done while rendering.
     */
    TextShort = 9,
    /** Variant of the type Text/HTML with a larger textarea in the tagfill dialog. */
    TextHtmlLong = 10,
    /** Render a tag of another page. */
    TagPage = 11,
    /**
     * The overview part type creates and overview which can be configured using a the provided wizard.
     * WARNING: Cannot be used together with other tag parts that are set as editable.
     */
    Overview = 13,
    /** Users can enter values (newline separated), and can choose whether to render them as ordered or unordered list. */
    List = 15,
    /** Users can enter values (newline separated), which will be rendered as unordered list. */
    ListUnordered = 16,
    /** Users can enter values (newline separated), which will be rendered as ordered list. */
    ListOrdered = 17,
    /**	Render a tag of another template */
    TagTemplate = 20,
    /**	Variant of HTML with a larger textarea in the tagfill dialog. */
    HtmlLong = 21,
    /** This part can be used to select a folder. */
    UrlFolder = 25,
    /** This part type provides static dataSources from which a user can pick a single value. */
    SelectSingle = 29,
    /**	This part type provides static dataSources from which a user can pick multiple values. */
    SelectMultiple = 30,
    /** Part type for input of boolean value. */
    Checkbox = 31,
    /** With this part type, the user can define individual dataSources in the tag. */
    DataSource = 32,
    /** The velocity part type can be used to evaluate all previously generated text using the velocity markup parser. */
    Velocity = 33,
    /** This part type can be used to generate breadcrumbs */
    Breadcrumb = 34,
    /** The navigation part type can be used to create a custom navigation using a velocity template. */
    Navigation = 35,
    /** The custom form part type can be used to create custom form elements. */
    HTMLCustomForm = 36,
    /** The custom form part type can be used to create custom form elements. */
    TextCustomForm = 37,
    /** The file upload part type is a URL (file) part type which has an additional file upload button. */
    FileUpload = 38,
    /** The folder upload part type is a URL (folder) part type which has an additional file upload button. */
    FolderUpload = 39,
    /** The node part is a drop-down menu with the nodes available to the user. */
    Node = 40,
    /** @deprecated */
    Form = 41,
    /**
     * The CMS Form part lets the user select a form from the CMS. See Gentics CMS Forms for details.
     * NOTE: Parts of this type can only be used in the new Editor User Interface.
     */
    CmsForm = 42,

    Handlebars = 43,
}

/** Pairs must be set correctly. */
export const TagPartTypePropertyType: Readonly<{ [key in TagPartType]: TagPropertyType }> = {
    [TagPartType.Text]: TagPropertyType.STRING,
    [TagPartType.TextHtml]: TagPropertyType.RICHTEXT,
    [TagPartType.Html]: TagPropertyType.RICHTEXT,
    [TagPartType.UrlPage]: TagPropertyType.PAGE,
    [TagPartType.UrlImage]: TagPropertyType.IMAGE,
    [TagPartType.UrlFile]: TagPropertyType.FILE,
    [TagPartType.TextShort]: TagPropertyType.STRING,
    [TagPartType.TextHtmlLong]: TagPropertyType.RICHTEXT,
    [TagPartType.TagPage]: TagPropertyType.PAGETAG,
    [TagPartType.Overview]: TagPropertyType.OVERVIEW,
    [TagPartType.List]: TagPropertyType.LIST,
    [TagPartType.ListUnordered]: TagPropertyType.UNORDEREDLIST,
    [TagPartType.ListOrdered]: TagPropertyType.ORDEREDLIST,
    [TagPartType.TagTemplate]: TagPropertyType.TEMPLATETAG,
    [TagPartType.HtmlLong]: TagPropertyType.RICHTEXT,
    [TagPartType.UrlFolder]: TagPropertyType.FOLDER,
    [TagPartType.SelectSingle]: TagPropertyType.SELECT,
    [TagPartType.SelectMultiple]: TagPropertyType.MULTISELECT,
    [TagPartType.Checkbox]: TagPropertyType.BOOLEAN,
    [TagPartType.DataSource]: TagPropertyType.DATASOURCE,
    [TagPartType.Velocity]: TagPropertyType.VELOCITY,
    [TagPartType.Breadcrumb]: TagPropertyType.BREADCRUMB,
    [TagPartType.Navigation]: TagPropertyType.NAVIGATION,
    [TagPartType.HTMLCustomForm]: TagPropertyType.RICHTEXT,
    [TagPartType.TextCustomForm]: TagPropertyType.STRING,
    [TagPartType.FileUpload]: TagPropertyType.FILE,
    [TagPartType.FolderUpload]: TagPropertyType.FOLDER,
    [TagPartType.Node]: TagPropertyType.NODE,
    [TagPartType.Form]: TagPropertyType.FORM,
    [TagPartType.CmsForm]: TagPropertyType.CMSFORM,
    [TagPartType.Handlebars]: TagPropertyType.RICHTEXT,
} as const;

/**
 * Selection Settings for type SELECT or MULTISELECT
 * @see https://www.gentics.com/Content.Node/cmp8/guides/restapi/json_SelectSetting.html
 */
export interface SelectSetting {

    /** DataSource ID */
    datasourceId: number;

    /** Rendering template */
    template: string;

    /** Selectable options of the dataSource */
    options: SelectOption[];

}

/**
 * Enumeration of the type of objects in an overview
 * @see https://www.gentics.com/Content.Node/cmp8/guides/restapi/json_ListType.html
 */
export enum ListType {
    PAGE = 'PAGE',
    FOLDER = 'FOLDER',
    FILE = 'FILE',
    IMAGE = 'IMAGE',
    UNDEFINED = 'UNDEFINED',
}

/**
 * Enumeration of the selection types in an overview
 * @see https://www.gentics.com/Content.Node/cmp8/guides/restapi/json_SelectType.html
 */
export enum SelectType {

    /** Listed objects are fetched from selected folders. */
    FOLDER = 'FOLDER',

    /** Listed objects are selected individually. */
    MANUAL = 'MANUAL',

    /** Listed objects are fetched from the folder of the currently rendered object. */
    AUTO = 'AUTO',

    /** Overview is not defined. */
    UNDEFINED = 'UNDEFINED',
}

/**
 * Model for overview settings
 * @see https://www.gentics.com/Content.Node/cmp8/guides/restapi/json_OverviewSetting.html
 */
export interface OverviewSetting {

    /** Allowed types of listed objects. */
    listTypes: ListType[];

    /** Allowed ways to select objects. */
    selectTypes: SelectType[];

    /** Flag to determine, whether sorting options shall be hidden. */
    hideSortOptions: boolean;

    /** Flag to determine, whether source channel shall be stored for every selected object. */
    stickyChannel: boolean;

}
