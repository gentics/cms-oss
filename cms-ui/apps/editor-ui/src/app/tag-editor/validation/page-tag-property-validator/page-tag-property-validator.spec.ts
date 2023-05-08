import { EditableTag, PageTagPartProperty, TagPartType, TagPropertyType, ValidationResult } from '@gentics/cms-models';
import { mockEditableTag } from '../../../../testing/test-tag-editor-data.mock';
import { PageTagPropertyValidator } from './page-tag-property-validator';

describe('PageTagPropertyValidator', () => {

    let tagValidator: PageTagPropertyValidator;

    beforeEach(() => {
        tagValidator = new PageTagPropertyValidator();
    });

    function assertResultsForAllTagProperties(tag: EditableTag, expectedResult: ValidationResult): void {
        tag.tagType.parts.forEach(tagPart => {
            const tagProperty = tag.properties[tagPart.keyword];
            expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);
        });
    }

    it('validation of unset, non-mandatory property works', () => {
        const tag = mockEditableTag<PageTagPartProperty>([
            {
                type: TagPropertyType.PAGE,
                typeId: TagPartType.UrlPage
            },
            {
                type: TagPropertyType.PAGE,
                typeId: TagPartType.UrlPage,
                pageId: null
            },
            {
                type: TagPropertyType.PAGE,
                typeId: TagPartType.UrlPage,
                pageId: 0
            },
            {
                type: TagPropertyType.PAGE,
                typeId: TagPartType.UrlPage,
                nodeId: 1234
            },
            {
                type: TagPropertyType.PAGE,
                typeId: TagPartType.UrlPage,
                stringValue: null
            },
            {
                type: TagPropertyType.PAGE,
                typeId: TagPartType.UrlPage,
                stringValue: ''
            }
        ]);

        const expectedResult: ValidationResult = {
            isSet: false,
            success: true
        };
        assertResultsForAllTagProperties(tag, expectedResult);
    });

    it('validation of unset, mandatory property works', () => {
        const tag = mockEditableTag<PageTagPartProperty>([
            {
                type: TagPropertyType.PAGE,
                typeId: TagPartType.UrlPage,
                mandatory: true
            },
            {
                type: TagPropertyType.PAGE,
                typeId: TagPartType.UrlPage,
                pageId: null,
                mandatory: true
            },
            {
                type: TagPropertyType.PAGE,
                typeId: TagPartType.UrlPage,
                pageId: 0,
                mandatory: true
            },
            {
                type: TagPropertyType.PAGE,
                typeId: TagPartType.UrlPage,
                nodeId: 1234,
                mandatory: true
            },
            {
                type: TagPropertyType.PAGE,
                typeId: TagPartType.UrlPage,
                stringValue: null,
                mandatory: true
            },
            {
                type: TagPropertyType.PAGE,
                typeId: TagPartType.UrlPage,
                stringValue: '',
                mandatory: true
            }
        ]);

        const expectedResult: ValidationResult = {
            isSet: false,
            success: false
        };
        assertResultsForAllTagProperties(tag, expectedResult);
    });

    it('validation of set, mandatory property works', () => {
        const tag = mockEditableTag<PageTagPartProperty>([
            {
                type: TagPropertyType.PAGE,
                typeId: TagPartType.UrlPage,
                pageId: 1234
            },
            {
                type: TagPropertyType.PAGE,
                typeId: TagPartType.UrlPage,
                stringValue: 'https://www.gentics.com'
            }
        ]);

        const expectedResult: ValidationResult = {
            isSet: true,
            success: true
        };
        assertResultsForAllTagProperties(tag, expectedResult);
    });

});
