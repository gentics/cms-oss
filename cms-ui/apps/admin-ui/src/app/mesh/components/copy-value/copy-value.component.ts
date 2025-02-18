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

    // We cannot use navigator.permissions.query({ name: 'clipboard-write' as PermissionName })
    // because of https://github.com/microsoft/TypeScript-DOM-lib-generator/issues/1245
    async copyToClipboard(): Promise<void> {
        let error = null;
        try {
            await navigator.clipboard.writeText(this.value);
            this.copy.emit();
        } catch (err) {
            error = err;
        }
        if (error !== null) {
            try {
                const textArea = document.createElement('textarea');
                textArea.value = this.value;
                document.body.appendChild(textArea);
                textArea.focus();
                textArea.select();
                try {
                    document.execCommand('copy');
                } catch (err) {
                    console.error('Unable to copy to clipboard', err);
                }
                document.body.removeChild(textArea);
                this.copy.emit();
                if (this.animate) {
                    this.animateCopy(1)
                }
            } catch (err) {
                error = err;
            }
        }
        if (error === null) {
            if (this.animate) {
                this.animateCopy(1)
            }
        } else {
            this.copy.emit(error);
        }
        return Promise.resolve();
    }
    async clipboardAllowed(): Promise<boolean> {
        const queryOpts = { name: 'clipboard-write' as PermissionName };
        const permissionStatus = await navigator.permissions.query(queryOpts);
        return permissionStatus.state === 'granted';
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
