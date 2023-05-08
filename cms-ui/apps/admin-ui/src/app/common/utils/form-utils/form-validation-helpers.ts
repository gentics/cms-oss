import { AbstractControl, ValidatorFn } from '@angular/forms';
import { isEqual } from 'lodash';
import { combineLatest, Observable } from 'rxjs';
import { map, startWith } from 'rxjs/operators';

/**
 * @returns An observable that emits the validity status of the specified `AbstractControl`.
 */
export function createFormValidityTracker(formGroup: AbstractControl): Observable<boolean> {
    return formGroup.statusChanges.pipe(
        map(() => formGroup.valid),
        startWith(formGroup.valid),
    );
}

/**
 * @returns An observable that emits the dirty status of the specified `AbstractControl`.
 */
export function createFormDirtyTracker(formGroup: AbstractControl): Observable<boolean> {
    return formGroup.valueChanges.pipe(
        map(() => formGroup.dirty),
        startWith(formGroup.dirty),
    );
}

/**
 * Combines `createFormValidityTracker()` and `createFormDirtyTracker()` for use with the disabled property
 * of a save button.
 *
 * @returns An observable that emits `true` if the save button for a AbstractControl needs to be disabled.
 */
export function createFormSaveDisabledTracker(formGroup: AbstractControl): Observable<boolean> {
    return combineLatest([
        createFormDirtyTracker(formGroup),
        createFormValidityTracker(formGroup),
    ]).pipe(
        map(([dirty, valid]) => !dirty || !valid),
    );
}

export function blacklistValidator(blacklist: any[] | (() => any[])): ValidatorFn {
    return (control) => {
        let blacklistToCheck: any[];

        if (typeof blacklist === 'function') {
            blacklistToCheck = blacklist();
        } else {
            blacklistToCheck = blacklist;
        }

        if (control.value == null || !Array.isArray(blacklistToCheck) || blacklistToCheck.length === 0) {
            return null;
        }

        if (Array.isArray(control.value)) {
            for (const arrValue of control.value) {
                for (const bannedItem of blacklistToCheck) {
                    if (isEqual(arrValue, bannedItem)) {
                        return { blacklist: bannedItem };
                    }
                }
            }
            return null;
        }

        for (const bannedItem of blacklistToCheck) {
            if (isEqual(control.value, bannedItem)) {
                return { blacklist: bannedItem };
            }
        }
    }
}

type InvalidLanguagesCallback = (invalidLanguages: string[]) => any;

export function createI18nRequiredValidator(requiredLanguages: string[] | (() => string[]), callback?: InvalidLanguagesCallback): ValidatorFn {
    return (control) => {
        if (control.value == null || typeof control.value !== 'object') {
            return { required: true };
        }

        let languagesToCheck: string[];

        if (typeof requiredLanguages === 'function') {
            languagesToCheck = requiredLanguages();
        } else {
            languagesToCheck = requiredLanguages;
        }

        if (!Array.isArray(languagesToCheck)) {
            return null;
        }

        const missingLanguages = new Set(languagesToCheck);
        const emptyLanguages = new Set<string>();

        for (const lang of languagesToCheck) {
            const langValue = control.value[lang];
            if (typeof langValue === 'string') {
                missingLanguages.delete(lang);

                if (langValue.trim().length === 0) {
                    emptyLanguages.add(lang);
                }
            }
        }

        const err: { missing?: string[]; required?: string[] } = {};
        let hasError = false;

        if (missingLanguages.size > 0) {
            hasError = true;
            err.missing = Array.from(missingLanguages);
        }

        if (emptyLanguages.size > 0) {
            hasError = true;
            err.required = Array.from(emptyLanguages);
        }

        if (typeof callback === 'function') {
            const invalidLanguages = new Set<string>();
            missingLanguages.forEach(l => invalidLanguages.add(l));
            emptyLanguages.forEach(l => invalidLanguages.add(l));
            callback(Array.from(invalidLanguages));
        }

        return hasError ? err : null;
    }
}

export function createNotNullValidator(): ValidatorFn {
    return (control: AbstractControl) => {
        if (control != null && control.value != null) {
            return null;
        }

        return { null: true };
    }
}
