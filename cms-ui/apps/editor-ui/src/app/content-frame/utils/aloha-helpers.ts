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

export const PATCHED_ALOHA_FN = Symbol('gtx-patched-aloha-fn');
export const ORIGINAL_ALOHA_FN = Symbol('gtx-original-aloha-fn');

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

export type ExtractFunctions<T extends object> = {
    // eslint-disable-next-line @typescript-eslint/ban-types
    [K in keyof T]-?: T[K] extends Function ? K : never
}[keyof T];

export type FunctionsOnly<T extends object> = {
    [K in ExtractFunctions<T>]: T[K];
}

export function patchAlohaFunction<T extends object, K extends ExtractFunctions<T>>(
    obj: T,
    name: K,
    fn: T[K],
): void {
    if (
        // Type safety
        obj == null
        || typeof obj !== 'object'
        || typeof obj[name] !== 'function'
        // Already patched, nothing to do
        || obj[name][PATCHED_ALOHA_FN]
    ) {
        return;
    }

    const original = obj[name];
    (obj as any)[name] = fn;
    obj[name][PATCHED_ALOHA_FN] = true;
    obj[name][ORIGINAL_ALOHA_FN] = original;
}

export function patchMultipleAlohaFunctions<T extends object>(
    obj: T,
    mapping: Partial<FunctionsOnly<T>>,
): void {
    if (
        obj == null
        || mapping == null
        || typeof obj !== 'object'
        || typeof mapping !== 'object'
    ) {
        return null;
    }

    Object.entries(mapping).forEach(([key, fn]) => {
        patchAlohaFunction(obj, key as any, fn);
    });
}

export function unpatchAlohaFunctions<T extends object, K extends ExtractFunctions<T>>(obj: T, ...names: K[]): void {
    if (obj == null || names == null) {
        return;
    }
    if (typeof names === 'string') {
        names = [names];
    } else if (!Array.isArray(names)) {
        return;
    }

    for (const fnName of names) {
        if (typeof obj[fnName] !== 'function' || !obj[fnName]?.[PATCHED_ALOHA_FN]) {
            continue;
        }
        const fn = obj[fnName]?.[ORIGINAL_ALOHA_FN];
        if (!fn) {
            console.warn(`Patched function "${fnName as string}" does not have an original function stored!`, obj);
            continue;
        }
        obj[fnName] = fn;
    }
}

export function unpatchAllAlohaFunctions<T extends object>(obj: T): void {
    if (obj == null || typeof obj !== 'object') {
        return;
    }

    unpatchAlohaFunctions(obj, Object.keys(obj) as any);
}
