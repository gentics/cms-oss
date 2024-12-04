import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { AbstractControl, FormGroup, ValidationErrors, Validator } from '@angular/forms';
import { BaseFormElementComponent, FormProperties } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { combineLatest } from 'rxjs';
import { distinctUntilChanged, filter, map, tap } from 'rxjs/operators';

const INITIAL_UNSET_VALUE = Symbol('initial-unset-value');

@Component({ template: '' })
export abstract class BasePropertiesComponent<T> extends BaseFormElementComponent<T> implements OnInit, OnChanges, Validator {

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
     * public ctl: FormControl = new UntypedFormControl({});
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

    /**
     * If the `initialValue` input should be seen as "pure" - This component will not update the value directly,
     * but will *always* use the dedicated output to request a change to the value.
     * @see initialValue
     */
    @Input()
    public pureInitialValue = false;

    /**
     * When the value changes and therefore warants an update to the `initialValue` to be applied.
     * This event typically only emits `false`, as the marking of `true` should be done in the parent.
     */
    @Output()
    public initialValueChange = new EventEmitter<boolean>();

    /**
     * Forwards an submit event of the form (if used), which can be used to determine if a user attempts
     * to save the current state of the properties.
     */
    @Output()
    public submit = new EventEmitter<void>();

    /**
     * The form which should be used in the component template for all form interactions.
     */
    public form: FormGroup<FormProperties<T>>;

    /**
     * Internal flag if the form should setup the value changes only after the first configuration.
     * This ignores the change performed by the first configuration and doesn't trigger a change for it (if any would occur).
     */
    protected delayedSetup = false;

    /** The control to which this component is bound to. */
    protected boundControl: AbstractControl<any, any>;

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
        this.booleanInputs.push(['initialValue', false]);
        // Set the value to this flag. Used to ignore changes until intial value has been provided.
        this.value = INITIAL_UNSET_VALUE as any;
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
                this.form.markAsPristine();
                this.form.updateValueAndValidity();
            }

            this.onValueReset();
        }
    }

    /**
     * Function which initializes the form and sets up all required change detections.
     */
    protected initializeForm(): void {
        this.form = this.createForm();
        this.configureForm(this.form.value as T);
        this.form.markAsPristine();

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
            // Do not emit values if the value hasn't been initialized yet
            filter(() => (this.value as any) !== INITIAL_UNSET_VALUE),
            // Do not emit values if disabled/pending
            filter(([, status]) => status !== 'DISABLED' && status !== 'PENDING'),
            map(([value]) => this.assembleValue(value as any)),
            distinctUntilChanged(isEqual),
        ).subscribe(value => {
            // Only trigger a change if the value actually changed or gone invalid.
            // Ignores the first value change, as it's a value from the initial setup.
            if (!this.form.pristine && !isEqual(value, this.value)) {
                this.triggerChange(value);
                this.onValueTrigger(value);
            }
            if (!this.pureInitialValue) {
                // Set it, in case that the parent-component has no binding for it
                this.initialValue = false;
            }
            this.initialValueChange.emit(false);
        }));

        this.subscriptions.push(this.form.statusChanges.subscribe(() => {
            if (this.boundControl) {
                this.boundControl.updateValueAndValidity();
            }
        }));
    }

    /**
     * Function which creates a new form (at initialization) for this component.
     * All controls should already be present and should be toggled by disabling them
     * via the `configureForm` method.
     */
    protected abstract createForm(): FormGroup;

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

    /** Hook which is called whenever `initialValue` is getting reset to `true`. */
    protected onValueReset(): void {}

    /** Hook which is called whenever the value has been dispatched to the parent. */
    protected onValueTrigger(value: T): void {}

    /** Validation implementation which simply forwards this forms validation state */
    public validate(control: AbstractControl<any, any>): ValidationErrors {
        this.boundControl = control;

        if (this.form.valid) {
            return null;
        }

        const err: ValidationErrors = {};
        Object.entries(this.form.controls).forEach(([name, ctl]) => {
            if (ctl.invalid) {
                err[name] = ctl.errors;
            }
        });

        return { propertiesError: err };
    }

    /**
     * A "wrapper" around the nullish access `this.value?.propertyName`, but with
     * an additional check if the value is actually rhe unset value/a symbol.
     * This broke some forms, as `Symbol` has a `description` property which caused some trouble.
     * @param property The property you want to access.
     * @returns The property-value, or null if not present.
     */
    protected safeValue<K extends keyof T>(property: K): T[K] | null {
        if (this.value === INITIAL_UNSET_VALUE || typeof this.value === 'symbol') {
            return null;
        }

        return this.value?.[property];
    }

    /**
     * Basic implementation which will simply put the value into the form.
     */
    protected override onValueChange(): void {
        if (this.form) {
            const tmpObj = {};
            Object.keys(this.form.controls).forEach(controlName => {
                if (
                    this.value != null
                    && typeof this.value !== 'symbol'
                    && this.value.hasOwnProperty(controlName)
                ) {
                    tmpObj[controlName] = this.value[controlName];
                }
            });
            this.form.patchValue(tmpObj);
        }
    }

    protected override onDisabledChange(): void {
        super.onDisabledChange();

        if (!this.form) {
            return;
        }

        /*
         * Special disabled handling, because angular forms are inconsistent.
         * Accoding to the documentation, angular form-groups itself should have it's own status
         * and should be able to be dis-/enabled without affecting their child elements.
         * This is not the case, and the option `onlySelf` is apparenly a lie, as it doesn't do anything.
         * Updating the state affects the form and all it's controls - however, only when *disabling* them.
         * When enabling the form, only the form is now enabled and the controls are still disabled.
         * Therefore, we do it manually here and fix this mess by doing it ourself.
         */

        // No change has to be applied
        if ((this.form.enabled && !this.disabled) || (this.form.disabled && this.disabled)) {
            return;
        }

        if (this.disabled) {
            this.form.enable({ emitEvent: false });
            Object.values(this.form.controls).forEach(ctrl => {
                ctrl.enable({ emitEvent: false });
            });
        } else {
            this.form.enable({ emitEvent: false });
            Object.values(this.form.controls).forEach(ctrl => {
                ctrl.enable({ emitEvent: false });
            });
        }

        this.configureForm(this.form.value as any, true);
        this.form.updateValueAndValidity();
    }

    /**
     * Simple event handler which may be used for nested properties components
     * to properly handle/forward the initialValue flag.
     */
    public updateInitialValueFlag(value: boolean): void {
        if (!this.pureInitialValue) {
            this.initialValue = value;
        }
        this.initialValueChange.emit(value);
    }

    /**
     * Trigger a submit to the parent, to potentially trigger a save of this state.
     */
    public triggerSubmit(): void {
        // Only allow submitting if this component in it self is valid.
        if (this.form && this.form.valid) {
            this.submit.emit();
        }
    }
}
