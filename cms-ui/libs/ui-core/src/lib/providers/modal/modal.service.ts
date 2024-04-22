import {
    ComponentRef,
    ElementRef,
    Injectable,
    Optional,
    SkipSelf,
    Type,
    ViewContainerRef,
} from '@angular/core';
import { ModalCloseError, ModalClosingReason } from '@gentics/cms-integration-api-models';
import { IDialogConfig, IModalDialog, IModalInstance, IModalOptions } from '../../common/modal';
import { BlankModal } from '../../components/blank-modal/blank-modal.component';
import { DynamicModal } from '../../components/dynamic-modal/dynamic-modal.component';
import { ModalDialogComponent } from '../../components/modal-dialog/modal-dialog.component';
import { OverlayHostService } from '../../providers/overlay-host/overlay-host.service';

/**
 * A promise-based service for creating modal windows and dialogs.
 * Depends on the [`<gtx-overlay-host>`](#/overlay-host) being present in the app.
 *
 * ## Return Values
 * All the public methods return the `IModalInstance` object:
 *
 * ```
 * interface IModalInstance {
 *     instance: IModalDialog;
 *     element: HTMLElement;
 *     open: () => Promise<any>;
 * }
 * ```
 * Calling the `open()` method returns a promise which will be resolved when the modal is closed
 * or rejected when a button is set to `shouldReject` or the modal calls the passed error handler.
 *
 * ## `.dialog()`
 * To create a basic dialog modal, use the `.dialog()` method. This accepts an `IDialogConfig` object:
 *
 * ```TypeScript
 * interface IDialogConfig {
 *   title: string;
 *   body?: string;
 *   buttons: {
 *       label: string;
 *       type?: 'default' | 'secondary' | 'success'| 'warning' | 'alert';
 *       flat?: boolean;
 *       // If specified, will be returned as the
 *       // value of the resolved promise (or the reason if rejected).
 *       returnValue?: any;
 *       // If true, clicking the button will cause
 *       // the promise to reject rather than resolve
 *       shouldReject?: boolean;
 * }[];
 * ```
 *
 * Example:
 * ```TypeScript
 * modalService.dialog({
 *   title: 'Are you sure?',
 *   body: 'Do you <em>really</em> want to delete this thing?',
 *   buttons: [
 *     { label: 'Delete', type: 'alert', returnValue: true },
 *     { label: 'Cancel', type: 'secondary', shouldReject: true }
 *   ]
 * }).then(modal => modal.open())
 *   .then(result => console.log('deleting...'))
 *   .catch(() => console.log('cancelled');
 * ```
 *
 * ## `.fromComponent()`
 * For more complex modals, a component can be passed to the `.fromComponent()` method which will then be
 * placed inside a modal window. The component must implement the IModalDialog interface, which allows the
 * ModalService to hook into a `closeFn` & `cancelFn` so it knows to close the modal and resolve the promise.
 * To forward errors from the modal to the caller, implement `registerErrorFn` from the IModalDialog interface.
 *
 * Example:
 * ```TypeScript
 * @Component({
 *   selector: 'my-modal-form',
 *   template: '...' // some big form
 * })
 * export class MyModalForm implements IModalDialog {
 *   // IModalDialog interface members
 *   closeFn: (val: any) => void;
 *   cancelFn: (val?: any) => void;
 *   registerCloseFn(close: (val: any) => void): void {
 *       this.closeFn = close;
 *   }
 *   registerCancelFn(cancel: (val?: any) => void): void {
 *       this.cancelFn = cancel;
 *   }
 *
 *   someLocalVariable: string;
 *
 *   // Bound to the form's submit event.
 *   onSubmitClick() : void {
 *      this.closeFn(this.form.value);
 *   }
 *
 *   // Bound to the "cancel" button in the template
 *   onCancelClick(): void {
 *      this.cancelFn();
 *   }
 * }
 * ```
 * The above component could then be used as follows:
 * ```TypeScript
 * modalService.fromComponent(MyModalForm, {}, { someLocalVariable: 'foo' })
 *   .then(modal => modal.open())
 *   .then(result => console.log(result));
 * ```
 *
 * ## Modal Options
 * All public methods take an optional options parameter to describe the behavior and appearance of the modal window
 * itself:
 * ```TypeScript
 * interface IModalOptions {
 *     onOpen?: Function;
 *     onClose?: Function;
 *     closeOnOverlayClick?: boolean;
 *     closeOnEscape?: boolean;
 *     width?: string;
 *     padding?: boolean;
 *     modalBodyClass?: string;
 * }
 * ```
 */
@Injectable()
export class ModalService {

    private openModalComponents: ComponentRef<IModalDialog>[] = [];
    private getHostViewContainer: () => Promise<ViewContainerRef>;

    /**
     * Returns an array of ComponentRefs for each currently-opened modal.
     */
    public get openModals(): ComponentRef<IModalDialog>[] {
        return this.parentService ? this.parentService.openModals : this.openModalComponents;
    }

    constructor(
        overlayHostService: OverlayHostService,
        @Optional()
        @SkipSelf()
        private parentService: ModalService = null,
    ) {
        this.getHostViewContainer = () => overlayHostService.getHostView();
    }

    /**
     * Create a new modal instance containing the specified component, optionally specifying "locals" which
     * will be defined on the component instance with the given value.
     */
    public fromComponent<T extends IModalDialog>(
        component: Type<T>,
        options?: IModalOptions,
        locals?: { [K in keyof T]?: T[K] },
    ): Promise<IModalInstance<T>> {
        const modal = this.wrapComponentInModal(component, options, locals);
        return Promise.resolve(modal);
    }

    /**
     * Create a new modal by appending the elementRef to a blank modal window. Primarily used internally
     * for the implementation of the declarative [Modal](#/modal) component.
     */
    public fromElement(elementRef: ElementRef, options?: IModalOptions): Promise<IModalInstance<BlankModal>> {
        return this.wrapComponentInModal(BlankModal, options)
            .then(modal => {
                modal.element.appendChild(elementRef.nativeElement);
                return modal;
            });
    }

    /**
     * Creates and displays a standard modal dialog.
     */
    public dialog(config: IDialogConfig, options?: IModalOptions): Promise<IModalInstance<ModalDialogComponent>> {
        return this.wrapComponentInModal(ModalDialogComponent, options)
            .then(modal => {
                modal.instance.setConfig(config);
                return modal;
            });
    }

    private wrapComponentInModal<T extends IModalDialog>(
        component: Type<T>,
        options?: IModalOptions,
        locals?: { [key: string]: any },
    ): Promise<IModalInstance<T>> {
        return this.createModalWrapper<T>(options)
            .then(modalWrapper => {
                const componentRef = modalWrapper.injectContent(component);
                const dialog = componentRef.instance;
                if (locals !== undefined) {
                    // eslint-disable-next-line guard-for-in
                    for (const key in locals) {
                        (<any>dialog)[key] = locals[key];
                    }
                    componentRef.changeDetectorRef.markForCheck();
                }
                this.checkModalDialogInterface(dialog);
                return {
                    instance: dialog as any,
                    element: componentRef.location.nativeElement,
                    open: (): Promise<any> => {
                        this.invokeOnOpenCallback(options);
                        this.openModals.push(componentRef);
                        componentRef.onDestroy(() => {
                            const index = this.openModals.indexOf(componentRef);
                            if (-1 < index) {
                                this.openModals.splice(index, 1);
                            }
                        });
                        modalWrapper.open();
                        return this.createPromiseFromDialog(modalWrapper, dialog);
                    },
                };
            });
    }

    /**
     * Ensure that the component passed in implements IModalDialog.
     */
    private checkModalDialogInterface(dialog: IModalDialog): void {
        const conforms = typeof dialog.registerCancelFn === 'function' && typeof dialog.registerCloseFn === 'function';
        if (!conforms) {
            throw new Error('ModalService#wrapComponentInModal(): Component must implement IModalDialog.');
        }
    }

    /**
     * Creates the DynamicModalWrapper in place in the DOM and returns a reference to the
     * created component.
     */
    private createModalWrapper<T extends IModalDialog>(options?: IModalOptions): Promise<DynamicModal> {
        return this.getHostViewContainer()
            .then(hostViewContainer => {
                if (!hostViewContainer) {
                    throw new Error('No OverlayHost present, add a <gtx-overlay-host> element!');
                }
                const ref = hostViewContainer.createComponent(DynamicModal);
                return this.getConfiguredModalWrapper(ref, options);
            });
    }

    /**
     * Decorate the ModalWrapper instance with the dismissFn and return that instance.
     */
    private getConfiguredModalWrapper<T extends IModalDialog>(wrapperComponentRef: ComponentRef<DynamicModal>,
        options?: IModalOptions): DynamicModal {
        const modalWrapper = wrapperComponentRef.instance;
        modalWrapper.dismissFn = (reason) => {
            this.invokeOnCloseCallback(options, reason);
            wrapperComponentRef.destroy();
        };
        modalWrapper.setOptions(options);
        return modalWrapper;
    }

    /**
     * Returns a promise which is bound to the closeFn and cancelFn of the dialog instance,
     * and will be resolved/rejected when either of those methods are invoked.
     */
    private createPromiseFromDialog<T extends IModalDialog>(modalWrapper: DynamicModal, dialog: IModalDialog): Promise<any> {
        return new Promise((resolve, reject) => {
            dialog.registerCloseFn((value: any) => {
                modalWrapper.dismissFn();
                resolve(value);
            });

            dialog.registerCancelFn((val, reason) => {
                modalWrapper.dismissFn(reason ?? ModalClosingReason.CANCEL);
                if (reason) {
                    reject(new ModalCloseError(reason));
                }
            });

            if (dialog.registerErrorFn) {
                dialog.registerErrorFn((err: Error) => {
                    modalWrapper.dismissFn(ModalClosingReason.ERROR);
                    reject(new ModalCloseError(err));
                });
            }
        });
    }

    private invokeOnOpenCallback(options: IModalOptions): void {
        if (options && options.onOpen && typeof options.onOpen === 'function') {
            options.onOpen();
        }
    }

    private invokeOnCloseCallback(options: IModalOptions, reason?: ModalClosingReason): void {
        if (options && options.onClose && typeof options.onClose === 'function') {
            options.onClose(reason);
        }
    }
}
