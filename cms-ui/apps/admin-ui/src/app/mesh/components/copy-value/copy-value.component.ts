import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import { BaseComponent, cancelEvent } from '@gentics/ui-core';

@Component({
    selector: 'gtx-mesh-copy-value',
    templateUrl: './copy-value.component.html',
    styleUrls: ['./copy-value.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CopyValueComponent extends BaseComponent  {

    public readonly cancelEvent = cancelEvent;

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
            this.icon = 'checked';
            this.changeDetector.markForCheck()
        } catch (err) {
            this.copy.emit(err);
        }
    }
}
