import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    Output,
} from '@angular/core';
import { BaseComponent, cancelEvent } from '@gentics/ui-core';

@Component({
    selector: 'gtx-mesh-copy-value',
    templateUrl: './copy-value.component.html',
    styleUrls: ['./copy-value.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CopyValueComponent extends BaseComponent implements OnDestroy {

    @Input()
    public animate: false;

    public icon = 'content_copy';

    @Input()
    public label: string;

    @Input()
    public value: string;

    /**
     * Event which is triggered when the value has been copied via the copy button
     */
    @Output()
    public valueCopy = new EventEmitter<null | Error>();

    private timeoutId: number;

    ngOnDestroy(): void {
        super.ngOnDestroy();

        if (this.timeoutId != null) {
            window.clearTimeout(this.timeoutId);
            this.timeoutId = null;
        }
    }

    async copyToClipboard(): Promise<void> {
        let error: Error | null = null;

        try {
            await navigator.clipboard.writeText(this.value);
        } catch (err) {
            error = err;
        }

        // In case the writeText didn't work (due to unsecure connection or otherwise)
        if (error != null) {
            // Workaround to trigger a copy
            const textArea = document.createElement('textarea');
            textArea.value = this.value;
            document.body.appendChild(textArea);
            textArea.focus();
            textArea.select();

            try {
                document.execCommand('copy');
                // Clear the error, as we don't want to display that to the user/mark this as a failure
                error = null;
            } catch (err) {
                error = err;
            }

            document.body.removeChild(textArea);
        }

        // If there's no error we can animate a "success"
        if (error == null && this.animate) {
            this.animateCopy(1);
        }

        this.valueCopy.emit(error);
    }

    public cancelIfDisabled(event: Event): void {
        if (this.disabled) {
            cancelEvent(event);
        }
    }

    private animateCopy(seconds: number): void {
        if (this.timeoutId != null) {
            window.clearTimeout(this.timeoutId);
        }

        this.icon = 'check';
        this.changeDetector.markForCheck();

        this.timeoutId = window.setTimeout(() => {
            this.timeoutId = null;
            this.icon = 'content_copy';
            this.changeDetector.markForCheck();
        }, 1000 * seconds);
    }
}
