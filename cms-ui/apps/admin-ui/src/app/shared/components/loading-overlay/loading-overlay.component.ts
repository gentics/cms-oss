import { ChangeDetectionStrategy, Component, ElementRef, Input, Renderer2, ViewChild } from '@angular/core';

/**
 * Use this overlay displaying a centered loading animation to indicate your component is loading.
 * Wrap the most outer component markup element with CSS `position: relative` to capture this overlay within the component.
 * Different to other loading indicators in this app this component indicates the loading state on component level.
 */
@Component({
    selector: 'gtx-loading-overlay',
    templateUrl: './loading-overlay.component.html',
    styleUrls: ['./loading-overlay.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoadingOverlayComponent {
    @ViewChild('messageEl', { static: false }) messageElRef: ElementRef;

    @Input() set visible(val: boolean) {
        this._visible = val;
        this.hideSpinner = false;
    }

    @Input() maxHeight = false;

    _message = null;

    @Input() set message(value: string) {
        if (value) {
            if (this._message === null) {
                this.renderer.addClass(this.messageElRef.nativeElement, 'delayed');
            } else {
                this.renderer.removeClass(this.messageElRef.nativeElement, 'delayed');
            }

            this.renderer.removeClass(this.messageElRef.nativeElement, 'active');
            setTimeout(() => {
                this.renderer.addClass(this.messageElRef.nativeElement, 'active');
            }, 50);
        }

        this._message = value;
    }

    get message(): string {
        return this._message;
    }

    _visible = false;
    hideSpinner = false;

    constructor(private renderer: Renderer2) { }
}
