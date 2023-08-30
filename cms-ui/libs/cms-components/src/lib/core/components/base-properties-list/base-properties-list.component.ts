import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges } from '@angular/core';
import { AbstractControl, FormArray, FormControl, ValidationErrors, Validator } from '@angular/forms';
import { BaseFormElementComponent } from '@gentics/ui-core';
import { isEqual } from'lodash-es'
import { combineLatest } from 'rxjs';
import { distinctUntilChanged, filter, map, tap } from 'rxjs/operators';

const INIVIAL_UNSET_VALUE = Symbol('initial-unset-value');

@Component({ template: '' })
export abstract class BasePropertiesListComponent<T> extends BaseFormElementComponent<T[]> implements OnInit, OnChanges, OnDestroy, Validator {

    /**
     * Flag which indicates that the provided value is a new initial value.
     * This ensures that this component only triggers a change initially when
     * the value is invalid or has actually changed.
     *
     * May be used in the following way:
     *
     * Template
     * ```html
     * <some-properties
     *      [formControl]="ctl"
     *      [(initialValue)]="isInitial"
     * ></some-properties>
     * ```
     *
     * Implementation
     * ```ts
     * public ctl: FormControl = new UntypedFormControl({}, createNestedControlValidator());
     *
     * loadItem(): void {
     *      loadContentFromSomewhere().then(item => {
     *          isInitial = true;
     *          ctl.setValue(item);
     *          ctl.markAsPristine();
     *          ctl.updateValueAndValidity();
     *      });
     * }
     *
     * updateItem(): void {
     *      updateContent(ctl.value).then(() => this.loadItem());
     * }
     * ```
     */
    @Input()
    public initialValue = true;

    @Output()
    public initialValueChange = new EventEmitter<boolean>();

    /**
     * The form which should be used in the component template for all form interactions.
     */
    public form: FormArray<FormControl<T>>;

    /**
     * Internal flag if the form should setup the value changes only after the first configuration.
     * This ignores the change performed by the first configuration and doesn't trigger a change for it (if any would occur).
     */
    protected delayedSetup = false;

    /** The control to which this component is bound to. */
    protected boundControl: AbstractControl<any, any>;

    constructor(changeDetector: ChangeDetectorRef) {
        super(changeDetector);
        this.booleanInputs.push(['initialValue', true]);
        // Set the value to this flag. Used to ignore changes until intial value has been provided.
        this.value = INIVIAL_UNSET_VALUE as any;
    }

    public ngOnInit(): void {
        this.initializeForm();
    }

    public ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        // When the initialValue flag is updated, it means that the value may be significantly changed (usually full entity change).
        // Therefore, configure the form with the current value again to properly update the controls.
        if (changes.initialValue && this.initialValue) {
            if (this.form) {
                this.configureForm(this.form.value as any);
                this.form.updateValueAndValidity();
            }

            this.onValueReset();
        }
    }

    public ngOnDestroy(): void {
        super.ngOnDestroy();
    }

    /**
     * Function which initializes the form and sets up all required change detections.
     */
    protected initializeForm(): void {
        this.form = this.createForm();
        this.configureForm(this.value);

        // For some reason, changes from `configureForm` are only really applied,
        // when this is done a tick later. No idea why.
        setTimeout(() => {
            if (this.delayedSetup) {
                this.setupFormSubscription();
            }

            this.form.updateValueAndValidity();
        });

        if (!this.delayedSetup) {
            this.setupFormSubscription();
        }
        this.changeDetector.markForCheck();
    }

    protected setupFormSubscription(): void {
        this.subscriptions.push(combineLatest([
            this.form.valueChanges.pipe(
                distinctUntilChanged(isEqual),
                tap(value => this.configureForm(value)),
                map(() => this.form.value),
            ),
            this.form.statusChanges,
        ]).pipe(
            // Do not emit values if the disabled state and value hasn't initialized yet
            filter(() => this.hasSetInitialDisabled && (this.value as any) !== INIVIAL_UNSET_VALUE),
            // Do not emit values if disabled/pending
            filter(([, status]) => status !== 'DISABLED' && status !== 'PENDING'),
            map(([value]) => this.assembleValue(value)),
            distinctUntilChanged(isEqual),
        ).subscribe(value => {
            // Only trigger a change if the value actually changed or gone invalid.
            // Ignores the first value change, as it's a value from the initial setup.
            if (!this.initialValue && !isEqual(value, this.value)) {
                this.triggerChange(value);
                this.onValueTrigger(value);
            }
            // Set it, in case that the parent-component has no binding for it
            this.initialValue = false;
            this.initialValueChange.emit(false);
        }));

        this.subscriptions.push(this.form.statusChanges.subscribe(() => {
            if (this.boundControl) {
                this.boundControl.updateValueAndValidity();
            }
        }));
    }

    protected abstract createControl(value?: T): FormControl<T>;

    /**
     * Hook for whenever the form value changes, to configure the form controls.
     *
     * @param value The current form value.
     * @param loud If the dis-/enabling of the controls should be with an event (Trigger a value change). (Defaults to `false`)
     */
    protected abstract configureForm(value: T[], loud?: boolean): void;

    /**
     * Function which is getting called whenever a pushable form value change occurred.
     * This function must convert the value from the form to a value for publishing.
     *
     * @param value The current form value.
     */
    protected abstract assembleValue(value: T[]): T[];

    /** Hook which is called whenever `initialValue` is getting reset to `true`. */
    protected onValueReset(): void {}

    /** Hook which is called whenever the value has been dispatched to the parent. */
    protected onValueTrigger(value: T[]): void {}

    /** Validation implementation which simply forwards this forms validation state */
    public validate(control: AbstractControl<any, any>): ValidationErrors {
        this.boundControl = control;

        if (this.form.valid) {
            return null;
        }

        const err: ValidationErrors = {};
        this.form.controls.forEach((ctl, index) => {
            if (ctl.invalid) {
                err[index] = ctl.errors;
            }
        });

        return { propertiesError: err };
    }

    /**
     * Function which creates a new form (at initialization) for this component.
     * All controls should already be present and should be toggled by disabling them
     * via the `configureForm` method.
     */
    protected createForm(): FormArray<FormControl<T>> {
        const arr = Array.isArray(this.value) ? this.value : [];
        return new FormArray(arr.map(entry => {
            return this.createControl(entry);
        }));
    }

    public add(value?: T): void {
        this.form.push(this.createControl(value));
    }

    public remove(index: number): void {
        this.form.removeAt(index);
    }

    /**
     * Basic implementation which will simply put the value into the form.
     */
    protected onValueChange(): void {
        if (this.form) {
            if (this.value?.length === this.form.length) {
                this.form.setValue(this.value);
            } else {
                // If there's another amount of elements now, we have to re-create the form
                this.form.clear({ emitEvent: false });
                (this.value || []).forEach(entry => {
                    this.form.push(this.createControl(entry), { emitEvent: false });
                });
                this.form.updateValueAndValidity();
            }
        }
    }

    public override setDisabledState(isDisabled: boolean): void {
        super.setDisabledState(isDisabled);

        if (this.form) {
            this.form.updateValueAndValidity();
        }
    }

    /**
     * Simple event handler which may be used for nested properties components
     * to properly handle/forward the initialValue flag.
     */
    public updateInitialValueFlag(value: boolean): void {
        this.initialValue = value;
        this.initialValueChange.emit(value);
    }
}
