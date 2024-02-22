import { AbstractControl } from '@angular/forms';
import { DynamicControlConfiguration, ExposedControl } from '@gentics/aloha-models';
import { Subscription } from 'rxjs';

const READONLY_PROPERTIES: (keyof ExposedControl<any>)[] = [
    'value',
    'enabled',
    'dirty',
    'pristine',
    'valid',
];

const FORWARD_METHODS: (keyof ExposedControl<any>)[] = [
    'setValue',
    'enable',
    'disable',
    'markAsDirty',
    'markAsPristine',
    'updateValueAndValidity',
];

export function exposeControl<T>(formControl: AbstractControl<T>): ExposedControl<T> {
    const obj: ExposedControl<T> = {} as any;

    for (const fnProp of FORWARD_METHODS) {
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        obj[fnProp as any] = (...args) => (formControl as any)[fnProp](...args);
    }

    for (const prop of READONLY_PROPERTIES) {
        Object.defineProperty(obj, prop, {
            configurable: true,
            enumerable: true,
            get: () => formControl[prop],
            set: () => {},
        });
    }

    return obj;
}

export function applyControl<T>(formControl: AbstractControl<T>, config: DynamicControlConfiguration<T>): Subscription | null {
    let sub: Subscription | null = null;

    if (typeof config.validate === 'function') {
        formControl.addValidators((ctl) => {
            return config.validate(ctl.value);
        });
    }

    if (typeof config.onChange === 'function') {
        const exposed = exposeControl(formControl);
        sub = formControl.valueChanges.subscribe(value => {
            config.onChange(value, exposed);
        });
    }

    return sub;
}
