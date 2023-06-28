import {
    ComponentFactoryResolver,
    ComponentRef,
    EventEmitter,
    Injectable,
    ViewContainerRef,
} from '@angular/core';
import { DEFAULT_TOAST_OPTIONS, INotificationOptions, OpenedNotification } from '../../common';
import { ToastComponent } from '../../components/toast/toast.component';
import { OverlayHostService } from '../../providers/overlay-host/overlay-host.service';

interface IOpenToast {
    toast: ToastComponent;
    dismissTimer: any;
}

/**
 * A toast notification service. Depends on the [`<gtx-overlay-host>`](#/overlay-host) being present in the app.
 *
 * ```typescript
 * let dismiss = this.notification.show({
 *     message: 'Content Saved',
 *     type: 'success',
 *     delay: 3000
 * });
 *
 * // to manually dismiss the toast
 * dismiss();
 * ```
 *
 * ## `INotificationOptions`
 *
 * The `show()` method takes an `INotificationOptions` object as its argument:
 *
 * | Property           | Type                                          | Default   | Description |
 * | --------           | ------------------------------                | -------   | ----------- |
 * | **message**        | `string`                                      | ''        | The message to display |
 * | **type**           | `'default'`,`'alert'`,`'success'`,`'warning'` | 'default' | The style of toast |
 * | **delay**          | `number`                                      | 3000      | ms before toast is dismissed. 0 == no dismiss |
 * | **dismissOnClick** | `boolean`                                     | true      | If true, the toast can be dismissed by click or swipe|
 * | **action.label**   | `string`                                      |           | Optional action label |
 * | **action.onClick** | `Function`                                    |           | Callback if action label is clicked |
 *
 */
@Injectable()
export class NotificationService {

    open$ = new EventEmitter<INotificationOptions>();
    private hostViewContainer: ViewContainerRef;
    private openToasts: IOpenToast[] = [];
    /*
     * Spacing between stacked toasts
     */
    private verticalMargin = 10;

    constructor(
        private componentFactoryResolver: ComponentFactoryResolver,
        overlayHostService: OverlayHostService,
    ) {
        overlayHostService.getHostView().then(view => {
            this.hostViewContainer = view;
        });
    }

    /**
     * Show a toast notification. Returns an object with a dismiss() method, which will
     * dismiss the toast when invoked.
     */
    public show(options: INotificationOptions): OpenedNotification {
        const mergedOptions: INotificationOptions = Object.assign({}, DEFAULT_TOAST_OPTIONS, options);
        const toast = this.createToast(mergedOptions);
        return {
            dismiss: (): void => toast.dismissFn(),
        };
    }

    /**
     * Used internally by the [OverlayHost](#/overlay-host) to clean up.
     */
    public destroyAllToasts(): void {
        this.openToasts.forEach((o: IOpenToast) => {
            if (typeof o.toast.dismissFn === 'function') {
                o.toast.dismissFn();
            }
        });
        this.openToasts = [];
    }

    /**
     * Dispose of the Toast component and remove its reference from the
     * openToasts array.
     */
    private destroyToast(componentRef: ComponentRef<ToastComponent>): void {
        const toast: ToastComponent = componentRef.instance;
        const index = this.getToastIndex(toast);
        if (-1 < index) {
            const timer: any = this.openToasts[index].dismissTimer;
            if (timer) {
                clearTimeout(timer);
            }
            this.openToasts.splice(index, 1);
        }
        toast.startDismiss();
        setTimeout(() => {
            componentRef.destroy();
            this.positionOpenToasts();
        }, 200);
    }

    /**
     * Dynamically create and load a new Toast component next to the
     * NotificationHost component in the DOM.
     */
    private createToast(options: INotificationOptions): ToastComponent {
        const toastFactory = this.componentFactoryResolver.resolveComponentFactory(ToastComponent);
        const ref = this.hostViewContainer.createComponent(toastFactory);
        const toast: ToastComponent = ref.instance;

        let dismissTimer: any;
        toast.message = options.message;
        toast.type = options.type;
        toast.dismissOnClick = options.dismissOnClick;
        toast.dismissFn = () => this.destroyToast(ref);

        if (options.action && options.action.label) {
            toast.actionLabel = options.action.label;
        }
        if (options.action && options.action.onClick) {
            toast.actionOnClick = options.action.onClick;
        }

        if (0 < options.delay) {
            dismissTimer = setTimeout(() => toast.dismissFn(), options.delay);
        }

        this.openToasts.unshift({
            toast,
            dismissTimer,
        });
        this.positionOpenToasts();
        return toast;
    }

    private positionOpenToasts(): void {
        setTimeout(() => {
            this.openToasts.forEach((o: IOpenToast) => {
                o.toast.position.top = this.getToastTop(o.toast);
            });
        });
    }

    /**
     * Calculates the value of the "top" offset for this toast by adding up
     * the heights of the other toasts which are open above this one.
     */
    private getToastTop(toast: ToastComponent): number {
        const index = this.getToastIndex(toast);

        return this.openToasts
            .filter((o: IOpenToast, i: number) => i < index)
            .reduce((top: number, o: IOpenToast) => {
                return top += o.toast.getHeight() + this.verticalMargin;
            }, 0);
    }

    /**
     * Returns the index of the toast object in the openToasts array.
     */
    private getToastIndex(toast: ToastComponent): number {
        return this.openToasts.map((o: IOpenToast) => o.toast).indexOf(toast);
    }
}
