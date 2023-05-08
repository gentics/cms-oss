import { EditableTag, StringTagPartProperty, TagPart, ValidationResult } from '@gentics/cms-models';
import { getExampleEditableTag, getExampleNaturalNumberValidationInfo } from '../../../../testing/test-tag-editor-data.mock';
import { StringTagPropertyValidator } from './string-tag-property-validator';

describe('StringTagPropertyValidator', () => {

    let tagValidator: StringTagPropertyValidator;
    let tag: EditableTag;
    let tagPart: TagPart;
    let tagProperty: StringTagPartProperty;

    beforeEach(() => {
        tagValidator = new StringTagPropertyValidator();
        tag = getExampleEditableTag();
        tagPart = tag.tagType.parts[0];
        tagProperty = tag.properties[tagPart.keyword] as StringTagPartProperty;
    });

    it('validation of unset, non-mandatory property works', () => {
        tagPart.mandatory = false;
        const expectedResult: ValidationResult = {
            isSet: false,
            success: true
        };

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
        const expectedResult: ValidationResult = {
            isSet: false,
            success: false
        };

        tagProperty.stringValue = '';
        expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);

        tagProperty.stringValue = null;
        expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);

        tagProperty.stringValue = undefined;
        expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);

        delete tagProperty.stringValue;
        expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);
    });

    it('validation of set property without regex works', () => {
        tagProperty.stringValue = 'Value is set';
        const expectedResult: ValidationResult = {
            isSet: true,
            success: true
        };

        tagPart.mandatory = true;
        expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);

        tagPart.mandatory = false;
        expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);
    });

    it('validation of correctly set property with regex works', () => {
        tagPart.regex = getExampleNaturalNumberValidationInfo();
        tagProperty.stringValue = '100';
        const expectedResult: ValidationResult = {
            isSet: true,
            success: true
        };

        tagPart.mandatory = true;
        expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);

        tagPart.mandatory = false;
        expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);
    });

    it('validation of incorrectly set property with regex works', () => {
        tagPart.regex = getExampleNaturalNumberValidationInfo();
        tagProperty.stringValue = '100x';
        const expectedResult: ValidationResult = {
            isSet: true,
            success: false,
            errorMessage: tagPart.regex.description
        };

        tagPart.mandatory = true;
        expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);

        tagPart.mandatory = false;
        expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);
    });

    it('validation of unset property with regex works', () => {
        tagPart.regex = getExampleNaturalNumberValidationInfo();
        tagProperty.stringValue = '';
        const expectedResult: ValidationResult = {
            isSet: false,
            success: true
        };

        tagPart.mandatory = false;
        expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);

        tagPart.mandatory = true;
        expectedResult.success = false;
        expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);

        tagProperty.stringValue = null;
        expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);

        tagProperty.stringValue = undefined;
        expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);

        delete tagProperty.stringValue;
        expect(tagValidator.validate(tagProperty, tagPart)).toEqual(expectedResult);
    });

});
