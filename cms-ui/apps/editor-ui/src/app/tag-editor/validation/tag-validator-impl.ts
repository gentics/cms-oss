import {
    findTagPart,
    MultiValidationResult,
    TagEditorError,
    TagPart,
    TagPartProperty,
    TagPropertyMap,
    TagPropertyType,
    TagPropertyValidator,
    TagType,
    TagValidationResult,
    TagValidator,
    ValidationResult,
} from '@gentics/cms-models';
import { cloneDeep } from 'lodash-es';
import { GenericTagPropertyValidator } from './generic-tag-property-validator/generic-tag-property-validator';
import { OverviewTagPropertyValidator } from './overview-tag-property-validator/overview-tag-property-validator';
import { PageTagPropertyValidator } from './page-tag-property-validator/page-tag-property-validator';
import { StringTagPropertyValidator } from './string-tag-property-validator/string-tag-property-validator';

export interface TagPropertyValidatorMap {
    [key: string]: TagPropertyValidator<TagPartProperty>;
}

/** Used for validating the properties of an EditableTag. */
export class TagValidatorImpl implements TagValidator {

    protected propertyValidators: TagPropertyValidatorMap = {};
    protected genericValidator = new GenericTagPropertyValidator();

    /** The editable (and non hidden in editor) parts of the tag. */
    editableTagParts: TagPart[];

    constructor(private tagType: TagType) {
        this.propertyValidators[TagPropertyType.STRING] = new StringTagPropertyValidator();
        this.propertyValidators[TagPropertyType.RICHTEXT] = new StringTagPropertyValidator();
        this.propertyValidators[TagPropertyType.PAGE] = new PageTagPropertyValidator();
        this.propertyValidators[TagPropertyType.OVERVIEW] = new OverviewTagPropertyValidator();
        this.editableTagParts = tagType.parts
            .filter(tagPart => tagPart.editable && !tagPart.hideInEditor);
    }

    validateTagProperty(property: TagPartProperty): ValidationResult {
        const validator = this.propertyValidators[property.type] || this.genericValidator;
        return validator.validate(property, findTagPart(property, this.tagType));
    }

    validateAllTagProperties(tagProperties: TagPropertyMap): TagValidationResult {
        const results: MultiValidationResult = {};
        let allPropertiesValid = true;

        for (const tagPart of this.editableTagParts) {
            const tagProperty = tagProperties[tagPart.keyword];
            if (!tagProperty) {
                throw new TagEditorError(`No property found for TagPart ${tagPart.keyword}. validateAllTagProperties() must be called with all TagProperties of a tag.`);
            }
            const validationResult = this.validateTagProperty(tagProperty);
            results[tagPart.keyword] = validationResult;
            if (!validationResult.success) {
                allPropertiesValid = false;
            }
        }

        return { allPropertiesValid, results };
    }

    clone(): TagValidator {
        return new TagValidatorImpl(cloneDeep(this.tagType));
    }
}
