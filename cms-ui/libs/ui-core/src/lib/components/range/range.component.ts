import {
    booleanAttribute,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    numberAttribute,
} from '@angular/core';
import { cancelEvent } from '@gentics/common';
import { UserAgentProvider } from '../../providers';
import { generateFormProvider } from '../../utils';
import { BaseFormElementComponent } from '../base-form-element/base-form-element.component';

/**
 * The Range wraps the native `<input type="range">` form element.
 *
 * ```html
 * <gtx-range [(ngModel)]="latitude" step="5" min="-180" max="180"></gtx-range>
 * ```
 */
@Component({
    selector: 'gtx-range',
    templateUrl: './range.component.html',
    styleUrls: ['./range.component.scss'],
    providers: [generateFormProvider(RangeComponent)],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class RangeComponent extends BaseFormElementComponent<number> {

    /**
     * Sets the input field to be auto-focused. Handled by `AutofocusDirective`.
     */
    @Input()
    public autofocus = false;

    /**
     * Maximum allowed value.
     */
    @Input({ transform: numberAttribute, required: true })
    public max: number;

    /**
     * Minimum allowed value.
     */
    @Input({ transform: numberAttribute })
    public min = 0;

    /**
     * Amount to increment by when sliding.
     */
    @Input({ transform: numberAttribute })
    public step = 1;

    /**
     * Set to false to not show the thumb label. Defaults to true.
     */
    @Input({ transform: booleanAttribute })
    public showThumb = true;

    /**
     * If it should display indicators for the steps
     */
    @Input({ transform: booleanAttribute })
    public showIndicators: boolean;

    /**
     * Which steps to use when showing the indicators
     */
    @Input({ transform: numberAttribute })
    public indicatorSteps: number | null = null;

    /**
     * If it should show the min/max value at the beginning/end of the slider
     */
    @Input({ transform: booleanAttribute })
    public showBounds: boolean;

    /**
     * Sets an id for the slider.
     */
    @Input()
    public id: string;

    /**
     * Name of the input.
     */
    @Input()
    public name: string;

    public focused = false;

    constructor(
        changeDetector: ChangeDetectorRef,
        public userAgent: UserAgentProvider,
    ) {
        super(changeDetector);
    }

    protected onValueChange(): void {
        // noop
    }

    onInput(e: InputEvent): void {
        cancelEvent(e);
        if (this.disabled) {
            return;
        }

        const newVal = Number((e.target as HTMLInputElement).value);
        this.triggerChange(newVal);
    }

    public override handleFocus(event?: Event): void {
        super.handleFocus(event);
        this.focused = true;
    }

    public override handleBlur(event?: Event): void {
        super.handleBlur(event);
        this.focused = false;
    }
}
