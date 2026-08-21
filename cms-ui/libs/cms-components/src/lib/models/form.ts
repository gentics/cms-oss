/**
 * @deprecated Shouldn't be used anymore, this functionality has been directly included into the base properties.
 * The forms will now self-validate, making this usage unecessary.
 */
export const CONTROL_INVALID_VALUE = Symbol();

export interface MultiValueValidityState {
    valid: boolean;
    errors: {
        [index: number]: ValidityState;
    };
}

export type ItemWithNode = { id: number; nodeId: number } | null;
