import { AbstractControl } from '@angular/forms';

export const CONTROL_INVALID_VALUE = Symbol();

export type FormProperties<T> = {
    [K in keyof T]: AbstractControl<T[K]>;
}

export interface MultiValueValidityState {
    valid: boolean;
    errors: {
        [index: number]: ValidityState;
    };
}
