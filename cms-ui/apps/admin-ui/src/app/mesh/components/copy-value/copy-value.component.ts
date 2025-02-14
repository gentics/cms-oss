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

    copyToClipboard(): Promise<void> {
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
            this.copy.emit(err);
        }
		return Promise.resolve();
    }
	// https://github.com/microsoft/TypeScript-DOM-lib-generator/issues/1245
	/*  async copyToClipboard(): Promise<void> {
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
	async clipboardAllowed(): Promise<boolean> {
		const queryOpts = { name: 'clipboard-write' as PermissionName };
        const permissionStatus = await navigator.permissions.query(queryOpts);
		return permissionStatus.state === 'granted';
	}*/
    clipboardAllowed = Promise.resolve(true);

    private animateCopy(seconds: number): void {
        this.icon = 'check';
        this.changeDetector.markForCheck();

        setTimeout(() => {
            this.icon = 'content_copy';
            this.changeDetector.markForCheck();
        }, 1000 * seconds);
    }
}
