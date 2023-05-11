import {
    EditableObjectTag,
    EditableTag,
    StringTagPartProperty,
    TagPartProperty,
    TagPartType,
    TagPropertyType,
    TagType
} from '@gentics/cms-models';
import {
    mockEditableObjectTag,
    mockEditableTag,
    MockObjectTagInfo,
    MockTagPropertyInfo,
    MockTagTypeInfo
} from './test-tag-editor-data.mock';

describe('TagEditor Test Helper Functions', () => {

    it('mockEditableTag works without tagTypeInfo', () => {

        const tagPropInfos: MockTagPropertyInfo<StringTagPartProperty>[] = [
            {
                type: TagPropertyType.STRING,
                typeId: TagPartType.Text,
                stringValue: 'test'
            },
            {
                type: TagPropertyType.STRING,
                typeId: TagPartType.TextShort,
                stringValue: '',
                mandatory: true
            },
            {
                type: TagPropertyType.STRING,
                typeId: TagPartType.Text,
                stringValue: 'test non editable',
                editable: false
            },
            {
                type: TagPropertyType.STRING,
                typeId: TagPartType.Text,
                stringValue: 'test hidden',
                keyword: 'customKeyword',
                hidden: true
            },
            {
                type: TagPropertyType.STRING,
                typeId: TagPartType.Text,
                stringValue: 'test hide in editor',
                hideInEditor: true
            }
        ];

        const expectedTagProperties: TagPartProperty[] = [
            {
                id: 0,
                type: TagPropertyType.STRING,
                partId: 0,
                stringValue: 'test'
            },
            {
                id: 1,
                type: TagPropertyType.STRING,
                partId: 1,
                stringValue: ''
            },
            {
                id: 2,
                type: TagPropertyType.STRING,
                partId: 2,
                stringValue: 'test non editable'
            },
            {
                id: 3,
                type: TagPropertyType.STRING,
                partId: 3,
                stringValue: 'test hidden'
            },
            {
                id: 4,
                type: TagPropertyType.STRING,
                partId: 4,
                stringValue: 'test hide in editor'
            }
        ];

        const tagType: TagType = {
            id: 4711,
            name: 'Test Tag',
            keyword: 'test_tagtype',
            parts: [
                {
                    defaultProperty: { ...expectedTagProperties[0] },
                    editable: true,
                    hidden: false,
                    hideInEditor: false,
                    liveEditable: false,
                    id: 0,
                    keyword: 'property0',
                    mandatory: false,
                    name: 'property0',
                    nameI18n: {},
                    type: TagPropertyType.STRING,
                    typeId: TagPartType.Text
                },
                {
                    defaultProperty: { ...expectedTagProperties[1] },
                    editable: true,
                    hidden: false,
                    hideInEditor: false,
                    liveEditable: false,
                    id: 1,
                    keyword: 'property1',
                    mandatory: true,
                    name: 'property1',
                    nameI18n: {},
                    type: TagPropertyType.STRING,
                    typeId: TagPartType.TextShort
                },
                {
                    defaultProperty: { ...expectedTagProperties[2] },
                    editable: false,
                    hidden: false,
                    hideInEditor: false,
                    liveEditable: false,
                    id: 2,
                    keyword: 'property2',
                    mandatory: false,
                    name: 'property2',
                    nameI18n: {},
                    type: TagPropertyType.STRING,
                    typeId: TagPartType.Text
                },
                {
                    defaultProperty: { ...expectedTagProperties[3] },
                    editable: true,
                    hidden: true,
                    hideInEditor: false,
                    liveEditable: false,
                    id: 3,
                    keyword: 'customKeyword',
                    mandatory: false,
                    name: 'customKeyword',
                    nameI18n: {},
                    type: TagPropertyType.STRING,
                    typeId: TagPartType.Text
                },
                {
                    defaultProperty: { ...expectedTagProperties[4] },
                    editable: true,
                    hidden: false,
                    hideInEditor: true,
                    liveEditable: false,
                    id: 4,
                    keyword: 'property4',
                    mandatory: false,
                    name: 'property4',
                    nameI18n: {},
                    type: TagPropertyType.STRING,
                    typeId: TagPartType.Text
                }
            ],
            icon: 'tag.gif'
        };

        const expectedTag: EditableTag = {
            active: true,
            id: 1234,
            constructId: 4711,
            construct: tagType,
            name: 'test0',
            properties: {
                property0: { ...expectedTagProperties[0] },
                property1: { ...expectedTagProperties[1] },
                property2: { ...expectedTagProperties[2] },
                customKeyword: { ...expectedTagProperties[3] },
                property4: { ...expectedTagProperties[4] }
            },
            type: 'CONTENTTAG',
            tagType: tagType
        };

        const actualTag = mockEditableTag(tagPropInfos);
        expect(actualTag).toEqual(expectedTag);
    });


    it('mockEditableTag works with tagTypeInfo', () => {

        const tagPropInfos: MockTagPropertyInfo<StringTagPartProperty>[] = [
            {
                type: TagPropertyType.STRING,
                typeId: TagPartType.Text,
                stringValue: 'test'
            }
        ];

        const expectedTagProperties: TagPartProperty[] = [
            {
                id: 0,
                type: TagPropertyType.STRING,
                partId: 0,
                stringValue: 'test'
            }
        ];

        const tagTypeInfo: MockTagTypeInfo = {
            id: 1,
            keyword: 'custom_keyword',
            name: 'Custom Name',
        };

        const tagType: TagType = {
            id: tagTypeInfo.id,
            name: tagTypeInfo.name,
            keyword: tagTypeInfo.keyword,
            parts: [
                {
                    defaultProperty: { ...expectedTagProperties[0] },
                    editable: true,
                    hidden: false,
                    hideInEditor: false,
                    liveEditable: false,
                    id: 0,
                    keyword: 'property0',
                    mandatory: false,
                    name: 'property0',
                    nameI18n: {},
                    type: TagPropertyType.STRING,
                    typeId: TagPartType.Text
                }
            ],
            icon: 'tag.gif',
        };

        const expectedTag: EditableTag = {
            active: true,
            id: 1234,
            constructId: tagTypeInfo.id,
            construct: tagType,
            name: 'test0',
            properties: {
                property0: { ...expectedTagProperties[0] }
            },
            type: 'CONTENTTAG',
            tagType: tagType
        };

        const actualTag = mockEditableTag(tagPropInfos, tagTypeInfo);
        expect(actualTag).toEqual(expectedTag);
    });

    it('mockEditableObjectTag works without objTagInfo', () => {

        const tagPropInfos: MockTagPropertyInfo<StringTagPartProperty>[] = [
            {
                type: TagPropertyType.STRING,
                typeId: TagPartType.Text,
                stringValue: 'test'
            },
            {
                type: TagPropertyType.STRING,
                typeId: TagPartType.TextShort,
                stringValue: '',
                mandatory: true
            }
        ];

        const expectedTagProperties: TagPartProperty[] = [
            {
                id: 0,
                type: TagPropertyType.STRING,
                partId: 0,
                stringValue: 'test'
            },
            {
                id: 1,
                type: TagPropertyType.STRING,
                partId: 1,
                stringValue: ''
            }
        ];

        const tagType: TagType = {
            id: 4711,
            name: 'Test Tag',
            keyword: 'test_tagtype',
            parts: [
                {
                    defaultProperty: { ...expectedTagProperties[0] },
                    editable: true,
                    hidden: false,
                    hideInEditor: false,
                    liveEditable: false,
                    id: 0,
                    keyword: 'property0',
                    mandatory: false,
                    name: 'property0',
                    nameI18n: {},
                    type: TagPropertyType.STRING,
                    typeId: TagPartType.Text
                },
                {
                    defaultProperty: { ...expectedTagProperties[1] },
                    editable: true,
                    hidden: false,
                    hideInEditor: false,
                    liveEditable: false,
                    id: 1,
                    keyword: 'property1',
                    mandatory: true,
                    name: 'property1',
                    nameI18n: {},
                    type: TagPropertyType.STRING,
                    typeId: TagPartType.TextShort
                }
            ],
            icon: 'tag.gif'
        };

        const expectedTag: EditableObjectTag = {
            active: true,
            id: 1234,
            constructId: 4711,
            construct: tagType,
            name: 'object.test0',
            properties: {
                property0: { ...expectedTagProperties[0] },
                property1: { ...expectedTagProperties[1] }
            },
            type: 'OBJECTTAG',
            displayName: 'object.test0',
            description: '',
            categoryName: 'CategoryA',
            inheritable: false,
            required: false,
            sortOrder: 0,
            tagType: tagType,
            readOnly: false
        };

        const actualTag = mockEditableObjectTag(tagPropInfos);
        expect(actualTag).toEqual(expectedTag);
    });


    it('mockEditableObjectTag works with objTagInfo and tagType', () => {

        const tagPropInfos: MockTagPropertyInfo<StringTagPartProperty>[] = [
            {
                type: TagPropertyType.STRING,
                typeId: TagPartType.Text,
                stringValue: 'test'
            }
        ];

        const expectedTagProperties: TagPartProperty[] = [
            {
                id: 0,
                type: TagPropertyType.STRING,
                partId: 0,
                stringValue: 'test'
            }
        ];

        const objTagInfo: MockObjectTagInfo = {
            tagType: {
                id: 1,
                keyword: 'custom_keyword',
                name: 'Custom Name',
            },
            name: 'object.customName',
            displayName: 'Display Name Test',
            description: 'Description',
            categoryName: 'Custom Category',
            inheritable: true,
            required: false,
            sortOrder: 10,
        };

        const tagType: TagType = {
            id: objTagInfo.tagType.id,
            name: objTagInfo.tagType.name,
            keyword: objTagInfo.tagType.keyword,
            parts: [
                {
                    defaultProperty: { ...expectedTagProperties[0] },
                    editable: true,
                    hidden: false,
                    hideInEditor: false,
                    liveEditable: false,
                    id: 0,
                    keyword: 'property0',
                    mandatory: false,
                    name: 'property0',
                    nameI18n: {},
                    type: TagPropertyType.STRING,
                    typeId: TagPartType.Text
                }
            ],
            icon: 'tag.gif',
        };

        const expectedTag: EditableObjectTag = {
            active: true,
            id: 1234,
            constructId: objTagInfo.tagType.id,
            construct: tagType,
            name: objTagInfo.name,
            properties: {
                property0: { ...expectedTagProperties[0] }
            },
            type: 'OBJECTTAG',
            displayName: objTagInfo.displayName,
            description: objTagInfo.description,
            categoryName: objTagInfo.categoryName,
            inheritable: objTagInfo.inheritable,
            required: objTagInfo.required,
            sortOrder: objTagInfo.sortOrder,
            tagType: tagType,
            readOnly: false
        };

        const actualTag = mockEditableObjectTag(tagPropInfos, objTagInfo);
        expect(actualTag).toEqual(expectedTag);
    });

    it('mockEditableObjectTag works with objTagInfo without tagType', () => {

        const tagPropInfos: MockTagPropertyInfo<StringTagPartProperty>[] = [
            {
                type: TagPropertyType.STRING,
                typeId: TagPartType.Text,
                stringValue: 'test'
            }
        ];

        const expectedTagProperties: TagPartProperty[] = [
            {
                id: 0,
                type: TagPropertyType.STRING,
                partId: 0,
                stringValue: 'test'
            }
        ];

        const objTagInfo: MockObjectTagInfo = {
            displayName: 'Display Name Test',
            description: 'Description',
            categoryName: 'Custom Category',
            inheritable: true,
            required: false,
            sortOrder: 10,
        };

        const tagType: TagType = {
            id: 4711,
            name: 'Test Tag',
            keyword: 'test_tagtype',
            parts: [
                {
                    defaultProperty: { ...expectedTagProperties[0] },
                    editable: true,
                    hidden: false,
                    hideInEditor: false,
                    liveEditable: false,
                    id: 0,
                    keyword: 'property0',
                    mandatory: false,
                    name: 'property0',
                    nameI18n: {},
                    type: TagPropertyType.STRING,
                    typeId: TagPartType.Text
                }
            ],
            icon: 'tag.gif',
        };

        const expectedTag: EditableObjectTag = {
            active: true,
            id: 1234,
            constructId: 4711,
            construct: tagType,
            name: 'object.test0',
            properties: {
                property0: { ...expectedTagProperties[0] }
            },
            type: 'OBJECTTAG',
            displayName: objTagInfo.displayName,
            description: objTagInfo.description,
            categoryName: objTagInfo.categoryName,
            inheritable: objTagInfo.inheritable,
            required: objTagInfo.required,
            sortOrder: objTagInfo.sortOrder,
            tagType: tagType,
            readOnly: false
        };

        const actualTag = mockEditableObjectTag(tagPropInfos, objTagInfo);
        expect(actualTag).toEqual(expectedTag);
    });

});
