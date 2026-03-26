import { FormSchemaProperty } from './form';

/**
 * Which condition has to be met in order to display this setting to the editor
 */
export type Condition
    = | ConditionOr
      | ConditionAnd
      | ConditionNot
      | ConditionEquals
      | ConditionMatches
      | ConditionContains
      | ConditionIncludedIn
      | ConditionBiggerThan
      | ConditionLowerThan
      | ConditionEmpty
      | ConditionVisible
      | ConditionDisabled
      | ConditionOr
      | ConditionAnd
      | ConditionNot
      | ConditionEquals
      | ConditionMatches
      | ConditionContains
      | ConditionIncludedIn
      | ConditionBiggerThan
      | ConditionLowerThan
      | ConditionEmpty
      | ConditionVisible
      | ConditionDisabled;

/**
 * The source from where the value should be pulled from
 */
export type ConditionControlSource
    = | ConditionSourceSetting
      | ConditionSourceSchema;

export type CompareValue = string | number | boolean | null;

/**
 * Is met when one or more of the provided conditions are met
 */
export interface ConditionOr {
    or: Condition[];
}

/**
 * Is met when all provided conditions are met
 */
export interface ConditionAnd {
    and: Condition[];
}

/**
 * Inverts the provided condition result
 */
export interface ConditionNot {
    not: Condition;
}

/**
 * Met when the control value strictly equals the specified value
 */
export interface ConditionEquals {
    source: ConditionControlSource;
    equals: CompareValue;
}

/**
 * Source which targets another setting which has been defined
 */
export interface ConditionSourceSetting {
    /** ID of the setting from this element which is to be checked against */
    setting: string;
}

/**
 * Source which targets one of the defined form schema fields or the current schema property
 */
export interface ConditionSourceSchema {
    /**
     * All values which are defined in 'schema.properties' of a form
     */
    schema: Omit<keyof FormSchemaProperty, 'properties'>;
}

/**
 * Met when the specified RegExp matches the control value
 */
export interface ConditionMatches {
    source: ConditionControlSource;
    /**
     * RegExp to match against the control value
     */
    matches: string;
}

/**
 * Met when one of the controls (array/list) values equals the specified value
 */
export interface ConditionContains {
    source: ConditionControlSource;
    contains: CompareValue;
}

/**
 * Met when the controls value equals one of the specified values
 */
export interface ConditionIncludedIn {
    source: ConditionControlSource;
    includedIn: [CompareValue, ...CompareValue[]];
}

/**
 * Met when the control value is (excluding) bigger than the specified value
 */
export interface ConditionBiggerThan {
    source: ConditionControlSource;
    biggerThan: number;
}

/**
 * Met when the control value is (excluding) lower than the specified value
 */
export interface ConditionLowerThan {
    source: ConditionControlSource;
    lowerThan: number;
}

/**
 * Met when the control value is or isn't empty/null
 */
export interface ConditionEmpty {
    source: ConditionControlSource;
    empty: boolean;
}

/**
 * Met when the control is or isn't visible
 */
export interface ConditionVisible {
    source: ConditionControlSource;
    visible: boolean;
}

/**
 * Met when the control is or isn't disabled
 */
export interface ConditionDisabled {
    source: ConditionControlSource;
    disabled: boolean;
}
