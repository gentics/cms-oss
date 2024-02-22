import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ComponentRef,
    HostListener,
    OnDestroy,
    Type,
    ViewChild,
    ViewContainerRef,
} from '@angular/core';
import { DEFAULT_MODAL_OPTIONS, IModalDialog, IModalOptions, ModalClosingReason } from '../../common';

/**
 * This is an internal component which is responsible for creating the modal dialog window and overlay.
 */
@Component({
    selector: 'gtx-dynamic-modal',
    templateUrl: './dynamic-modal.component.html',
    styleUrls: ['./dynamic-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DynamicModal implements OnDestroy {

    @ViewChild('portal', { read: ViewContainerRef, static: true })
    portal: ViewContainerRef;

    dismissFn: (reason?: ModalClosingReason) => any;

    visible = false;
    options: IModalOptions = DEFAULT_MODAL_OPTIONS;

    private cmpRef: ComponentRef<IModalDialog>;
    private openTimer: any;

    constructor(
        private changeDetector: ChangeDetectorRef,
    ) { }

    ngOnDestroy(): void {
        clearTimeout(this.openTimer);
        if (this.cmpRef && this.cmpRef.destroy) {
            this.cmpRef.destroy();
        }
    }

    setOptions(options: IModalOptions): void {
        this.options = Object.assign({}, DEFAULT_MODAL_OPTIONS, options);
    }

    /**
     * Inject the component which will appear within the modal.
     */
    injectContent(component: Type<IModalDialog>): ComponentRef<IModalDialog> {
        this.cmpRef = this.portal.createComponent(component);
        return this.cmpRef;
    }

    /**
     * Display the modal
     */
    open(): void {
        clearTimeout(this.openTimer);
        this.openTimer = setTimeout(() => {
            this.visible = true;
            this.changeDetector.markForCheck();
        }, 50);
    }

    /**
     * Programatically force the modal to close and resolve with the value passed.
     */
    forceClose(val?: any): void {
        this.cmpRef.instance.closeFn(val);
    }

    /**
     * Close the modal and call the cancelFn of the embedded component.
     */
    cancel(reason: ModalClosingReason): void {
        clearTimeout(this.openTimer);
        this.visible = false;
        this.cmpRef.instance.cancelFn(null, reason);
        this.cmpRef.destroy();
    }

    overlayClick(): void {
        if (this.options.closeOnOverlayClick) {
            this.cancel(ModalClosingReason.OVERLAY_CLICK);
        }
    }

    @HostListener('document:keydown', ['$event'])
    keyHandler(e: KeyboardEvent): void {
        if (e.which === 27 && this.options.closeOnEscape) {
            this.cancel(ModalClosingReason.ESCAPE);
        }
    }
}
