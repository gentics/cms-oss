import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ColorThemes } from '../../common';

/**
 * A Toast notification component. Not to be used directly - see Notification service for
 * documentation.
 */
@Component({
    selector: 'gtx-toast',
    templateUrl: './toast.component.html',
    styleUrls: ['./toast.component.scss'],
})
export class ToastComponent implements OnInit, AfterViewInit, OnDestroy {

    @ViewChild('toast', { static: true })
    toastRef: ElementRef<HTMLElement>;

    message: string;
    messageLines: string[];
    type: ColorThemes | string;
    position = {
        top: 10,
        right: 10,
    };
    actionLabel: string;
    actionOnClick: () => void;
    dismissFn: () => void;
    dismissOnClick = true;
    dismissing = false;

    private hammerManager: HammerManager;

    constructor(private elementRef: ElementRef) { }

    ngOnInit(): void {
        this.messageLines = (this.message || '').split('\n');
    }

    ngAfterViewInit(): void {
        this.initSwipeHandler();
    }

    ngOnDestroy(): void {
        if (this.hammerManager) {
            this.hammerManager.destroy();
            this.hammerManager = undefined;
        }
    }

    /**
     * Returns the height of the toast div.
     */
    getHeight(): number {
        return this.toastRef.nativeElement.getBoundingClientRect().height;
    }

    /**
     * Returns a CSS transform string for positioning
     */
    getTransform(): string {
        if (this.dismissing) {
            return `translate3d(100%, ${this.position.top}px, 0)`;
        } else {
            return `translate3d(0, ${this.position.top}px, 0)`;
        }
    }

    /**
     * Begin the dismiss animation
     */
    startDismiss(): void {
        this.dismissing = true;
    }

    /**
     * Invoke the action onClick handler if defined.
     */
    actionClick(): void {
        if (typeof this.actionOnClick === 'function') {
            this.actionOnClick();
        }
    }


    /**
     * Manual dismiss which is invoked when the toast is clicked.
     */
    dismiss(): void {
        if (this.dismissOnClick && typeof this.dismissFn === 'function') {
            this.dismissFn();
        }
    }

    /**
     * Set up a Hammerjs-based swipe gesture handler to dismiss toasts.
     */
    private initSwipeHandler(): void {
        this.hammerManager = new Hammer(this.elementRef.nativeElement);
        this.hammerManager.on('swipe', (e: HammerInput) => {
            if (e.pointerType === 'touch') {
                // Hammerjs represents directions with an enum; 4 = right.
                if (e.direction === 4) {
                    this.dismiss();
                }
            }
        });
    }
}
