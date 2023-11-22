import {
    EditableObjectTag,
    EditableTag,
    MultiValidationResult,
    RegexValidationInfo,
    StringTagPartProperty,
    TagEditorContext,
    TagPart,
    TagPartProperty,
    TagPartType,
    TagPropertyMap,
    TagPropertyType,
    TagType,
    ValidationResult,
    VariableTagEditorContext,
} from '@gentics/cms-models';
import { BehaviorSubject } from 'rxjs';
import { TagEditorContextImpl } from '../app/tag-editor/common/impl/tag-editor-context-impl';
import { getMockTagEditorTranslator } from '../app/tag-editor/common/impl/tag-editor-context.spec';
import { getExampleNodeData, getExamplePageData } from './test-data.mock';

/**
 * Contains test data and mocks for TagEditor and TagPropertyEditor tests.
 */


/**
 * Used to capture required and optional information for creating mocked EditableTags.
 * All properties specific to the TagProperty type will be copied to the TagProperty object.
 */
export type MockTagPropertyInfo<T extends TagPartProperty> = Partial<T> & {
    /** Defaults to true if not set. */
    editable?: boolean;
    hidden?: boolean;
    hideInEditor?: boolean;
    liveEditable?: boolean;
    keyword?: string;
    mandatory?: boolean;
    type: TagPropertyType;
    typeId: TagPartType;
};

/** Captures information about a mocked TagType. */
export interface MockTagTypeInfo {
    id?: number;
    keyword?: string;
    name?: string;
    icon?: string;
}

/** Captures information about a mocked ObjectTag and its TagType. */
export interface MockObjectTagInfo {
    tagType?: MockTagTypeInfo;
    name?: string;
    displayName?: string;
    description?: string;
    required?: boolean;
    inheritable?: boolean;
    categoryName?: string;
    sortOrder?: number;
}

/**
 * MockTagPropertyInfo properties that should not be copied to a TagProperty.
 * For some reason, declaring the variable with `const` or `let` would initialize
 * it after it is first used inside a function, which means that it would be part of
 * the temporal dead zone - that is why we need to declare it with `var`.
 */
// eslint-disable-next-line no-var
var excludedProperties: Set<string>;

/** Gets the set of MockTagPropertyInfo properties that should not be copied to a TagProperty. */
function getExludedProperties(): Set<string> {
    if (!excludedProperties) {
        excludedProperties =  new Set([ 'editable', 'hidden', 'hideInEditor', 'keyword', 'mandatory', 'type', 'typeId' ]);
    }
    return excludedProperties;
}

/**
 * Creates a mocked EditableTag using the specified tag property infos. The information in each tagPropInfo will
 * be used to set up a TagPart and TagProperty. If no keyword is specified for a tag property, `property${index}` is used.
 */
export function mockEditableTag<T extends TagPartProperty>(tagPropInfos: MockTagPropertyInfo<T>[], tagTypeInfo: MockTagTypeInfo = { }): EditableTag {
    const keywords: string[] = [];
    const mockedProperties: TagPartProperty[] = [];
    const tagPropertyMap: TagPropertyMap = { };
    for (let i = 0; i < tagPropInfos.length; ++i) {
        const keyword = tagPropInfos[i].keyword || `property${i}`;
        keywords.push(keyword);
        const prop = createTagProperty(tagPropInfos[i], i, i);
        mockedProperties.push(prop);
        tagPropertyMap[keyword] = prop;
    }

    const tagType: TagType = {
        id: typeof tagTypeInfo.id === 'number' ? tagTypeInfo.id : 4711,
        keyword: tagTypeInfo.keyword || 'test_tagtype',
        name: tagTypeInfo.name || 'Test Tag',
        icon: tagTypeInfo.icon || 'tag.gif',
        parts: [],
    };

    for (let i = 0; i < mockedProperties.length; ++i) {
        const src = tagPropInfos[i];
        tagType.parts.push({
            id: i,
            defaultProperty: { ...mockedProperties[i] },
            editable: typeof src.editable === 'boolean' ? src.editable : true,
            hidden: !!src.hidden,
            hideInEditor: !!src.hideInEditor,
            liveEditable: !!src.liveEditable,
            keyword: keywords[i],
            mandatory: !!src.mandatory,
            name: keywords[i],
            nameI18n: {},
            type: src.type,
            typeId: src.typeId,
        });
    }

    const tag: EditableTag = {
        active: true,
        id: 1234,
        constructId: tagType.id,
        construct: tagType,
        name: 'test0',
        type: 'CONTENTTAG',
        properties: tagPropertyMap,
        tagType: tagType,
    };

    return tag;
}

/**
 * Creates a mocked `EditableObjectTag` using the specified tag property infos. The information in each tagPropInfo will
 * be used to set up a TagPart and TagProperty. If no keyword is specified for a tag property, `property${index}` is used.
 */
export function mockEditableObjectTag<T extends TagPartProperty>(
    tagPropInfos: MockTagPropertyInfo<T>[],
    objTagInfo: MockObjectTagInfo = { },
): EditableObjectTag {
    const tag = mockEditableTag(tagPropInfos, objTagInfo.tagType);
    let tagName = objTagInfo.name || tag.name;
    if (!tagName.startsWith('object.')) {
        tagName = 'object.' + tagName;
    }

    const objectTag: EditableObjectTag = {
        ...tag,
        type: 'OBJECTTAG',
        name: tagName,
        displayName: objTagInfo.displayName || tagName,
        description: objTagInfo.description || '',
        categoryName: objTagInfo.categoryName || 'CategoryA',
        inheritable: typeof objTagInfo.inheritable === 'boolean' ? objTagInfo.inheritable : false,
        sortOrder: typeof objTagInfo.sortOrder === 'number' ? objTagInfo.sortOrder : 0,
        required: typeof objTagInfo.required === 'boolean' ? objTagInfo.required : false,
        readOnly: false,
    };
    return objectTag;
}

/**
 * Creates a TagProperty of type T with all the properties of src (except for EXCLUDED_PROPERTIES).
 */
function createTagProperty<T extends TagPartProperty>(src: MockTagPropertyInfo<T>, id: number, partId: number): T {
    const prop: any = {
        id: id,
        partId: partId,
        type: src.type,
    };
    const excludedProps = getExludedProperties();
    for (const key of Object.keys(src)) {
        if (!excludedProps.has(key)) {
            prop[key] = (<any> src)[key];
        }
    }
    return prop;
}

/** Creates an example EditableTag object with String TagParts. */
export function getExampleEditableTag(): EditableTag {
    const tagPropInfos: MockTagPropertyInfo<StringTagPartProperty>[] = [
        {
            stringValue: '',
            type: TagPropertyType.STRING,
            typeId: TagPartType.Text,
        },
        {
            stringValue: '',
            type: TagPropertyType.STRING,
            typeId: TagPartType.Text,
        },
        {
            mandatory: true,
            stringValue: '',
            type: TagPropertyType.STRING,
            typeId: TagPartType.Text,
        },
        {
            editable: false,
            stringValue: '',
            type: TagPropertyType.STRING,
            typeId: TagPartType.Text,
        },
        {
            hideInEditor: true,
            stringValue: '',
            type: TagPropertyType.STRING,
            typeId: TagPartType.Text,
        },
        {
            hidden: true,
            stringValue: '',
            type: TagPropertyType.STRING,
            typeId: TagPartType.Text,
        },
    ];
    return mockEditableTag(tagPropInfos);
}

/**
 * Creates a mocked TagEditorContext for the specified tag.
 * @param tag The tag, for which the context should be created.
 * @param contextInfo (optional) This can be used to customize the TagEditorContext.
 */
export function getMockedTagEditorContext(
    tag: EditableTag,
    contextInfo: Partial<TagEditorContext> = { },
): TagEditorContext {
    if (!contextInfo.page) {
        contextInfo.page = getExamplePageData();
    }
    if (typeof contextInfo.readOnly !== 'boolean') {
        contextInfo.readOnly = false;
    }
    if (!contextInfo.node) {
        contextInfo.node = getExampleNodeData();
    }
    if (typeof contextInfo.sid !== 'number') {
        contextInfo.sid = 4711;
    }
    if (!contextInfo.translator) {
        contextInfo.translator = getMockTagEditorTranslator();
    }
    if (!contextInfo.variableContext) {
        contextInfo.variableContext = new BehaviorSubject<VariableTagEditorContext>({uiLanguage: 'en'});
    }
    if (!contextInfo.gcmsUiServices) {
        contextInfo.gcmsUiServices = {
            openRepositoryBrowser: jasmine.createSpy('openRepositoryBrowser'),
            openImageEditor: jasmine.createSpy('openImageEditor'),
            openUploadModal: jasmine.createSpy('openUploadModal'),
            restRequestDELETE: jasmine.createSpy('restRequestDELETE'),
            restRequestGET: jasmine.createSpy('restRequestGET'),
            restRequestPOST: jasmine.createSpy('restRequestPOST'),
        };
    }

    return TagEditorContextImpl.create(
        tag,
        contextInfo.readOnly,
        contextInfo.page,
        contextInfo.node,
        4711,
        contextInfo.translator,
        contextInfo.variableContext,
        contextInfo.gcmsUiServices,
    );
}

/**
 * Creates a RegexValidationInfo for natural numbers.
 */
export function getExampleNaturalNumberValidationInfo(): RegexValidationInfo {
    return {
        id: 0 as any,
        name: 'Natural Number',
        description: 'The string must be a natural number',
        expression: '^[0-9]+$',
    };
}

/**
 * Creates a ValidationResult of a successful validation of a set TagProperty.
 */
export function getExampleValidationSuccess(): ValidationResult {
    return { isSet: true, success: true };
}

/**
 * Creates a ValidationResult of a failed validation of a set TagProperty.
 */
export function getExampleValidationFailed(): ValidationResult {
    return { isSet: true, success: false, errorMessage: 'Validation error' };
}

/**
 * Creates a MultiValidationResult, which contains the specified result
 * for the specified tagPart.
 */
export function getMultiValidationResult(tagPart: TagPart, validationResult: ValidationResult): MultiValidationResult {
    const multiResult: MultiValidationResult = { };
    multiResult[tagPart.keyword] = validationResult;
    return multiResult;
}
