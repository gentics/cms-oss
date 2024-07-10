import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { ControlValueAccessor } from '@angular/forms';
import { BaseComponent } from '../base-component/base.component';

/**
 * Base class for all components which can/are used in a form (via ngModel or via FormControls).
 * Provides a basic and consistent way to use them and prevents basic boilerplate
 * code in every component.
 */
@Component({ template: '' })
export abstract class BaseFormElementComponent<T>
    extends BaseComponent
    implements ControlValueAccessor, OnChanges {

    /**
     * The label to be displayed for this control.
     */
    @Input()
    public label: string;

    /**
     * If this control is required to be filled out or not.
     */
    @Input()
    public required = false;

    /**
     * If this control is disallowed to manage the state internally.
     */
    @Input()
    public pure = false;

    /**
     * The value of the control.
     */
    @Input()
    public value: T;

    /**
     * Event which triggers whenever the value is supposed to change.
     */
    @Output()
    public valueChange = new EventEmitter<T>();

    /**
     * Event which triggers whenever this control has been touched/dirtied.
     */
    @Output()
    public touch = new EventEmitter<void>();

    /**
     * Since Angular 16, the #setDisabledState function is getting called initially with the disabled state.
     * Even if it's false.
     * To not lock our form and then unlock it again for no reason (and pushing a change which shouldn't occur),
     * we check here if it has been called once already.
     */
    protected hasSetInitialDisabled = false;

    /** Internal values for control-value accessor impl */
    private cvaChange: (value: T) => void;
    private cvaTouch: () => void;

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
        this.booleanInputs.push('required' , 'pure');
    }

    /* Life-Cycle hooks */

    public ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.value) {
            this.onValueChange();
            this.changeDetector.markForCheck();
        }
    }

    /* Methods for implementing controls */

    /**
     * Hook which is called whenever the value has been changed.
     */
    protected abstract onValueChange(): void;

    /**
     * Optional hook to get the final value which is being emitted on value changes/triggers.
     */
    protected getFinalValue(): T {
        return this.value;
    }

    /**
     * Hook which is called whenever the component is being touched by the user.
     */
    protected onTouch(): void {}

    /**
     * Function to trigger all required external hooks and to update the
     * internal state correctly, when the value is supposed to change.
     *
     * @param value The new value
     */
    public triggerChange(value: T): void {
        if (!this.pure) {
            this.value = value;
            this.onValueChange();
            this.changeDetector.markForCheck();
        }

        const changeValue = this.getFinalValue();
        if (typeof this.cvaChange === 'function') {
            this.cvaChange(changeValue);
        }

        this.valueChange.emit(changeValue);
    }

    /**
     * Function to trigger all required external hooks whenever the actual
     * control has been touched/dirtied.
     */
    public triggerTouch(): void {
        if (typeof this.cvaTouch === 'function') {
            this.cvaTouch();
        }

        this.touch.emit();
    }

    /* Control Value Accessort implementation */

    public writeValue(value: T): void {
        this.value = value;
        this.onValueChange();
        this.changeDetector.markForCheck();
    }

    public registerOnChange(fn: (change: T) => void): void {
        this.cvaChange = fn;
    }

    public registerOnTouched(fn: () => void): void {
        this.cvaTouch = fn;
    }

    public setDisabledState(isDisabled: boolean): void {
        if (this.disabled !== isDisabled) {
            this.disabled = isDisabled;
            this.changeDetector.markForCheck();
            this.onDisabledChange();
        }
        this.hasSetInitialDisabled = true;
    }
}
