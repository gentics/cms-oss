import { forwardRef, Provider, Type } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export const generateFormProvider: (type: Type<ControlValueAccessor>) => Provider = (type) => ({
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => type),
    multi: true,
});
