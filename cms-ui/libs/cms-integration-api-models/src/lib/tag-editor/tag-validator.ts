import { TagPartProperty, TagPropertyMap } from '@gentics/cms-models';
import { MultiValidationResult, ValidationResult } from './tag-property-validator';

/**
 * Collects the results from validating all editable properties of a tag.
 *
 * A TagProperty is editable, if for its tagPart, we have `tagPart.editable` is `true` and `tagPart.hiddenInEditor` is `false`)
 */
export interface TagValidationResult {

    /**
     * `true` if for each editable `TagProperty` its `ValidationResult.success` is true.
     */
    allPropertiesValid: boolean;

    /** The validation results for all the tag's editable properties. */
    results: MultiValidationResult;

}

/** Interface used for validating the properties of an EditableTag. */
export interface TagValidator {

    /**
     * Validates the specified `TagProperty` with the `TagPropertyValidator` registered for it.
     * @returns The `ValidationResult` returned by the TagPropertyValidator.
     */
    validateTagProperty(property: TagPartProperty): ValidationResult;

    /**
     * Validates all TagProperties using the registered `TagPropertyValidator` for each.
     * @returns The `TagValidationResult` for all the tagProperties.
     */
    validateAllTagProperties(tagProperties: TagPropertyMap): TagValidationResult;

    /** Clones this TagValidator instance. */
    clone(): TagValidator;

}
