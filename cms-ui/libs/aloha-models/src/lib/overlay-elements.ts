/**
 * A simplified API for Angulars FormControls.
 * Allows to manage the control is a defined manner.
 */
export interface ExposedControl<T> {
    /**
     * The current value of the control.
     */
    readonly value: T;
    /**
     * If the control is currently enabled.
     */
    readonly enabled: boolean;
    /**
     * If the control is dirty/has been touched by the user.
     */
    readonly dirty: boolean;
    /**
     * If the control is pristine/has not yet been touched by the user.
     */
    readonly pristine: boolean;
    /**
     * If the control is currently valid/has no validation errors.
     */
    readonly valid: boolean;
    /**
     * Sets/Updates the value of this control.
     */
    setValue(value: T): void;
    /**
     * Enables the control (if it is currently disabled).
     */
    enable(): void;
    /**
     * Disables the control (if it is currently enabled).
     */
    disable(): void;
    /**
     * Sets this control as dirty/touched by the user.
     */
    markAsDirty(): void;
    /**
     * Sets this control as pristine/not yet touched by the user.
     */
    markAsPristine(): void;
    /**
     * Synchronizes the value of the control and the component and runs the validation again.
     */
    updateValueAndValidity(): void;
}

export interface ExposedFormControl<T> extends ExposedControl<T> {
    controls?: {
        [key in keyof T]: DynamicControlConfiguration<T[key]>;
    };
}

/**
 * Configuration for a single Control to manage it's content correctly.
 */
export interface DynamicControlConfiguration<T> {
    /**
     * The type of the component which is supposed to handle the value.
     */
    type: string;
    /**
     * Options for the component. Is component dependend.
     */
    options?: Record<string, any>;
    /**
     * Validates a control and determines if it is valid or not.
     *
     * @param controlValue The current value of the entire form.
     * @returns `null` if the control is valid, or a record of issues/errors for this control.
     */
    validate?: (controlValue: T) => null | Record<string, any>;
    /**
     * Simple callback which is called whenever the control value changes,
     * to be able to react to it's changes.
     *
     * @param controlValue The current value of the control.
     * @param control An api for the control itself to manage it.
     */
    onChange?: (controlValue: T, control: ExposedControl<T>) => void;
}

export interface OverlayElementSettings<T> {
    /**
     * Value of the form/component and what it's starting value is.
     */
    initialValue?: T;
    /**
     * If the overlay eleemnt should close when the escape key is being pressed.
     */
    closeOnEscape?: boolean;
    /**
     * If the overlay element should close when the user clicks outside of it.
     */
    closeOnOverlayClick?: boolean;
}

/**
 * Control for an opened overlay element.
 */
export interface OverlayElementControl<T> {
    /**
     * Close the overlay element if it isn't closed yet.
     */
    close: () => void;
    /**
     * @returns If the overlay element is still open.
     */
    isOpen: () => boolean;
    /**
     * The return value of the overlay element.
     */
    value: Promise<T>;
}

/**
 * Configuration for an entire Dynamic Form Modal.
 */
export interface DynamicFormModalConfiguration<T> extends OverlayElementSettings<T> {
    /**
     * Title of the Modal
     */
    title: string;
    /**
     * Controls which may manage the values of the element.
     */
    controls: {
        [key in keyof T]: DynamicControlConfiguration<T[key]>;
    };
    /**
     * Validates the entire form and determines if it is valid or not.
     *
     * @param formValue The current value of the entire form.
     * @returns `null` if the form is valid, or a record of issues/errors for this form.
     */
    validate?: (formValue: T) => null | Record<string, any>;
    /**
     * Simple callback which is called whenever a single control changes,
     * to be able to react to it's changes.
     *
     * @param formValue The current value of the form.
     * @param formControl The control of the form control itself to manage it.
     */
    onChange?: (formValue: T, formControl: ExposedControl<T>) => void;
}

interface BaseDialogButton {
    label: string;
    type?: 'default' | 'secondary' | 'success' | 'warning' | 'alert';
    flat?: boolean;
    // If true, clicking the button will cause
    // the promise to reject rather than resolve
    shouldReject?: boolean;
}

interface DialogValueButton<T> extends BaseDialogButton {
    // If specified, will be returned as the
    // value of the resolved promise (or the reason if rejected).
    returnValue?: T;
    shouldReject: never | false;
}

interface DialogRejectButton extends BaseDialogButton {
    shouldReject: true;
}

type DialogButton<T> = DialogValueButton<T> | DialogRejectButton;

export interface DynamicDialogConfiguration<T> {
    title: string;
    body?: string;
    buttons: DialogButton<T>[];
}

/**
 * Configuration for a dynamic dropdown.
 */
export interface DynamicDropdownConfiguration<T> extends DynamicControlConfiguration<T>, OverlayElementSettings<T> {
    /**
     * If the dropdown should render a confirm button and only resolve,
     * when the confirm button has been clicked.
     * This changes the behaviour from resolving as soon as a valid value has been selected.
     */
    resolveWithConfirmButton?: boolean;
}
