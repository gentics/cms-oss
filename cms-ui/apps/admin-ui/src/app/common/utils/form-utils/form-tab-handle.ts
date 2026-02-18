import { AbstractControl } from '@angular/forms';
import { deepFreeze } from '@gentics/ui-core/utils/deep-freeze/deep-freeze';

/**
 * Allows interaction with the form in a single detail tab.
 */
export interface FormTabHandle {

    /**
     * @returns `true` if the user has changed the form's data.
     */
    isDirty(): boolean;

    /**
     * @returns `true` if the form data is valid.
     */
    isValid(): boolean;

    /**
     * Saves the changes made by the user and marks the form as not modified.
     */
    save(): Promise<void>;

    /**
     * Reverts the changes made by the user and marks the form as not modified.
     */
    reset?(): Promise<void>;

}

/**
 * Reusable `FormTabHandle` for tabs that do not have a form.
 */
export const NULL_FORM_TAB_HANDLE: FormTabHandle = deepFreeze({
    isDirty: () => false,
    isValid: () => true,
    save: () => Promise.resolve(),
});

/**
 * Wraps a `FormGroup` in a `FormTabHandle`.
 */
export class FormGroupTabHandle implements FormTabHandle {

    constructor(
        public formGroup: AbstractControl,
        private config: Pick<FormTabHandle, 'save' | 'reset'>,
    ) {}

    isDirty(): boolean {
        return this.formGroup.dirty;
    }

    isValid(): boolean {
        return this.formGroup.valid;
    }

    save(): Promise<void> {
        return this.config.save();
    }

    reset(): Promise<void> {
        if (this.config.reset) {
            return this.config.reset();
        }
        return Promise.resolve();
    }
}

