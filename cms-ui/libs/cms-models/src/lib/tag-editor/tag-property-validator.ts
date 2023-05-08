import {TagPartProperty, TagPart} from '../models';

/** The result of validating a TagProperty against the constraints defined in the corresponding TagPart. */
export interface ValidationResult {

    /** True if the TagProperty has a value. */
    isSet: boolean;

    /**
     * True if the value of the TagProperty is valid.
     *
     * A TagProperty is valid if:
     * - it has a value set and that value passes validation or
     * - it has no value set and it is a non-mandatory property.
     */
    success: boolean;

    /**
     * The i18n string of the error message in case the validation was not successful.
     *
     * This message must be translated using the TagEditorContext.translator if operating in a
     * custom TagEditor or custom TagPropertyEditor. Inside the GCMS UI the normal translateService or
     * i18n pipe can be used.
     */
    errorMessage?: string;
}

/**
 * A map of possibly multiple validation results.
 *
 * multiValidationResult['propA'] is the ValidationResult for the TagProperty with the key 'propA'.
 */
export interface MultiValidationResult {
    [key: string]: ValidationResult;
}

/** Validates a single TagProperty against the constraints defined in the corresponding TagPart. */
export interface TagPropertyValidator<T extends TagPartProperty> {

    /** Validates the specified TagProperty against the constraints defined in the TagPart. */
    validate(editedProperty: T, tagPart: TagPart): ValidationResult;

}
