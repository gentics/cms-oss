import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    Output,
} from '@angular/core';
import { BaseComponent, cancelEvent } from '@gentics/ui-core';

@Component({
    selector: 'gtx-mesh-copy-value',
    templateUrl: './copy-value.component.html',
    styleUrls: ['./copy-value.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CopyValueComponent extends BaseComponent {
    public readonly cancelEvent = cancelEvent;

    @Input()
    public animate: false;

    public icon = 'content_copy';

    @Input()
    public label: string;

    @Input()
    public value: string;

    @Output()
    public copy = new EventEmitter<void | Error>();

    constructor(protected changeDetector: ChangeDetectorRef) {
        super(changeDetector);
    }

    async copyToClipboard(): Promise<void> {
        try {
            await navigator.clipboard.writeText(this.value);
            this.copy.emit();
            if (this.animate) {
                this.animateCopy(1)
            }
        } catch (err) {
            this.copy.emit(err);
        }
    }

    private animateCopy(seconds: number): void {
        this.icon = 'check';
        this.changeDetector.markForCheck();

        setTimeout(() => {
            this.icon = 'content_copy';
            this.changeDetector.markForCheck();
        }, 1000 * seconds);
    }
}
