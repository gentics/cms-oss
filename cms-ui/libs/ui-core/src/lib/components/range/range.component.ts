import {
    booleanAttribute,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    Input,
    numberAttribute,
    ViewChild,
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

    @ViewChild('rangeField', { static: true })
    private rangeFieldRef: ElementRef<HTMLDivElement>;

    public active = false;
    public thumbOffset = '';

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

    onMousedown(e: MouseEvent): void {
        if (!this.disabled) {
            this.active = true;
            this.setThumbPosition(e);
        }
    }

    onMouseup(): void {
        this.active = false;
    }

    onMousemove(e: MouseEvent): void {
        if (this.disabled) {
            return;
        }
        if (this.active) {
            this.setThumbPosition(e);
        }
    }

    private setThumbPosition(e: MouseEvent): void {
        const endMargin = 8;
        const boundingRect = this.rangeFieldRef.nativeElement.getBoundingClientRect();
        const wrapperLeft = boundingRect.left;
        const wrapperWidth = boundingRect.width;
        let left = e.pageX - wrapperLeft;
        if (left < endMargin) {
            left = endMargin;
        } else if (left > wrapperWidth - endMargin) {
            left = wrapperWidth - endMargin;
        }
        this.thumbOffset = left + 'px';
    }
}
