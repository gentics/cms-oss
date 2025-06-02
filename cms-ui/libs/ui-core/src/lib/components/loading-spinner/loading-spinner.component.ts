import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';
import { coerceToBoolean } from '../../utils';

/**
 * Use this overlay displaying a centered loading animation to indicate your component is loading.
 * Wrap the most outer component markup element with CSS `position: relative` to capture this overlay within the component.
 * Different to other loading indicators in this app this component indicates the loading state on component level.
 */
@Component({
    selector: 'gtx-loading-spinner',
    templateUrl: './loading-spinner.component.html',
    styleUrls: ['./loading-spinner.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class LoadingSpinnerComponent implements OnChanges, OnDestroy {

    /** If it should be visible */
    @Input({ transform: coerceToBoolean })
    public visible = false;

    /** Message for the user */
    @Input()
    public message: string = null;

    /** Notice that the loading takes longer. Set to `null` to disable functionality */
    @Input()
    public longerMessage: string = null;

    /** Milliseconds how long it should wait/delay the additional longer message. */
    @Input()
    public longerDelay = 3_000;

    public displayLonger = false;

    private timeoutId: number | null;

    constructor(
        protected changeDetector: ChangeDetectorRef,
    ) {}

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.visible) {
            this.displayLonger = false;

            if (this.visible) {
                this.timeoutId = window.setTimeout(() => {
                    this.displayLonger = true;
                    this.changeDetector.markForCheck();
                }, this.longerDelay);
            } else {
                this.clearTimeout();
            }
        }
    }

    ngOnDestroy(): void {
        this.clearTimeout();
    }

    private clearTimeout(): void {
        if (this.timeoutId) {
            window.clearTimeout(this.timeoutId);
        }
    }
}
