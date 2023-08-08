import { Component, ChangeDetectionStrategy, Input, Output, EventEmitter } from '@angular/core';
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
    public label: string;

    @Input()
    public value: string;

    @Output()
    public copy = new EventEmitter<void | Error>();

    async copyToClipboard(): Promise<void> {
        try {
            await navigator.clipboard.writeText(this.value);
            this.copy.emit();
        } catch (err) {
            this.copy.emit(err);
        }
    }
}
