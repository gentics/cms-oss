import {
    EditableTag,
    ListType,
    OrderBy,
    OrderDirection,
    OverviewTagPartProperty,
    SelectType,
    TagPart,
    TagPartType,
    TagPropertyType,
    ValidationResult
} from '@gentics/cms-models';
import { mockEditableTag } from '../../../../testing/test-tag-editor-data.mock';
import { OverviewTagPropertyValidator } from './overview-tag-property-validator';

/**
 * Creates default OverviewSettings on each of the specified tagParts.
 */
export function createDefaultOverviewSettings(tagParts: TagPart[]): void {
    tagParts.forEach(part => {
        part.overviewSettings = {
            hideSortOptions: false,
            listTypes: [],
            selectTypes: [],
            stickyChannel: false
        };
    });
}

describe('OverviewTagPropertyValidator', () => {

    let tagValidator: OverviewTagPropertyValidator;

    beforeEach(() => {
        tagValidator = new OverviewTagPropertyValidator();
    });

    function assertResultsForAllTagProperties(tag: EditableTag, expectedResult: ValidationResult): void {
        tag.tagType.parts.forEach(tagPart => {
            const tagProperty = tag.properties[tagPart.keyword];
            expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);
        });
    }

    it('validation of unset, non-mandatory property works', () => {
        const tag = mockEditableTag<OverviewTagPartProperty>([
            {
                type: TagPropertyType.OVERVIEW,
                typeId: TagPartType.Overview
            },
            {
                type: TagPropertyType.OVERVIEW,
                typeId: TagPartType.Overview,
                overview: {
                    listType: ListType.UNDEFINED,
                    selectType: SelectType.UNDEFINED,
                    maxItems: 10,
                    orderBy: OrderBy.UNDEFINED,
                    orderDirection: OrderDirection.UNDEFINED,
                    recursive: false,
                    source: ''
                }
            },
            {
                type: TagPropertyType.OVERVIEW,
                typeId: TagPartType.Overview,
                overview: {
                    listType: ListType.UNDEFINED,
                    selectType: SelectType.FOLDER,
                    maxItems: 10,
                    orderBy: OrderBy.ALPHABETICALLY,
                    orderDirection: OrderDirection.DESC,
                    recursive: false,
                    selectedItemIds: [ 1234, 4711 ],
                    source: ''
                }
            },
            {
                type: TagPropertyType.OVERVIEW,
                typeId: TagPartType.Overview,
                overview: {
                    listType: ListType.PAGE,
                    selectType: SelectType.UNDEFINED,
                    maxItems: 10,
                    orderBy: OrderBy.ALPHABETICALLY,
                    orderDirection: OrderDirection.DESC,
                    recursive: false,
                    selectedItemIds: [ 1234, 4711 ],
                    source: ''
                }
            },
            {
                // stickyChannel = true, but no selectedNodeItemIds set
                type: TagPropertyType.OVERVIEW,
                typeId: TagPartType.Overview,
                overview: {
                    listType: ListType.PAGE,
                    selectType: SelectType.MANUAL,
                    maxItems: 10,
                    orderBy: OrderBy.ALPHABETICALLY,
                    orderDirection: OrderDirection.DESC,
                    recursive: false,
                    selectedItemIds: [ 1234, 4711 ],
                    source: ''
                }
            },
            {
                // stickyChannel = true, but selectedNodeItemIds are empty
                type: TagPropertyType.OVERVIEW,
                typeId: TagPartType.Overview,
                overview: {
                    listType: ListType.PAGE,
                    selectType: SelectType.MANUAL,
                    maxItems: 10,
                    orderBy: OrderBy.ALPHABETICALLY,
                    orderDirection: OrderDirection.DESC,
                    recursive: false,
                    selectedItemIds: [ 1234, 4711 ],
                    selectedNodeItemIds: [],
                    source: ''
                }
            },
            {
                // stickyChannel = false, but no selectedItemIds set
                type: TagPropertyType.OVERVIEW,
                typeId: TagPartType.Overview,
                overview: {
                    listType: ListType.PAGE,
                    selectType: SelectType.FOLDER,
                    maxItems: 10,
                    orderBy: OrderBy.ALPHABETICALLY,
                    orderDirection: OrderDirection.DESC,
                    recursive: false,
                    selectedNodeItemIds: [ { nodeId: 1, objectId: 1234 }, { nodeId: 1, objectId: 4711 } ],
                    source: ''
                }
            },
            {
                // stickyChannel = false, but selectedItemIds are empty
                type: TagPropertyType.OVERVIEW,
                typeId: TagPartType.Overview,
                overview: {
                    listType: ListType.PAGE,
                    selectType: SelectType.FOLDER,
                    maxItems: 10,
                    orderBy: OrderBy.ALPHABETICALLY,
                    orderDirection: OrderDirection.DESC,
                    recursive: false,
                    selectedItemIds: [],
                    selectedNodeItemIds: [ { nodeId: 1, objectId: 1234 }, { nodeId: 1, objectId: 4711 } ],
                    source: ''
                }
            },
            {
                type: TagPropertyType.OVERVIEW,
                typeId: TagPartType.Overview,
                overview: {
                    listType: ListType.PAGE,
                    selectType: SelectType.MANUAL,
                    maxItems: 10,
                    orderBy: OrderBy.UNDEFINED,
                    orderDirection: OrderDirection.DESC,
                    recursive: false,
                    selectedItemIds: [ 1234, 4711 ],
                    source: ''
                }
            },
            {
                type: TagPropertyType.OVERVIEW,
                typeId: TagPartType.Overview,
                overview: {
                    listType: ListType.PAGE,
                    selectType: SelectType.FOLDER,
                    maxItems: 10,
                    orderBy: OrderBy.ALPHABETICALLY,
                    orderDirection: OrderDirection.UNDEFINED,
                    recursive: false,
                    selectedItemIds: [ 1234, 4711 ],
                    selectedNodeItemIds: [ { nodeId: 1, objectId: 1234 }, { nodeId: 1, objectId: 4711 } ],
                    source: ''
                }
            }
        ]);

        createDefaultOverviewSettings(tag.tagType.parts);
        tag.tagType.parts[4].overviewSettings.stickyChannel = true;
        tag.tagType.parts[5].overviewSettings.stickyChannel = true;

        const expectedResult: ValidationResult = {
            isSet: false,
            success: true
        };
        assertResultsForAllTagProperties(tag, expectedResult);
    });

    it('validation of unset, mandatory property works', () => {
        const tag = mockEditableTag<OverviewTagPartProperty>([
            {
                type: TagPropertyType.OVERVIEW,
                typeId: TagPartType.Overview,
                mandatory: true,
                overview: {
                    listType: ListType.PAGE,
                    selectType: SelectType.FOLDER,
                    maxItems: 10,
                    orderBy: OrderBy.ALPHABETICALLY,
                    orderDirection: OrderDirection.DESC,
                    recursive: false,
                    source: ''
                }
            }
        ]);
        createDefaultOverviewSettings(tag.tagType.parts);

        const expectedResult: ValidationResult = {
            isSet: false,
            success: false
        };
        assertResultsForAllTagProperties(tag, expectedResult);
    });

    it('validation of set, mandatory property works', () => {
        const tag = mockEditableTag<OverviewTagPartProperty>([
            {
                type: TagPropertyType.OVERVIEW,
                typeId: TagPartType.Overview,
                mandatory: true,
                overview: {
                    listType: ListType.PAGE,
                    selectType: SelectType.FOLDER,
                    maxItems: 10,
                    orderBy: OrderBy.ALPHABETICALLY,
                    orderDirection: OrderDirection.DESC,
                    recursive: false,
                    selectedItemIds: [ 1234, 4711 ],
                    source: ''
                }
            },
            {
                type: TagPropertyType.OVERVIEW,
                typeId: TagPartType.Overview,
                mandatory: true,
                overview: {
                    listType: ListType.PAGE,
                    selectType: SelectType.FOLDER,
                    maxItems: 10,
                    orderBy: OrderBy.ALPHABETICALLY,
                    orderDirection: OrderDirection.DESC,
                    recursive: false,
                    selectedNodeItemIds: [ { nodeId: 1, objectId: 1234 }, { nodeId: 1, objectId: 4711 } ],
                    source: ''
                }
            },
            {
                type: TagPropertyType.OVERVIEW,
                typeId: TagPartType.Overview,
                mandatory: true,
                overview: {
                    listType: ListType.PAGE,
                    selectType: SelectType.AUTO,
                    maxItems: 10,
                    orderBy: OrderBy.ALPHABETICALLY,
                    orderDirection: OrderDirection.DESC,
                    recursive: false,
                    source: ''
                }
            },
            {
                type: TagPropertyType.OVERVIEW,
                typeId: TagPartType.Overview,
                mandatory: true,
                overview: {
                    listType: ListType.PAGE,
                    selectType: SelectType.AUTO,
                    maxItems: 10,
                    orderBy: OrderBy.UNDEFINED,
                    orderDirection: OrderDirection.UNDEFINED,
                    recursive: false,
                    source: ''
                }
            }
        ]);
        createDefaultOverviewSettings(tag.tagType.parts);
        tag.tagType.parts[1].overviewSettings.stickyChannel = true;
        tag.tagType.parts[3].overviewSettings.hideSortOptions = true;

        const expectedResult: ValidationResult = {
            isSet: true,
            success: true
        };
        assertResultsForAllTagProperties(tag, expectedResult);
    });

});
