/**
 * @deprecated Shouldn't be used anymore, this functionality has been improved
 * replaced  by the `BasePropertiesComponent` and `BasePropertiesListComponent` implementations.
 */
export const CONTROL_INVALID_VALUE = Symbol();

export interface MultiValueValidityState {
    valid: boolean;
    errors: {
        [index: number]: ValidityState;
    };
}

export type ItemWithNode = { id: number; nodeId: number; } | null;
