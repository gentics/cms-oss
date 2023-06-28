import { ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup, UntypedFormGroup } from '@angular/forms';
import { BaseFormElementComponent } from '@gentics/ui-core';
import { isEqual } from 'lodash';
import { combineLatest } from 'rxjs';
import { debounceTime, distinctUntilChanged, filter, map, tap } from 'rxjs/operators';
import { CONTROL_INVALID_VALUE } from '../../../common';

@Component({ template: '' })
export abstract class BasePropertiesComponent<T> extends BaseFormElementComponent<T> implements OnInit {

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
    public form: UntypedFormGroup;

    /**
     * Internal flag if the form should setup the value changes only after the first configuration.
     * This ignores the change performed by the first configuration and doesn't trigger a change for it (if any would occur).
     */
    protected delayedSetup = false;

    constructor(changeDetector: ChangeDetectorRef) {
        super(changeDetector);
        this.booleanInputs.push(['initialValue', true]);
    }

    public ngOnInit(): void {
        this.initializeForm();
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
            this.form.updateValueAndValidity();
            if (this.delayedSetup) {
                this.setupFormSubscription();
            }
            this.form.markAsPristine();
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
                tap(value => {
                    this.configureForm(value);

                    // See comment above
                    setTimeout(() => {
                        this.form.updateValueAndValidity();
                        this.changeDetector.markForCheck();
                    });
                }),
            ),
            this.form.statusChanges,
        ]).pipe(
            // Do not emit values if disabled/pending
            filter(([, status]) => status !== 'DISABLED' && status !== 'PENDING'),
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
            if (value === CONTROL_INVALID_VALUE || (!this.initialValue && !isEqual(this.value, value))) {
                this.triggerChange(value);
            }
            // Set it, in case that the parent-component has no binding for it
            this.initialValue = false;
            this.initialValueChange.emit(false);
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

    /**
     * Basic implementation which will simply put the value into the form.
     */
    protected onValueChange(): void {
        if (this.form && this.value && (this.value as any) !== CONTROL_INVALID_VALUE) {
            const tmpObj = {};
            Object.keys(this.form.controls).forEach(controlName => {
                tmpObj[controlName] = this.value?.[controlName] || null;
            });
            this.form.setValue(tmpObj);
        }
    }

    public override setDisabledState(isDisabled: boolean): void {
        super.setDisabledState(isDisabled);

        if (isDisabled) {
            this.form.disable({ emitEvent: false });
        } else {
            this.form.enable({ emitEvent: false });
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
