/**
 * Which condition has to be met in order to display this setting to the editor
 */
export type FormCondition
    = | FormConditionOr
      | FormConditionAnd
      | FormConditionNot
      | FormConditionEquals
      | FormConditionMatches
      | FormConditionContains
      | FormConditionIncludedIn
      | FormConditionBiggerThan
      | FormConditionLowerThan
      | FormConditionEmpty
      | FormConditionVisible
      | FormConditionDisabled;

/**
 * The source from where the value should be pulled from
 */
export type FormConditionSource
    = | FormConditionSourceSetting
      | FormConditionSourceSchema
      | FormConditionSourceControl
      ;

export type FormCompareValue = string | number | boolean | null;

/**
 * Is met when one or more of the provided conditions are met
 */
export interface FormConditionOr {
    or: FormCondition[];
}

/**
 * Is met when all provided conditions are met
 */
export interface FormConditionAnd {
    and: FormCondition[];
}

/**
 * Inverts the provided condition result
 */
export interface FormConditionNot {
    not: FormCondition;
}

/**
 * Source which targets another setting which has been defined.
 * Used by `FormSettingConfiguration`.
 */
export interface FormConditionSourceSetting {
    /** ID of the setting from this element which is to be checked against */
    setting: string;
}

/**
 * Source which targets one of the defined form schema fields or the current schema property.
 * Used by `FormSettingConfiguration`.
 */
export interface FormConditionSourceSchema {
    /**
     * A JSON path to the schema property you want to check for.
     */
    schema: string;
}

/**
 * Source which targets a control within the current form.
 * Used by `FormElement`.
 */
export interface FormConditionSourceControl {
    /** The ID of the control within `schema.properties`. */
    control: string;
}

export interface BaseFormCondition {
    /**
     * The source of the value that is used for this condition.
     */
    source: FormConditionSource;
}

/**
 * Met when the control value strictly equals the specified value
 */
export interface FormConditionEquals extends BaseFormCondition {
    equals: FormCompareValue;
}

/**
 * Met when the specified RegExp matches the control value
 */
export interface FormConditionMatches extends BaseFormCondition {
    /**
     * RegExp to match against the control value
     */
    matches: string;
}

/**
 * Met when one of the controls (array/list) values equals the specified value
 */
export interface FormConditionContains extends BaseFormCondition {
    contains: FormCompareValue;
}

/**
 * Met when the controls value equals one of the specified values
 */
export interface FormConditionIncludedIn extends BaseFormCondition {
    includedIn: [FormCompareValue, ...FormCompareValue[]];
}

/**
 * Met when the control value is (excluding) bigger than the specified value
 */
export interface FormConditionBiggerThan extends BaseFormCondition {
    biggerThan: number;
}

/**
 * Met when the control value is (excluding) lower than the specified value
 */
export interface FormConditionLowerThan extends BaseFormCondition {
    lowerThan: number;
}

/**
 * Met when the control value is or isn't empty/null
 */
export interface FormConditionEmpty extends BaseFormCondition {
    empty: boolean;
}

/**
 * Met when the control is or isn't visible
 */
export interface FormConditionVisible extends BaseFormCondition {
    visible: boolean;
}

/**
 * Met when the control is or isn't disabled
 */
export interface FormConditionDisabled extends BaseFormCondition {
    disabled: boolean;
}
