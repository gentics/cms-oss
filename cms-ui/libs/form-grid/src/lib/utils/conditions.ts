import {
    FormCondition,
    FormConditionAnd,
    FormConditionBiggerThan,
    FormConditionContains,
    FormConditionEmpty,
    FormConditionEquals,
    FormConditionIncludedIn,
    FormConditionLowerThan,
    FormConditionMatches,
    FormConditionNot,
    FormConditionOr,
    FormConditionSource,
    FormConditionSourceSchema,
    FormConditionSourceSetting,
    FormConditionVisible,
    FormElement,
    FormElementConfiguration,
    FormSchemaProperty,
    FormSettingConfiguration,
} from '@gentics/cms-models';
import { getValueByPath } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';

export function isSettingVisible(
    setting: FormSettingConfiguration,
    config: FormElementConfiguration,
    element: FormElement,
    schema?: FormSchemaProperty | null,
): boolean {
    if (!setting.condition) {
        return true;
    }

    return formCondition(setting.condition, config, element, schema);
}

function formCondition(
    condition: FormCondition,
    config: FormElementConfiguration,
    element: FormElement,
    schema?: FormSchemaProperty | null,
): boolean {
    if ((condition as FormConditionAnd).and != null) {
        return conditionAnd(condition as FormConditionAnd, config, element, schema);
    } else if ((condition as FormConditionOr).or != null) {
        return conditionOr(condition as FormConditionOr, config, element, schema);
    } else if ((condition as FormConditionNot).not != null) {
        return conditionNot(condition as FormConditionNot, config, element, schema);
    } else if ((condition as FormConditionEquals).equals != null) {
        return conditionEquals(condition as FormConditionEquals, config, element, schema);
    } else if ((condition as FormConditionMatches).matches != null) {
        return conditionMatches(condition as FormConditionMatches, config, element, schema);
    } else if ((condition as FormConditionContains).contains != null) {
        return conditionContains(condition as FormConditionContains, config, element, schema);
    } else if ((condition as FormConditionIncludedIn).includedIn != null) {
        return conditionIncludedIn(condition as FormConditionIncludedIn, config, element, schema);
    } else if ((condition as FormConditionBiggerThan).biggerThan != null) {
        return conditionBiggerThan(condition as FormConditionBiggerThan, config, element, schema);
    } else if ((condition as FormConditionLowerThan).lowerThan != null) {
        return conditionLowerThan(condition as FormConditionLowerThan, config, element, schema);
    } else if ((condition as FormConditionEmpty).empty != null) {
        return conditionEmpty(condition as FormConditionEmpty, config, element, schema);
    } else if ((condition as FormConditionVisible).visible != null) {
        return conditionVisible(condition as FormConditionVisible, config, element, schema);
    }

    return false;
}

function conditionAnd(
    condition: FormConditionAnd,
    config: FormElementConfiguration,
    element: FormElement,
    schema?: FormSchemaProperty | null,
): boolean {
    return condition.and.every((condition) => {
        return formCondition(condition, config, element, schema);
    });
}

function conditionOr(
    condition: FormConditionOr,
    config: FormElementConfiguration,
    element: FormElement,
    schema?: FormSchemaProperty | null,
): boolean {
    return condition.or.some((condition) => {
        return formCondition(condition, config, element, schema);
    });
}

function conditionNot(
    condition: FormConditionNot,
    config: FormElementConfiguration,
    element: FormElement,
    schema?: FormSchemaProperty | null,
): boolean {
    return !formCondition(condition.not, config, element, schema);
}

function getSourceValue(
    source: FormConditionSource,
    config: FormElementConfiguration,
    element: FormElement,
    schema?: FormSchemaProperty | null,
): any {
    if ((source as FormConditionSourceSchema).schema) {
        return getValueByPath(schema, (source as FormConditionSourceSchema).schema);
    }

    if ((source as FormConditionSourceSetting).setting) {
        const setting = (config.settings || []).find((tmpSetting) => tmpSetting.id === (source as FormConditionSourceSetting).setting);
        if (!setting) {
            return null;
        }

        if (setting.backend) {
            return getValueByPath(schema, setting.propertyPath || ['formGridOptions', setting.id]);
        } else {
            return getValueByPath(element, setting.propertyPath || ['formGridOptions', setting.id]);
        }
    }

    return null;
}

function valueEquals(left: any, right: any): boolean {
    return (left == null && right == null)
      || (
          left != null
          && right != null
          && (
              isEqual(left, right)
              || isEqual(`${left}`, `${right}`)
          )
      );
}

function conditionEquals(
    condition: FormConditionEquals,
    config: FormElementConfiguration,
    element: FormElement,
    schema?: FormSchemaProperty | null,
): boolean {
    const source = getSourceValue(condition.source, config, element, schema);
    return valueEquals(source, condition.equals);
}

function conditionMatches(
    condition: FormConditionMatches,
    config: FormElementConfiguration,
    element: FormElement,
    schema?: FormSchemaProperty | null,
): boolean {
    const source = getSourceValue(condition.source, config, element, schema);
    try {
        const regex = new RegExp(condition.matches);
        return regex.test(`${source}`);
    } catch (err) {
        console.error('Error while parsing regex of condition', condition, err);
        return false;
    }
}

function conditionContains(
    condition: FormConditionContains,
    config: FormElementConfiguration,
    element: FormElement,
    schema?: FormSchemaProperty | null,
): boolean {
    let source = getSourceValue(condition.source, config, element, schema);
    if (source == null) {
        return false;
    }
    if (!Array.isArray(source)) {
        source = [source];
    }
    return (source as any[]).some((value) => valueEquals(value, condition.contains));
}

function conditionIncludedIn(
    condition: FormConditionIncludedIn,
    config: FormElementConfiguration,
    element: FormElement,
    schema?: FormSchemaProperty | null,
): boolean {
    const source = getSourceValue(condition.source, config, element, schema);
    return condition.includedIn.some((value) => valueEquals(value, source));
}

function conditionBiggerThan(
    condition: FormConditionBiggerThan,
    config: FormElementConfiguration,
    element: FormElement,
    schema?: FormSchemaProperty | null,
): boolean {
    const source = getSourceValue(condition.source, config, element, schema);
    return source > condition.biggerThan;
}

function conditionLowerThan(
    condition: FormConditionLowerThan,
    config: FormElementConfiguration,
    element: FormElement,
    schema?: FormSchemaProperty | null,
): boolean {
    const source = getSourceValue(condition.source, config, element, schema);
    return source < condition.lowerThan;
}

function conditionEmpty(
    condition: FormConditionEmpty,
    config: FormElementConfiguration,
    element: FormElement,
    schema?: FormSchemaProperty | null,
): boolean {
    const source = getSourceValue(condition.source, config, element, schema);
    const isEmpty = source == null
      || source === ''
      || (Array.isArray(source) && source.length === 0);
    return condition.empty === isEmpty;
}

function conditionVisible(
    condition: FormConditionVisible,
    config: FormElementConfiguration,
    element: FormElement,
    schema?: FormSchemaProperty | null,
): boolean {
    const settingIdToCheck = (condition.source as FormConditionSourceSetting).setting;
    if (settingIdToCheck == null || !settingIdToCheck) {
        console.warn('Visible condition needs a setting as source', condition);
        return false;
    }
    const setting = (config.settings || []).find((tmpSetting) => tmpSetting.id === settingIdToCheck);
    if (!setting) {
        console.warn('Visible condition checks for setting which does not exist', condition);
        return false;
    }

    return condition.visible === isSettingVisible(setting, config, element, schema);
}
