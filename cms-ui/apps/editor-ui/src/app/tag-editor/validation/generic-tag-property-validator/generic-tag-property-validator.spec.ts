import {
    DataSourceTagPartProperty,
    EditableTag,
    FileTagPartProperty,
    FolderTagPartProperty,
    ImageTagPartProperty,
    ListTagPartProperty,
    NodeTagPartProperty,
    OrderedUnorderedListTagPartProperty,
    PageTagTagPartProperty,
    SelectTagPartProperty,
    StringTagPartProperty,
    TagPart,
    TagPartType,
    TagPropertyType,
    TemplateTagTagPartProperty,
    ValidationResult
} from '@gentics/cms-models';
import { getExampleEditableTag, mockEditableTag } from '../../../../testing/test-tag-editor-data.mock';
import { GenericTagPropertyValidator } from './generic-tag-property-validator';

describe('GenericTagPropertyValidator', () => {

    let tagValidator: GenericTagPropertyValidator;

    beforeEach(() => {
        tagValidator = new GenericTagPropertyValidator();
    });

    describe('validation of TagPropertyTypes without any configured properties', () => {

        let tag: EditableTag;
        let tagPart: TagPart;
        let tagProperty: StringTagPartProperty;
        let expectedResult: ValidationResult;

        beforeEach(() => {
            tag = getExampleEditableTag();
            tagPart = tag.tagType.parts[0];
            tagProperty = tag.properties[tagPart.keyword] as StringTagPartProperty;
            expectedResult = {
                isSet: true,
                success: true
            };
        });

        /**
         * For these tests a String TagProperty is used, because it does not have any (JavaScript object) properties
         * configured that need to be checked
         */

        it('validation of unset, non-mandatory property works', () => {
            tagPart.mandatory = false;

            tagProperty.stringValue = '';
            expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);

            tagProperty.stringValue = null;
            expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);

            tagProperty.stringValue = undefined;
            expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);

            delete tagProperty.stringValue;
            expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);
        });

        it('validation of unset, mandatory property works', () => {
            tagPart.mandatory = true;

            tagProperty.stringValue = '';
            expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);

            tagProperty.stringValue = null;
            expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);

            tagProperty.stringValue = undefined;
            expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);

            delete tagProperty.stringValue;
            expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);
        });

        it('validation of set, non-mandatory property works', () => {
            tagPart.mandatory = false;
            tagProperty.stringValue = 'Test';
            expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);
        });

        it('validation of set, mandatory property works', () => {
            tagPart.mandatory = true;
            tagProperty.stringValue = 'Test';
            expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);
        });

    });

    describe('validation of TagPropertyTypes with configured properties', () => {

        let expectedResultIsSet: ValidationResult;
        let expectedResultNotSet: ValidationResult;

        beforeEach(() => {
            expectedResultIsSet = {
                isSet: true,
                success: true
            };
            expectedResultNotSet = {
                isSet: false,
                success: true
            };
        });

        function assertResultsForAllTagProperties(tag: EditableTag, expectedResult: ValidationResult): void {
            tag.tagType.parts.forEach(tagPart => {
                const tagProperty = tag.properties[tagPart.keyword];
                expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);
            });
        }

        it('handles unset, mandatory TagProperties correctly', () => {
            const tag = mockEditableTag<PageTagTagPartProperty>([{
                type: TagPropertyType.PAGETAG,
                typeId: TagPartType.TagPage,
                mandatory: true
            }]);
            const tagPart = tag.tagType.parts[0];
            const tagProperty = tag.properties[tagPart.keyword];
            expect(tagValidator.validate(tagProperty, tagPart)).toEqual({
                isSet: false,
                success: false
            });
        });

        it('handles set, mandatory TagProperties correctly', () => {
            const tag = mockEditableTag<PageTagTagPartProperty>([{
                type: TagPropertyType.PAGETAG,
                typeId: TagPartType.TagPage,
                mandatory: true,
                pageId: 1234,
                contentTagId: 4711
            }]);
            const tagPart = tag.tagType.parts[0];
            const tagProperty = tag.properties[tagPart.keyword];
            expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResultIsSet);
        });

        it('handles unset DataSourceTagProperties correctly', () => {
            const tag = mockEditableTag<DataSourceTagPartProperty>([
                {
                    type: TagPropertyType.DATASOURCE,
                    typeId: TagPartType.DataSource
                },
                {
                    type: TagPropertyType.DATASOURCE,
                    typeId: TagPartType.DataSource,
                    options: null
                },
                {
                    type: TagPropertyType.DATASOURCE,
                    typeId: TagPartType.DataSource,
                    options: []
                }
            ]);
            assertResultsForAllTagProperties(tag, expectedResultNotSet);
        });

        it('handles set DataSourceTagProperties correctly', () => {
            const tag = mockEditableTag<DataSourceTagPartProperty>([
                {
                    type: TagPropertyType.DATASOURCE,
                    typeId: TagPartType.DataSource,
                    options: [
                        { id: 0, key: 'key0', value: 'value0' }
                    ]
                },
                {
                    type: TagPropertyType.DATASOURCE,
                    typeId: TagPartType.DataSource,
                    options: [
                        { id: 0, key: 'key0', value: 'value0' },
                        { id: 1, key: 'key1', value: 'value1' }
                    ]
                }
            ]);
            assertResultsForAllTagProperties(tag, expectedResultIsSet);
        });

        it('handles unset SelectTagProperties correctly for single select', () => {
            const tag = mockEditableTag<SelectTagPartProperty>([
                {
                    type: TagPropertyType.SELECT,
                    typeId: TagPartType.SelectSingle
                },
                {
                    type: TagPropertyType.SELECT,
                    typeId: TagPartType.SelectSingle,
                    options: [
                        {
                            id: 1,
                            key: '1',
                            value: '1'
                        },
                        {
                            id: 2,
                            key: '2',
                            value: '2'
                        }],
                    selectedOptions: null
                },
                {
                    type: TagPropertyType.SELECT,
                    typeId: TagPartType.SelectSingle,
                    options: [
                        {
                            id: 1,
                            key: '1',
                            value: '1'
                        },
                        {
                            id: 2,
                            key: '2',
                            value: '2'
                        }],
                    selectedOptions: []
                }
            ]);
            assertResultsForAllTagProperties(tag, expectedResultNotSet);
        });

        it('handles unset SelectTagProperties correctly for multiple select', () => {
            const tag = mockEditableTag<SelectTagPartProperty>([
                {
                    type: TagPropertyType.MULTISELECT,
                    typeId: TagPartType.SelectMultiple
                },
                {
                    type: TagPropertyType.MULTISELECT,
                    typeId: TagPartType.SelectMultiple,
                    options: [
                        {
                            id: 1,
                            key: '1',
                            value: '1'
                        },
                        {
                            id: 2,
                            key: '2',
                            value: '2'
                        }],
                    selectedOptions: null
                },
                {
                    type: TagPropertyType.MULTISELECT,
                    typeId: TagPartType.SelectMultiple,
                    options: [
                        {
                            id: 1,
                            key: '1',
                            value: '1'
                        },
                        {
                            id: 2,
                            key: '2',
                            value: '2'
                        }],
                    selectedOptions: []
                }
            ]);
            assertResultsForAllTagProperties(tag, expectedResultNotSet);
        });

        it('handles set SelectTagProperties correctly for single select', () => {
            const tag = mockEditableTag<SelectTagPartProperty>([
                {
                    type: TagPropertyType.SELECT,
                    typeId: TagPartType.SelectSingle,
                    options: [
                        {
                            id: 1,
                            key: '1',
                            value: '1'
                        },
                        {
                            id: 2,
                            key: '2',
                            value: '2'
                        }
                    ],
                    selectedOptions: [
                        {
                            id: 1,
                            key: '1',
                            value: '1'
                        }
                    ]
                }
            ]);
            assertResultsForAllTagProperties(tag, expectedResultIsSet);
        });

        it('handles set SelectTagProperties correctly for multiple select', () => {
            const tag = mockEditableTag<SelectTagPartProperty>([
                {
                    type: TagPropertyType.MULTISELECT,
                    typeId: TagPartType.SelectMultiple,
                    options: [
                        {
                            id: 1,
                            key: '1',
                            value: '1'
                        },
                        {
                            id: 2,
                            key: '2',
                            value: '2'
                        }
                    ],
                    selectedOptions: [
                        {
                            id: 1,
                            key: '1',
                            value: '1'
                        },
                        {
                            id: 2,
                            key: '2',
                            value: '2'
                        }
                    ]
                }
            ]);
            assertResultsForAllTagProperties(tag, expectedResultIsSet);
        });

        it('handles unset FileTagProperties correctly', () => {
            const tag = mockEditableTag<FileTagPartProperty>([
                {
                    type: TagPropertyType.FILE,
                    typeId: TagPartType.UrlFile
                },
                {
                    type: TagPropertyType.FILE,
                    typeId: TagPartType.UrlFile,
                    fileId: null
                },
                {
                    type: TagPropertyType.FILE,
                    typeId: TagPartType.UrlFile,
                    fileId: 0
                },
                {
                    type: TagPropertyType.FILE,
                    typeId: TagPartType.UrlFile,
                    nodeId: 1234
                }
            ]);
            assertResultsForAllTagProperties(tag, expectedResultNotSet);
        });

        it('handles set FileTagProperties correctly', () => {
            const tag = mockEditableTag<FileTagPartProperty>([
                {
                    type: TagPropertyType.FILE,
                    typeId: TagPartType.UrlFile,
                    fileId: 1234
                },
                {
                    type: TagPropertyType.FILE,
                    typeId: TagPartType.UrlFile,
                    fileId: 1234,
                    nodeId: 4711
                }
            ]);
            assertResultsForAllTagProperties(tag, expectedResultIsSet);
        });

        it('handles unset FolderTagProperties correctly', () => {
            const tag = mockEditableTag<FolderTagPartProperty>([
                {
                    type: TagPropertyType.FOLDER,
                    typeId: TagPartType.UrlFolder
                },
                {
                    type: TagPropertyType.FOLDER,
                    typeId: TagPartType.UrlFolder,
                    folderId: null
                },
                {
                    type: TagPropertyType.FOLDER,
                    typeId: TagPartType.UrlFolder,
                    folderId: 0
                },
                {
                    type: TagPropertyType.FOLDER,
                    typeId: TagPartType.UrlFolder,
                    nodeId: 1234
                }
            ]);
            assertResultsForAllTagProperties(tag, expectedResultNotSet);
        });

        it('handles set FolderTagProperties correctly', () => {
            const tag = mockEditableTag<FolderTagPartProperty>([
                {
                    type: TagPropertyType.FOLDER,
                    typeId: TagPartType.UrlFolder,
                    folderId: 1234
                },
                {
                    type: TagPropertyType.FOLDER,
                    typeId: TagPartType.UrlFolder,
                    folderId: 1234,
                    nodeId: 4711
                }
            ]);
            assertResultsForAllTagProperties(tag, expectedResultIsSet);
        });

        it('handles unset NodeSelectorTagProperties correctly', () => {
            const tag = mockEditableTag<NodeTagPartProperty>([
                {
                    type: TagPropertyType.NODE,
                    typeId: TagPartType.Node,
                    nodeId: null
                },
                {
                    type: TagPropertyType.NODE,
                    typeId: TagPartType.Node,
                    nodeId: 0
                }
            ]);
            assertResultsForAllTagProperties(tag, expectedResultNotSet);
        });

        it('handles set NodeSelectorTagProperties correctly', () => {
            const tag = mockEditableTag<NodeTagPartProperty>([
                {
                    type: TagPropertyType.NODE,
                    typeId: TagPartType.Node,
                    nodeId: 1
                },
                {
                    type: TagPropertyType.NODE,
                    typeId: TagPartType.Node,
                    nodeId: 2
                },
                {
                    type: TagPropertyType.NODE,
                    typeId: TagPartType.Node,
                    nodeId: 3
                }
            ]);
            assertResultsForAllTagProperties(tag, expectedResultIsSet);
        });

        it('handles unset ImageTagProperties correctly', () => {
            const tag = mockEditableTag<ImageTagPartProperty>([
                {
                    type: TagPropertyType.IMAGE,
                    typeId: TagPartType.UrlImage
                },
                {
                    type: TagPropertyType.IMAGE,
                    typeId: TagPartType.UrlImage,
                    imageId: null
                },
                {
                    type: TagPropertyType.IMAGE,
                    typeId: TagPartType.UrlImage,
                    imageId: 0
                },
                {
                    type: TagPropertyType.IMAGE,
                    typeId: TagPartType.UrlImage,
                    nodeId: 4711
                }
            ]);
            assertResultsForAllTagProperties(tag, expectedResultNotSet);
        });

        it('handles set ImageTagProperties correctly', () => {
            const tag = mockEditableTag<ImageTagPartProperty>([
                {
                    type: TagPropertyType.IMAGE,
                    typeId: TagPartType.UrlImage,
                    imageId: 1234
                },
                {
                    type: TagPropertyType.IMAGE,
                    typeId: TagPartType.UrlImage,
                    imageId: 1234,
                    nodeId: 4711
                }
            ]);
            assertResultsForAllTagProperties(tag, expectedResultIsSet);
        });

        it('handles unset ListTagProperties correctly', () => {
            const tag = mockEditableTag<ListTagPartProperty>([
                {
                    type: TagPropertyType.LIST,
                    typeId: TagPartType.List
                },
                {
                    type: TagPropertyType.LIST,
                    typeId: TagPartType.List,
                    booleanValue: true
                },
                {
                    type: TagPropertyType.LIST,
                    typeId: TagPartType.List,
                    booleanValue: true,
                    stringValues: null
                },
                {
                    type: TagPropertyType.LIST,
                    typeId: TagPartType.List,
                    booleanValue: true,
                    stringValues: []
                }
            ]);
            assertResultsForAllTagProperties(tag, expectedResultNotSet);
        });

        it('handles set ListTagProperties correctly', () => {
            const tag = mockEditableTag<ListTagPartProperty>([
                {
                    type: TagPropertyType.LIST,
                    typeId: TagPartType.List,
                    booleanValue: false,
                    stringValues: [ 'item0' ]
                },
                {
                    type: TagPropertyType.LIST,
                    typeId: TagPartType.List,
                    booleanValue: true,
                    stringValues: [ 'item0', 'item1' ]
                }
            ]);
            assertResultsForAllTagProperties(tag, expectedResultIsSet);
        });

        it('handles unset OrderedUnorderedListTagProperties correctly', () => {
            const tag = mockEditableTag<OrderedUnorderedListTagPartProperty>([
                {
                    type: TagPropertyType.ORDEREDLIST,
                    typeId: TagPartType.ListOrdered
                },
                {
                    type: TagPropertyType.ORDEREDLIST,
                    typeId: TagPartType.ListOrdered,
                    stringValues: null
                },
                {
                    type: TagPropertyType.ORDEREDLIST,
                    typeId: TagPartType.ListOrdered,
                    stringValues: []
                },
                {
                    type: TagPropertyType.UNORDEREDLIST,
                    typeId: TagPartType.ListUnordered
                },
                {
                    type: TagPropertyType.UNORDEREDLIST,
                    typeId: TagPartType.ListUnordered,
                    stringValues: null
                },
                {
                    type: TagPropertyType.UNORDEREDLIST,
                    typeId: TagPartType.ListUnordered,
                    stringValues: []
                }
            ]);
            assertResultsForAllTagProperties(tag, expectedResultNotSet);
        });

        it('handles set OrderedUnorderedListTagProperties correctly', () => {
            const tag = mockEditableTag<OrderedUnorderedListTagPartProperty>([
                {
                    type: TagPropertyType.ORDEREDLIST,
                    typeId: TagPartType.ListOrdered,
                    stringValues: [ 'item0' ]
                },
                {
                    type: TagPropertyType.UNORDEREDLIST,
                    typeId: TagPartType.ListUnordered,
                    stringValues: [ 'item0' ]
                }
            ]);
            assertResultsForAllTagProperties(tag, expectedResultIsSet);
        });

        it('handles unset PageTagTagProperties correctly', () => {
            const tag = mockEditableTag<PageTagTagPartProperty>([
                {
                    type: TagPropertyType.PAGETAG,
                    typeId: TagPartType.TagPage
                },
                {
                    type: TagPropertyType.PAGETAG,
                    typeId: TagPartType.TagPage,
                    pageId: null,
                    contentTagId: null
                },
                {
                    type: TagPropertyType.PAGETAG,
                    typeId: TagPartType.TagPage,
                    contentTagId: 4711
                },
                {
                    type: TagPropertyType.PAGETAG,
                    typeId: TagPartType.TagPage,
                    pageId: 1234
                },
                {
                    type: TagPropertyType.PAGETAG,
                    typeId: TagPartType.TagPage,
                    pageId: 0,
                    contentTagId: 0
                }
            ]);
            assertResultsForAllTagProperties(tag, expectedResultNotSet);
        });

        it('handles set PageTagTagProperties correctly', () => {
            const tag = mockEditableTag<PageTagTagPartProperty>([{
                type: TagPropertyType.PAGETAG,
                typeId: TagPartType.TagPage,
                pageId: 1234,
                contentTagId: 4711
            }]);
            assertResultsForAllTagProperties(tag, expectedResultIsSet);
        });

        it('handles unset TemplateTagTagProperties correctly', () => {
            const tag = mockEditableTag<TemplateTagTagPartProperty>([
                {
                    type: TagPropertyType.TEMPLATETAG,
                    typeId: TagPartType.TagTemplate
                },
                {
                    type: TagPropertyType.TEMPLATETAG,
                    typeId: TagPartType.TagTemplate,
                    templateId: null,
                    templateTagId: null
                },
                {
                    type: TagPropertyType.TEMPLATETAG,
                    typeId: TagPartType.TagTemplate,
                    templateTagId: 4711
                },
                {
                    type: TagPropertyType.TEMPLATETAG,
                    typeId: TagPartType.TagTemplate,
                    templateId: 1234
                },
                {
                    type: TagPropertyType.TEMPLATETAG,
                    typeId: TagPartType.TagTemplate,
                    templateId: 0,
                    templateTagId: 0
                }
            ]);
            assertResultsForAllTagProperties(tag, expectedResultNotSet);
        });

        it('handles set TemplateTagTagProperties correctly', () => {
            const tag = mockEditableTag<TemplateTagTagPartProperty>([{
                type: TagPropertyType.TEMPLATETAG,
                typeId: TagPartType.TagTemplate,
                templateId: 1234,
                templateTagId: 4711
            }]);
            assertResultsForAllTagProperties(tag, expectedResultIsSet);
        });

    });

});
