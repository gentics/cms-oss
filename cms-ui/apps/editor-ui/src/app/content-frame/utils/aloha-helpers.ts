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

export const OVERRIDE_FN = Symbol('gtx-fn-override');
export const ORIGINAL_FN = Symbol('gtx-original-fn');
export const DEFAULT_KEY = Symbol('gtx-override');
export const ALOHA_KEY = Symbol('gtx-aloha');

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

export function overrideFunction<T extends object, K extends ExtractFunctions<T>>(
    obj: T,
    name: K,
    fn: T[K],
    overrideKey: any = DEFAULT_KEY,
): void {
    if (
        // Type safety
        obj == null
        || typeof obj !== 'object'
        || typeof obj[name] !== 'function'
        // Already patched, nothing to do
        || obj[name][OVERRIDE_FN]
    ) {
        return;
    }

    const original = obj[name];
    // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
    const tmpName = `patched_${name as any}`;
    const patched = {
        [tmpName]: function(...args) {
            // eslint-disable-next-line @typescript-eslint/ban-types
            return (fn as Function).call(this, ...args);
        },
    }[tmpName];
    patched[OVERRIDE_FN] = overrideKey;
    patched[ORIGINAL_FN] = original;

    (obj as any)[name] = patched;
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
        overrideFunction(obj, key as any, fn, ALOHA_KEY);
    });
}

export function revertFunctionOverride<T extends object, K extends ExtractFunctions<T>>(
    obj: T,
    fnName: K,
    key: any = null,
): void {
    // Can't work with that
    if (obj == null || fnName == null) {
        return;
    }

    if (
        typeof obj[fnName] !== 'function'
        || !obj[fnName]?.[OVERRIDE_FN]
        || (key != null && obj[fnName][OVERRIDE_FN] !== key)
    ) {
        return;
    }

    const fn = obj[fnName]?.[ORIGINAL_FN];
    if (!fn) {
        console.warn(`Patched function "${fnName as string}" does not have an original function stored!`, obj);
    }

    obj[fnName] = fn;
}

export function revertKeyedFunctionOverrides<T extends object>(
    obj: T,
    key: any = DEFAULT_KEY,
): void {
    if (obj == null) {
        return;
    }

    const names = Object.keys(obj);
    for (const fnName of names) {
        revertFunctionOverride(obj, fnName as any, key);
    }
}

export function unpatchAllAlohaFunctions<T extends object>(obj: T): void {
    if (obj == null || typeof obj !== 'object') {
        return;
    }

    revertKeyedFunctionOverrides(obj, ALOHA_KEY);
}
