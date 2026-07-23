import { booleanAttribute, ChangeDetectionStrategy, Component, EventEmitter, HostListener, Input, Output } from '@angular/core';
import { cancelEvent } from '@gentics/common';

/**
 * A Button component.
 *
 * ```html
 * <gtx-button>Click me</gtx-button>
 * <gtx-button size="large">Buy Now!</gtx-button>
 * <gtx-button type="alert">Delete all stuff</gtx-button>
 * <gtx-button icon>
 *     <i class="material-icons">settings</i>
 * </gtx-button>
 * ```
 */
@Component({
    selector: 'gtx-button',
    templateUrl: './button.component.html',
    styleUrls: ['./button.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class ButtonComponent {

    /**
     * Sets the input field to be auto-focused. Handled by `AutofocusDirective`.
     */
    @Input({ transform: booleanAttribute })
    public autofocus = false;

    /**
     * Specify the size of the button. Can be "small", "regular" or "large".
     */
    @Input()
    public size: 'small' | 'regular' | 'large' = 'regular';

    /**
     * Type determines the style of the button. Can be "default", "secondary",
     * "success", "warning" or "alert".
     */
    @Input()
    public type: 'primary' | 'secondary' | 'success' | 'warning' | 'alert' | 'default' = 'default';

    /**
     * Setting the "flat" attribute gives the button a transparent background
     * and only depth on hover.
     */
    @Input({ transform: booleanAttribute })
    public flat = false;

    /**
     * Setting the "icon" attribute turns the button into an "icon button", which is
     * like a flat button without a border, suitable for wrapping an icon.
     */
    @Input({ transform: booleanAttribute })
    public icon = false;

    /**
     * Controls whether the button is disabled.
     */
    @Input({ transform: booleanAttribute })
    public disabled = false;

    /**
     * Set button as a submit button.
     */
    @Input({ transform: booleanAttribute })
    public submit = false;

    /**
     * Event for when the button is getting focused.
     */
    @Output()
    // eslint-disable-next-line @angular-eslint/no-output-native
    public focus = new EventEmitter<void>();

    /**
     * Event for when the button looses focus.
     */
    @Output()
    // eslint-disable-next-line @angular-eslint/no-output-native
    public blur = new EventEmitter<void>();

    // In some browsers, disabled elements don't fire mouse events, but bubble them up the DOM tree.
    // To not trigger actions when the button is disabled, we need to prevent them manually.
    @HostListener('click', ['$event'])
    preventDisabledClick(event: Event): void {
        if (event && this.disabled) {
            cancelEvent(event);
        }
    }

    public handleFocus(event?: Event): void {
        cancelEvent(event);
        this.focus.emit();
    }

    public handleBlur(event?: Event): void {
        cancelEvent(event);
        this.blur.emit();
    }
}
