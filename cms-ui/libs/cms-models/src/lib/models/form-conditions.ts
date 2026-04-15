import { FormSchemaProperty } from './form';

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
      | FormConditionDisabled
      | FormConditionOr
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
export type FormConditionControlSource
    = | FormConditionSourceSetting
      | FormConditionSourceSchema;

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
 * Met when the control value strictly equals the specified value
 */
export interface FormConditionEquals {
    source: FormConditionControlSource;
    equals: FormCompareValue;
}

/**
 * Source which targets another setting which has been defined
 */
export interface FormConditionSourceSetting {
    /** ID of the setting from this element which is to be checked against */
    setting: string;
}

/**
 * Source which targets one of the defined form schema fields or the current schema property
 */
export interface FormConditionSourceSchema {
    /**
     * All values which are defined in 'schema.properties' of a form
     */
    schema: Omit<keyof FormSchemaProperty, 'properties'>;
}

/**
 * Met when the specified RegExp matches the control value
 */
export interface FormConditionMatches {
    source: FormConditionControlSource;
    /**
     * RegExp to match against the control value
     */
    matches: string;
}

/**
 * Met when one of the controls (array/list) values equals the specified value
 */
export interface FormConditionContains {
    source: FormConditionControlSource;
    contains: FormCompareValue;
}

/**
 * Met when the controls value equals one of the specified values
 */
export interface FormConditionIncludedIn {
    source: FormConditionControlSource;
    includedIn: [FormCompareValue, ...FormCompareValue[]];
}

/**
 * Met when the control value is (excluding) bigger than the specified value
 */
export interface FormConditionBiggerThan {
    source: FormConditionControlSource;
    biggerThan: number;
}

/**
 * Met when the control value is (excluding) lower than the specified value
 */
export interface FormConditionLowerThan {
    source: FormConditionControlSource;
    lowerThan: number;
}

/**
 * Met when the control value is or isn't empty/null
 */
export interface FormConditionEmpty {
    source: FormConditionControlSource;
    empty: boolean;
}

/**
 * Met when the control is or isn't visible
 */
export interface FormConditionVisible {
    source: FormConditionControlSource;
    visible: boolean;
}

/**
 * Met when the control is or isn't disabled
 */
export interface FormConditionDisabled {
    source: FormConditionControlSource;
    disabled: boolean;
}
