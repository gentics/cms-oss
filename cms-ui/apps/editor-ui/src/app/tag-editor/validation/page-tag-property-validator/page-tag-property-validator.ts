import { PageTagPartProperty, TagPart, TagPartProperty, TagPropertyValidator, ValidationResult } from '@gentics/cms-models';

/**
 * Validator for tag properties of type TagPropertyType.PAGE.
 */
export class PageTagPropertyValidator implements TagPropertyValidator<PageTagPartProperty> {

    validate(editedProperty: TagPartProperty, tagPart: TagPart): ValidationResult {
        const pageProperty = editedProperty as PageTagPartProperty;
        const isSet = !!pageProperty.pageId || !!pageProperty.stringValue;
        return {
            isSet: isSet,
            success: isSet || !tagPart.mandatory
        };
    }

}
