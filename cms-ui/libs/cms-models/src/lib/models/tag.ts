import { schema } from 'normalizr';
import { CmsI18nValue } from './cms-i18n-value';
import { InstancePermissionItem } from './permissions';
import { TagPart, TagPartProperty } from './tag-part';
import { DefaultModelType, IndexByKey, ModelType } from './type-util';
import { User } from './user';
import { ConstructCategory } from './construct-category';

export type TagTypeType = 'CONTENTTAG' | 'TEMPLATETAG' | 'OBJECTTAG';

/**
 * The normalizr schema for Tags
 */
export const tagSchema = new schema.Entity('tag');

/** Maps keys to their respective TagProperties in a Tag. */
export interface TagPropertyMap extends IndexByKey<TagPartProperty> { }

/** Represents a Tag (basically an instance of a TagType). */
export interface Tag {
    /** The ID of the tag. */
    id: number;

    /** The name of the tag */
    name: string;

    /** The ID of the `TagType`, of which this tag is an instance. */
    constructId: number;

    /** The `TagType`, of which this tag is an instance. */
    construct?: TagType;

    /** True, if this tag is active. */
    active: boolean;

    /**
     * If the tag is a root element, and therefore {@link inherited} can be changed.
     * @readonly
     */
    root: boolean;
    /**
     * If this tag is inherited or not.
     * @readonly
     */
    inherited: boolean;

    /** The type of the tag (content tag, object tag, or template tag). */
    type: TagTypeType;

    /** Properties of the contenttag (representing the values in GCN) */
    properties: TagPropertyMap;
}

/**
 * Maps the name of a tag to its `Tag` instance - used to declare
 * the `tags` property of an item, e.g., `Page.tags`.
 */
export interface Tags {
    [name: string]: Tag;
}

/** Represents a tag that is used as an object property. */
export interface ObjectTag extends Tag {

    /** The name used for displaying this tag to the user. */
    displayName: string;

    /** Description of the object property. */
    description?: string;

    /** Whether or not the object property must be set on its container item. */
    required: boolean;

    /** True, if this object property is inherited by child items (e.g., from a folder or a template to pages). */
    inheritable: boolean;

    /** The name of the category, to which the object property belongs. */
    categoryName?: string;

    /** The id of the category, to which the object property belongs. */
    categoryId?: number;

    /** Used for sorting the object properties of a container item. */
    sortOrder: number;

    /** True, if the current user may not edit this object tag. */
    readOnly: boolean;

    type: 'OBJECTTAG';
}

/** Represents a tag that is defined by a template. */
export interface TemplateTag extends Tag {

    /** True if the Tag can be edited in Pages. */
    editableInPage: boolean;

    /** True if the Tag has to be filled in by all Pages. */
    mandatory: boolean;
}

/**
 * Represents regex validation information for string TagParts.
 */
export interface RegexValidationInfo {

    id: TagPartValidatorId;

    /**
     * The human readable name of the validation expression (e.g., 'Number (natural)').
     */
    name: string;

    /**
     * The description of the validation expression, which should also be used as the error messages
     * if validation fails.
     */
    description: string;

    /**
     * The regular expression as a string.
     */
    expression: string;
}

/**
 * The normalizr schema for TagTypes
 */
export const tagTypeSchema = new schema.Entity('tagType');

/**
 * Describes a TagType in GCMS.
 * Note that TagTypes are called Constructs by the REST API.
 */
// eslint-disable-next-line @typescript-eslint/no-unused-vars
export interface TagTypeBase<T extends ModelType> {

    /** primary identifier of a construct */
    globalId?: string;

    /** The keyword of the TagType. */
    keyword: string;

    /** Name in the current language */
    name?: string;

    /** The name of the tag in multiple languages */
    nameI18n?: CmsI18nValue;

    /** Description in the current language */
    description?: string;

    /** Description of th etag in multiple languages */
    descriptionI18n?: CmsI18nValue;

    /** The list of TagParts of this TagType. */
    parts: TagPart[];

    /**
     * If an CustomTagEditor should be used for this TagType, this property contains its URL.
     * If the GenticsTagEditor should be used, this property is not set.
     */
    externalEditorUrl?: string;

    /** Whether a tag of this construct may be inserted/nested in other tags */
    mayBeSubtag?: boolean;
    /** Whether this construct may contain other tags. */
    mayContainSubtags?: boolean;
    /** Construct id of this construct */
    constructId?: number;
    /** Creator of the construct */
    creator?: User;
    /** Creation Date of the construct */
    cdate?: number;
    /** Last Editor of the construct */
    editor?: User;
    /** Last Edit Date of the construct */
    edate?: number;
    /** Category id of the construct */
    categoryId?: number;
    /** Category of the construct */
    category?: ConstructCategory;
    /** Order for the category that was set */
    categorySortorder?: number;
    /** True if the construct shall be visible in the menu, false if not */
    visibleInMenu?: boolean;
    /** True if tags of this construct shall be enabled by default */
    autoEnable?: boolean;
    /** If the tag-editor should be opened when the tag get's inserted in a page. */
    openEditorOnInsert?: boolean;
    /** How the controls should be displayed in the page. */
    editorControlStyle?: EditorControlStyle;
    /** If the controls should be displayed inside/over the tag. */
    editorControlsInside?: boolean;
}

export enum EditorControlStyle {
    ABOVE = 'ABOVE',
    ASIDE = 'ASIDE',
    CLICK = 'CLICK',
}

/** Data model as defined by backend. */
export interface TagType<T extends ModelType = DefaultModelType> extends TagTypeBase<T> {
    /** Internal ID of the object property definition */
    id: number;
}

/**
 * Data model as defined by frontend.
 * @deprecated Create your own application specific type/business object instead.
 */
export interface TagTypeBO<T extends ModelType = DefaultModelType> extends TagTypeBase<T>, InstancePermissionItem {
    /** Internal ID of the object property definition */
    id: string;
}

export type Construct = TagType;
/** @deprecated */
export type ConstructBO = TagTypeBO;

/**
 * @returns The TagPart that corresponds to the specified TagProperty
 * or null if no matching TagPart can be found in the TagType.
 */
export function findTagPart(property: TagPartProperty, tagType: TagType): TagPart {
    if (tagType.parts) {
        const tagPart = tagType.parts.find(part => property.partId === part.id
            || (part.defaultProperty != null && property.partId === part.defaultProperty.partId),
        );
        return tagPart || null;
    } else {
        return null;
    }
}

/** The current Status of a Tag */
export interface TagStatus {
    /** The icon of the construct */
    constructIcon: string;
    /** The ID of the construct */
    contstructId: number;
    /** The name of the construct */
    constructName: string;
    /** Name of the Tag */
    name: string;
    /** How many pages are in sync */
    inSync: number;
    /** How many pages have incompatible tags */
    incompatible: number;
    /** How many pages are missing the tag entirely */
    missing: number;
    /** How many pages are out of sync */
    outOfSync: number;
}

/**
 * Business-Object representation of a Tag-Status.
 * @deprecated Create your own application specific type/business object instead.
 */
export interface TagStatusBO extends TagStatus {
    /** Pseudo ID - is actually the `name` property, but is required for internal application usage. */
    id: string;
}

/** IDs for generic validator config of `RegexValidationInfo`. */
export enum TagPartValidatorId {
    REG_INTEGER = 1001,
    REG_EMAIL = 1004,
    REG_NOTEMPTY = 1005,
    REG_TEXT_SHORT = 1008,
    REG_TEXT = 1009,
    REG_FLOAT = 1010,
    REG_TEXT_SIMPLE = 1011,
    REG_LONG = 1012,
    REG_TEXT_RESTRICTED = 1013,
    REG_URL = 1015,
    REG_FOLDERNAME = 1017,
    REG_FILENAME = 1018,
    REG_DATE = 1019,
    REG_TIME = 1020,
    REG_TIME_ALT = 1021,
    REG_SHORT = 1022,
    REG_DATE_ALT = 1023,
    REG_PATH = 1024,
    REG_HOSTNAME = 1025,
    REG_TEXT_UNIQUE = 1026,
    REG_PLZ = 1027,
    REG_PHONE = 1028,
    REG_PRICE = 1029,
    REG_FILESHARE = 1030,
    REG_NODETAG = 1031,
}

/** Generic validator data for `RegexValidationInfo`. */
// eslint-disable-next-line @typescript-eslint/naming-convention
export const TagPartValidatorConfigs: Readonly<{ [key in TagPartValidatorId]: RegexValidationInfo; }> = {

    // Zahl (natürlich)
    [TagPartValidatorId.REG_INTEGER]: {
        id: TagPartValidatorId.REG_INTEGER,
        name: 'validator_name_number_natural',
        description: null,
        expression: '^[1-9][0-9]{0,8}$',
    },

    // E-Mail Adresse
    [TagPartValidatorId.REG_EMAIL]: {
        id: TagPartValidatorId.REG_EMAIL,
        name: 'validator_name_email_address',
        description: null,
        expression: '^([-_.&0-9a-zA-Z+])+@[0-9a-z]([-.]?[0-9a-z])*.[a-z]{2,}$',
    },

    // Nicht leer
    [TagPartValidatorId.REG_NOTEMPTY]: {
        id: TagPartValidatorId.REG_NOTEMPTY,
        name: 'validator_name_must_not_be_emty',
        description: null,
        expression: '^.+$',
    },

    // Text (kurz)
    [TagPartValidatorId.REG_TEXT_SHORT]: {
        id: TagPartValidatorId.REG_TEXT_SHORT,
        name: 'validator_name_text_short',
        description: null,
        expression: '^.{1,255}$',
    },

    // Text (lang)
    [TagPartValidatorId.REG_TEXT]: {
        id: TagPartValidatorId.REG_TEXT,
        name: 'validator_name_text_long',
        description: null,
        expression: '^.{255}$',
    },

    // Zahl (reell)
    [TagPartValidatorId.REG_FLOAT]: {
        id: TagPartValidatorId.REG_FLOAT,
        name: 'validator_name_number_real',
        description: null,
        expression: '^[-+]{0,1}[0-9]{1,9}.{0,1}[0-9]{0,2}$',
    },

    // Text (einfach)
    [TagPartValidatorId.REG_TEXT_SIMPLE]: {
        id: TagPartValidatorId.REG_TEXT_SIMPLE,
        name: 'validator_name_text_simple',
        description: null,
        expression: '^[ßäöüÄÖÜa-zA-Z .-]{1,50}$',
    },

    // Zahl (ganz)
    [TagPartValidatorId.REG_LONG]: {
        id: TagPartValidatorId.REG_LONG,
        name: 'validator_name_number_integer',
        description: null,
        expression: '^[+-]{0,1}[0-9]{1,9}$',
    },

    // Text (Benutzername)
    [TagPartValidatorId.REG_TEXT_RESTRICTED]: {
        id: TagPartValidatorId.REG_TEXT_RESTRICTED,
        name: 'validator_name_text_username_password',
        description: null,
        expression: '^[a-zA-Z0-9._@+-]{4,40}$',
    },

    // Web Adresse
    [TagPartValidatorId.REG_URL]: {
        id: TagPartValidatorId.REG_URL,
        name: 'validator_name_www_address',
        description: null,
        expression: '^https?:\\/\\/(?:www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+.~#?&\\/=]*)$',
    },

    // Ordnername
    [TagPartValidatorId.REG_FOLDERNAME]: {
        id: TagPartValidatorId.REG_FOLDERNAME,
        name: 'validator_name_foldername',
        description: null,
        expression: '^[0-9ßäöüÄÖÜa-zA-Z .-]{1,255}$',
    },

    // Filename
    [TagPartValidatorId.REG_FILENAME]: {
        id: TagPartValidatorId.REG_FILENAME,
        name: 'validator_name_1148.filename',
        description: null,
        expression: '^(.){1,64}$',
    },

    // Datum (fix)
    [TagPartValidatorId.REG_DATE]: {
        id: TagPartValidatorId.REG_DATE,
        name: 'validator_name_date_fix',
        description: null,
        expression: '^([0-9]{1,2})[.,_/ -]([0-9]{1,2})[.,_/ -]([0-9]{2}|[0-9]{4})$',
    },

    // Uhrzeit (fix)
    [TagPartValidatorId.REG_TIME]: {
        id: TagPartValidatorId.REG_TIME,
        name: 'validator_name_time_fix',
        description: null,
        expression: '^([0-9]{1,2}):([0-9]{1,2})$',
    },

    // Uhrzeit (alternativ)
    [TagPartValidatorId.REG_TIME_ALT]: {
        id: TagPartValidatorId.REG_TIME_ALT,
        name: 'validator_name_time_alternative',
        description: null,
        expression: '^([0-1]?[0-9]|2[0-3])(:([0-5]?[0-9])|())$',
    },

    // Zahl (natürlich, klein)
    [TagPartValidatorId.REG_SHORT]: {
        id: TagPartValidatorId.REG_SHORT,
        name: 'validator_name_number_natural_small',
        description: null,
        expression: '^[1-5]{0,1}[0-9]$',
    },

    // Datum (alternativ)
    [TagPartValidatorId.REG_DATE_ALT]: {
        id: TagPartValidatorId.REG_DATE_ALT,
        name: 'validator_name_date_alternative',
        description: null,
        expression: '^([0-2]?[0-9]|3[0-1])([.:_ /-]([0]?[0-9]|1[0-2])([.:_ /-]([0-9]{1,2}|[0-9]{4})|[.:_ /-]|())|[.:_ /-]|())$',
    },

    // Verzeichnispfad
    [TagPartValidatorId.REG_PATH]: {
        id: TagPartValidatorId.REG_PATH,
        name: 'validator_name_directory_path',
        description: null,
        expression: '^/{0,1}([a-zA-Z0-9._-]{1,64}/{0,1}){0,127}$',
    },

    // Hostname
    [TagPartValidatorId.REG_HOSTNAME]: {
        id: TagPartValidatorId.REG_HOSTNAME,
        name: 'validator_name_hostname',
        description: null,
        expression: '^[0-9a-z]([-.]?[0-9a-z:])*$',
    },

    // Text (eindeutig)
    [TagPartValidatorId.REG_TEXT_UNIQUE]: {
        id: TagPartValidatorId.REG_TEXT_UNIQUE,
        name: 'validator_name_text_unique',
        description: null,
        expression: '^[a-z0-9]{3,64}$',
    },

    // Postleitzahl
    [TagPartValidatorId.REG_PLZ]: {
        id: TagPartValidatorId.REG_PLZ,
        name: 'validator_name_1166.zip_code',
        description: null,
        expression: '[A-Z]{0,2}[-]{0,1}[0-9]{4,6}',
    },

    // Telefon/Faxnummer
    [TagPartValidatorId.REG_PHONE]: {
        id: TagPartValidatorId.REG_PHONE,
        name: 'validator_name_telefonfaxnummer',
        description: null,
        expression: '^[0-9-/ +()]{4,25}$',
    },

    // Preis (SN)
    [TagPartValidatorId.REG_PRICE]: {
        id: TagPartValidatorId.REG_PRICE,
        name: 'validator_name_preis_sn',
        description: null,
        expression: '^[0-9]{1,12}.{0,1}[0-9]{0,2}$',
    },

    // Fileshare
    [TagPartValidatorId.REG_FILESHARE]: {
        id: TagPartValidatorId.REG_FILESHARE,
        name: 'validator_name_fileshare',
        description: null,
        expression: '^\\\\.*$',
    },

    // Node Tag
    [TagPartValidatorId.REG_NODETAG]: {
        id: TagPartValidatorId.REG_NODETAG,
        name: 'validator_name_1173.node_tag',
        description: null,
        expression: '^[a-zA-Z0-9_-]{3,255}$',
    },
} as const;
