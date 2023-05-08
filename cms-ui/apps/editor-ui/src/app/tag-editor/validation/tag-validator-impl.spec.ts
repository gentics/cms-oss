import {
    EditableTag,
    ListType,
    OrderBy,
    OrderDirection,
    OverviewTagPartProperty,
    PageTagPartProperty,
    SelectTagPartProperty,
    SelectType,
    StringTagPartProperty,
    TagPart,
    TagPartProperty,
    TagPartType,
    TagPropertyType,
    TagPropertyValidator,
    TagType,
    TagValidationResult,
    TagValidator,
    ValidationResult
} from '@gentics/cms-models';
import { getExampleEditableTag, getExampleValidationFailed, getExampleValidationSuccess, mockEditableTag } from '../../../testing/test-tag-editor-data.mock';
import { GenericTagPropertyValidator } from './generic-tag-property-validator/generic-tag-property-validator';
import { createDefaultOverviewSettings } from './overview-tag-property-validator/overview-tag-property-validator.spec';
import { StringTagPropertyValidator } from './string-tag-property-validator/string-tag-property-validator';
import { TagValidatorImpl } from './tag-validator-impl';

interface TagPropertyValidatorMap {
    [key: string]: TagPropertyValidator<TagPartProperty>;
}

describe('TagValidatorImpl', () => {

    let tagValidator: TagValidator;
    let propertyValidators: TagPropertyValidatorMap;
    let genericValidator: GenericTagPropertyValidator;

    const setUpTagValidator = (tag: EditableTag) => {
        const validator = new TagValidatorImplForTesting(tag.tagType);
        tagValidator = validator;
        propertyValidators = validator.getPropertyValidators();
        genericValidator = validator.getGenericValidator();
    };

    it('initializes correctly', () => {
        const tag = getExampleEditableTag();
        setUpTagValidator(tag);
        expect(propertyValidators[TagPropertyType.STRING] instanceof StringTagPropertyValidator).toBeTruthy();
        expect(genericValidator instanceof GenericTagPropertyValidator).toBeTruthy();
    });

    it('calls StringTagPropertyValidator for TagPropertyType.STRING', () => {
        const tag = getExampleEditableTag();
        const tagPart = tag.tagType.parts[0];
        tagPart.type = TagPropertyType.STRING;
        tagPart.typeId = TagPartType.Text;
        setUpTagValidator(tag);

        const stringValidator = propertyValidators[TagPropertyType.STRING];
        const origValidate = stringValidator.validate.bind(stringValidator);
        let expectedResult: ValidationResult;
        const stringValidatorSpy = spyOn(stringValidator, 'validate').and.callFake((property: TagPartProperty, part: TagPart) => {
            expectedResult = origValidate(property, part);
            return expectedResult;
        });
        const genericValidatorSpy = spyOn(genericValidator, 'validate').and.callThrough();

        const actualResult = tagValidator.validateTagProperty(tag.properties[tagPart.keyword]);
        expect(stringValidatorSpy).toHaveBeenCalledWith(tag.properties[tagPart.keyword], tagPart);
        expect(actualResult).toBe(expectedResult);
        expect(genericValidatorSpy).not.toHaveBeenCalled();
    });

    it('calls PageTagPropertyValidator for TagPropertyType.PAGE', () => {
        const tag = mockEditableTag<PageTagPartProperty>([
            {
                type: TagPropertyType.PAGE,
                typeId: TagPartType.UrlPage,
                pageId: 1234
            }
        ]);
        const tagPart = tag.tagType.parts[0];
        setUpTagValidator(tag);

        const pageValidator = propertyValidators[TagPropertyType.PAGE];
        const origValidate = pageValidator.validate.bind(pageValidator);
        let expectedResult: ValidationResult;
        const pageValidatorSpy = spyOn(pageValidator, 'validate').and.callFake((property: TagPartProperty, part: TagPart) => {
            expectedResult = origValidate(property, part);
            return expectedResult;
        });
        const genericValidatorSpy = spyOn(genericValidator, 'validate').and.callThrough();

        const actualResult = tagValidator.validateTagProperty(tag.properties[tagPart.keyword]);
        expect(pageValidatorSpy).toHaveBeenCalledWith(tag.properties[tagPart.keyword], tagPart);
        expect(actualResult).toBe(expectedResult);
        expect(genericValidatorSpy).not.toHaveBeenCalled();
    });

    it('calls OverviewTagPropertyValidator for TagPropertyType.OVERVIEW', () => {
        const tag = mockEditableTag<OverviewTagPartProperty>([
            {
                type: TagPropertyType.OVERVIEW,
                typeId: TagPartType.Overview,
                overview: {
                    listType: ListType.PAGE,
                    selectType: SelectType.FOLDER,
                    maxItems: 10,
                    orderBy: OrderBy.ALPHABETICALLY,
                    orderDirection: OrderDirection.DESC,
                    recursive: false,
                    selectedItemIds: null,
                    selectedNodeItemIds: [ { nodeId: 1, objectId: 1234 }, { nodeId: 1, objectId: 4711 } ],
                    source: ''
                }
            }
        ]);
        createDefaultOverviewSettings(tag.tagType.parts);
        const tagPart = tag.tagType.parts[0];
        setUpTagValidator(tag);

        const overviewValidator = propertyValidators[TagPropertyType.OVERVIEW];
        const origValidate = overviewValidator.validate.bind(overviewValidator);
        let expectedResult: ValidationResult;
        const overviewValidatorSpy = spyOn(overviewValidator, 'validate').and.callFake((property: TagPartProperty, part: TagPart) => {
            expectedResult = origValidate(property, part);
            return expectedResult;
        });
        const genericValidatorSpy = spyOn(genericValidator, 'validate').and.callThrough();

        const actualResult = tagValidator.validateTagProperty(tag.properties[tagPart.keyword]);
        expect(overviewValidatorSpy).toHaveBeenCalledWith(tag.properties[tagPart.keyword], tagPart);
        expect(actualResult).toBe(expectedResult);
        expect(genericValidatorSpy).not.toHaveBeenCalled();
    });

    it('calls the GenericValidator for other TagPropertyTypes', () => {
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
        const tagPart = tag.tagType.parts[0];
        setUpTagValidator(tag);

        const origValidate = genericValidator.validate.bind(genericValidator);
        let expectedResult: ValidationResult;
        const genericValidatorSpy = spyOn(genericValidator, 'validate').and.callFake((property: TagPartProperty, part: TagPart) => {
            expectedResult = origValidate(property, part);
            return expectedResult;
        });
        const stringValidator = propertyValidators[TagPropertyType.STRING];
        const stringValidatorSpy = spyOn(stringValidator, 'validate').and.callThrough();

        const actualResult = tagValidator.validateTagProperty(tag.properties[tagPart.keyword]);
        expect(genericValidatorSpy).toHaveBeenCalledWith(tag.properties[tagPart.keyword], tagPart);
        expect(actualResult).toBe(expectedResult);
        expect(stringValidatorSpy).not.toHaveBeenCalled();
    });

    it('validateAllTagProperties() calls validateTagProperty() for each editable TagProperty and returns the correct result for all properties valid', () => {
        const tag = mockEditableTag<StringTagPartProperty>([
            {
                stringValue: 'test',
                type: TagPropertyType.STRING,
                typeId: TagPartType.Text
            },
            {
                mandatory: true,
                stringValue: 'test',
                type: TagPropertyType.STRING,
                typeId: TagPartType.Text
            },
            {
                editable: false,
                stringValue: 'test',
                type: TagPropertyType.STRING,
                typeId: TagPartType.Text
            },
            {
                hideInEditor: true,
                stringValue: 'test',
                type: TagPropertyType.STRING,
                typeId: TagPartType.Text
            },
            {
                hidden: true,
                stringValue: 'test',
                type: TagPropertyType.STRING,
                typeId: TagPartType.Text
            },
            {
                stringValue: 'test',
                type: TagPropertyType.STRING,
                typeId: TagPartType.Text
            }
        ]);
        setUpTagValidator(tag);

        const validatePropSpy = spyOn(tagValidator, 'validateTagProperty').and.callFake(() => getExampleValidationSuccess());
        const expectedResult: TagValidationResult = {
            allPropertiesValid: true,
            results: {
                'property0': getExampleValidationSuccess(),
                'property1': getExampleValidationSuccess(),
                'property4': getExampleValidationSuccess(),
                'property5': getExampleValidationSuccess()
            }
        }

        const result = tagValidator.validateAllTagProperties(tag.properties);
        expect(result).toEqual(expectedResult);
        expect(validatePropSpy).toHaveBeenCalledTimes(4);
        expect(validatePropSpy.calls.argsFor(0)[0]).toBe(tag.properties['property0']);
        expect(validatePropSpy.calls.argsFor(1)[0]).toBe(tag.properties['property1']);
        expect(validatePropSpy.calls.argsFor(2)[0]).toBe(tag.properties['property4']);
        expect(validatePropSpy.calls.argsFor(3)[0]).toBe(tag.properties['property5']);
    });

    it('validateAllTagProperties() returns the correct result if not all properties are valid', () => {
        const tag = mockEditableTag<StringTagPartProperty>([
            {
                stringValue: 'test',
                type: TagPropertyType.STRING,
                typeId: TagPartType.Text
            },
            {
                mandatory: true,
                stringValue: '',
                type: TagPropertyType.STRING,
                typeId: TagPartType.Text
            }
        ]);
        setUpTagValidator(tag);

        spyOn(tagValidator, 'validateTagProperty').and.returnValues(
            getExampleValidationSuccess(),
            getExampleValidationFailed()
        );
        const expectedResult: TagValidationResult = {
            allPropertiesValid: false,
            results: {
                'property0': getExampleValidationSuccess(),
                'property1': getExampleValidationFailed(),
            }
        }

        const result = tagValidator.validateAllTagProperties(tag.properties);
        expect(result).toEqual(expectedResult);
    });

    it('clone works', () => {
        const tag = getExampleEditableTag();
        setUpTagValidator(tag);

        const clone = tagValidator.clone();
        expect(clone instanceof TagValidatorImpl).toBeTruthy();
        expect(clone).not.toBe(tagValidator);
    });

});

/**
 * This class is needed to provide access to the property validators of TagValidatorImpl.
 */
class TagValidatorImplForTesting extends TagValidatorImpl {

    constructor(tagType: TagType) {
        super(tagType);
    }

    getPropertyValidators(): TagPropertyValidatorMap {
        return this.propertyValidators;
    }

    getGenericValidator(): GenericTagPropertyValidator {
        return this.genericValidator;
    }

}
