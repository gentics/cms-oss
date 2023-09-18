import { forwardRef, Provider, Type } from '@angular/core';
import { ControlValueAccessor, NG_VALIDATORS, NG_VALUE_ACCESSOR, Validator } from '@angular/forms';

export const generateFormProvider: (type: Type<ControlValueAccessor>) => Provider = (type) => ({
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => type),
    multi: true,
});

export const generateValidatorProvider: (type: Type<Validator>) => Provider = (type) => ({
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => type),
    multi: true,
});
