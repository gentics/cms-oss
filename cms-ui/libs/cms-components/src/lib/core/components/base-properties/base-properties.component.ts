import { ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';
import { BaseFormElementComponent } from '@gentics/ui-core';
import { isEqual } from 'lodash';
import { combineLatest } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, tap } from 'rxjs/operators';
import { CONTROL_INVALID_VALUE } from '../../../common';

@Component({ template: '' })
export abstract class BasePropertiesComponent<T> extends BaseFormElementComponent<T> implements OnInit {

    /** If it should skip the initial change trigger when initializing. */
    @Input()
    public skipInitialChange = false;

    /**
     * The form which should be used in the component template for all form interactions.
     */
    public form: UntypedFormGroup;

    constructor(changeDetector: ChangeDetectorRef) {
        super(changeDetector);
        this.booleanInputs.push('skipInitialChange');
    }

    public ngOnInit(): void {
        this.initializeForm();

        this.form.updateValueAndValidity();

        // Special opt-out for the initial change trigger
        if (!this.skipInitialChange) {
            // Trigger an initial change, to make the parent form detect if these properties
            // are invalid in the beginning - Initial value may be invalid and would otherwise only
            // be detected/validated on user-input which is not what we want to achieve with this.
            this.triggerChange(this.form.valid ? this.form.value : CONTROL_INVALID_VALUE);
        }
    }

    /**
     * Function which initializes the form and sets up all required change detections.
     */
    protected initializeForm(): void {
        this.form = this.createForm();

        this.configureForm(this.value);
        this.form.markAsPristine();

        let firstChange = true;

        this.subscriptions.push(combineLatest([
            this.form.valueChanges.pipe(
                distinctUntilChanged(isEqual),
                tap(value => this.configureForm(value)),
            ),
            this.form.statusChanges,
        ]).pipe(
            map(([value, status]) => {
                if (status === 'VALID') {
                    return this.assembleValue(value);
                }
                return CONTROL_INVALID_VALUE;
            }),
            distinctUntilChanged(isEqual),
            debounceTime(100),
        ).subscribe(value => {
            // Only trigger a change if the value actually changed or gone invalid.
            // Ignores the first value change, as it's a value from the initial setup.
            if (value === CONTROL_INVALID_VALUE || (!firstChange && !isEqual(this.value, value))) {
                this.triggerChange(value);
            }
            firstChange = false;
        }));

    }

    // Override to fix the typings
    override triggerChange(value: T | typeof CONTROL_INVALID_VALUE): void {
        super.triggerChange(value as any);
    }

    /**
     * Function which creates a new form (at initialization) for this component.
     * All controls should already be present and should be toggled by disabling them
     * via the `configureForm` method.
     */
    protected abstract createForm(): UntypedFormGroup;

    /**
     * Hook for whenever the form value changes, to configure the form controls.
     *
     * @param value The current form value.
     * @param loud If the dis-/enabling of the controls should be with an event (Trigger a value change). (Defaults to `false`)
     */
    protected abstract configureForm(value: T, loud?: boolean): void;

    /**
     * Function which is getting called whenever a pushable form value change occurred.
     * This function must convert the value from the form to a value for publishing.
     *
     * @param value The current form value.
     */
    protected abstract assembleValue(value: T): T;

    /**
     * Basic implementation which will simply put the value into the form.
     */
    protected onValueChange(): void {
        if (this.form && this.value) {
            const tmpObj = {};
            Object.keys(this.form.controls).forEach(controlName => {
                tmpObj[controlName] = this.value?.[controlName] || null;
            });
            this.form.setValue(tmpObj);
        }
    }
}
