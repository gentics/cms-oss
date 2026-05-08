import { booleanAttribute, ChangeDetectionStrategy, Component, EventEmitter, Input, Output, ViewEncapsulation } from '@angular/core';
import { cancelEvent } from '../../utils';
import { BaseComponent } from '../base-component/base.component';

/**
 * A very minimalistic container component, to have a consistent form-element design.
 * Used as a container element of form-controls, to have
 * styling and layout to be consistent across all form elements.
 */
@Component({
    selector: 'gtx-form-element-container',
    templateUrl: './form-element-container.component.html',
    styleUrls: ['./form-element-container.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    encapsulation: ViewEncapsulation.None,
    standalone: false,
})
export class FormElementContainerComponent extends BaseComponent {
    /**
     * If the content should be displayed as readonly.
     * Only affects the styling.
     */
    @Input({ transform: booleanAttribute })
    public readonly = false;

    /**
     * If the box should be focusable.
     * Allows the user to focus the container with the keyboard.
     */
    @Input({ transform: booleanAttribute })
    public focusable = false;

    /**
     * If focusable, if the user should be able to click the container
     * with the keyboard.
     */
    @Input({ transform: booleanAttribute })
    public clickable = false;

    /**
     * Event for when the box is clicked (or with keyboard if focusable).
     */
    @Output()
    public boxClick = new EventEmitter<void>();

    public handleKeyPress(event: KeyboardEvent): void {
        if (this.disabled || this.readonly || !this.clickable) {
            return;
        }

        if (event.code === 'Space' || event.code === 'Enter') {
            cancelEvent(event);
            this.boxClick.emit();
        }
    }

    public handleClick(): void {
        this.boxClick.emit();
    }
}
