import { TagPropertyValidator, ValidationResult } from '@gentics/cms-integration-api-models';
import { RegexValidationInfo, StringTagPartProperty, TagPart, TagPartType } from '@gentics/cms-models';

interface RegExpMap {
    [expression: string]: RegExp;
}

/**
 * Validator for tag properties of type TagPropertyType.STRING.
 */
export class StringTagPropertyValidator implements TagPropertyValidator<StringTagPartProperty> {

    private regExpCache: RegExpMap = { };

    validate(editedProperty: StringTagPartProperty, tagPart: TagPart): ValidationResult {
        if (editedProperty.stringValue) {
            if (tagPart.typeId === TagPartType.Json) {
                return this.validateJson(editedProperty.stringValue);
            } else if (tagPart.regex) {
                return this.validateString(editedProperty.stringValue, tagPart.regex);
            } else {
                return {
                    isSet: true,
                    success: true,
                };
            }
        } else {
            return {
                isSet: false,
                success: !tagPart.mandatory,
            };
        }
    }

    private validateJson(value: string): ValidationResult {
        let jsonError = null;
        let parsed: object;
        try {
            parsed = JSON.parse(value);
        } catch (error) {
            jsonError = error;
        }
        const result: ValidationResult = {
            isSet: true,
            success: parsed != null && typeof parsed === 'object',
            errorMessage: jsonError
        };
        return result;
    }

    private validateString(value: string, regexInfo: RegexValidationInfo): ValidationResult {
        const regExp = this.getRegExp(regexInfo);
        const result: ValidationResult = {
            isSet: true,
            success: regExp.test(value),
        };
        if (!result.success) {
            result.errorMessage = regexInfo.description;
        }
        return result;
    }

    private getRegExp(regexInfo: RegexValidationInfo): RegExp {
        let regExp = this.regExpCache[regexInfo.expression];
        if (!regExp) {
            regExp = new RegExp(regexInfo.expression);
            this.regExpCache[regexInfo.expression] = regExp;
        }
        return regExp;
    }

}
