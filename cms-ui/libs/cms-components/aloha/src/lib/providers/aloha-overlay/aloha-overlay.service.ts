import { ComponentRef, Injectable } from '@angular/core';
import {
    DynamicDialogConfiguration,
    DynamicDropdownConfiguration,
    DynamicFormModalConfiguration,
    OverlayElementControl,
} from '@gentics/aloha-models';
import { ModalCloseError, ModalClosingReason } from '@gentics/cms-integration-api-models';
import { ModalService, OverlayHostService } from '@gentics/ui-core';
import { DynamicDropdownComponent } from '../../components/dynamic-dropdown/dynamic-dropdown.component';
import { DynamicFormModal } from '../../components/dynamic-form-modal/dynamic-form-modal.component';
import { AlohaIntegrationService } from '../aloha-integration/aloha-integration.service';

interface ExtendedOverlayElementControl<T> extends OverlayElementControl<T> {
    onClose: Promise<void>;
}

@Injectable()
export class AlohaOverlayService {

    private openOverlays: OverlayElementControl<any>[] = [];

    constructor(
        protected aloha: AlohaIntegrationService,
        protected modals: ModalService,
        protected overlayHost: OverlayHostService,
    ) {}

    public closeRemaining(): void {
        this.openOverlays.forEach((ctl) => ctl.close());
        this.openOverlays = [];
    }

    public async openDialog<T>(config: DynamicDialogConfiguration<T>): Promise<ExtendedOverlayElementControl<T>> {
        const dialog = await this.modals.dialog(config);

        let open = true;
        const { promise: closePromise, resolve: closeResolve } = Promise.withResolvers<void>();

        const ctl: ExtendedOverlayElementControl<T> = {
            isOpen: () => open,
            close: () => {
                dialog?.instance?.cancelFn(new ModalCloseError(ModalClosingReason.API));
                open = false;
                closeResolve();
            },
            value: dialog.open()
                .finally(() => {
                    open = false;
                    closeResolve();
                }),
            onClose: closePromise,
        };

        this.openOverlays.push(ctl);

        return ctl;
    }

    public async openDynamicDropdown<T>(configuration: DynamicDropdownConfiguration<T>, slot?: string): Promise<ExtendedOverlayElementControl<T>> {
        const host = await this.overlayHost.getHostView();
        let dropdownRef: ComponentRef<DynamicDropdownComponent<T>> = host.createComponent(DynamicDropdownComponent) as any;
        const instance = dropdownRef.instance;
        let open = true;
        const { promise: closePromise, resolve: closeResolve } = Promise.withResolvers<void>();

        dropdownRef.onDestroy(() => {
            open = false;
            dropdownRef = null;
        });

        if (slot) {
            this.positionDropdown(slot, dropdownRef.location.nativeElement);
        }

        // Apply input parameters and initialize the component
        instance.configuration = configuration;
        dropdownRef.changeDetectorRef.markForCheck();

        const closeDropdown = () => {
            if (dropdownRef != null) {
                // Rest will be handled by the on-destroy handler above
                dropdownRef.destroy();
            }
            this.aloha.restoreSelection(true);
            closeResolve();
        };

        const ctl: ExtendedOverlayElementControl<T> = {
            close: () => closeDropdown(),
            isOpen: () => open,
            value: new Promise((resolve, reject) => {
                instance.registerCloseFn((value) => {
                    closeDropdown();
                    resolve(value);
                });
                instance.registerErrorFn((error) => {
                    closeDropdown();
                    reject(error);
                });
            }),
            onClose: closePromise,
        };

        this.openOverlays.push(ctl);

        return ctl;
    }

    protected positionDropdown(slot: string, dropdownElement: HTMLElement): void {
        const comp = this.aloha.renderedComponents[slot];
        // target element doesn't exist
        if (comp == null || comp.element == null || comp.element.nativeElement == null) {
            return;
        }

        const rect = comp.element.nativeElement.getBoundingClientRect();

        dropdownElement.style.setProperty('--target-width', `${rect.width}px`);
        dropdownElement.style.setProperty('--target-height', `${rect.height}px`);
        dropdownElement.style.setProperty('--target-x', `${rect.x}px`);
        dropdownElement.style.setProperty('--target-y', `${rect.y}px`);
    }

    public openDynamicModal<T>(configuration: DynamicFormModalConfiguration<T>): Promise<ExtendedOverlayElementControl<T>> {
        let open = true;
        const { promise: closePromise, resolve: closeResolve } = Promise.withResolvers<void>();

        return this.modals.fromComponent(DynamicFormModal, {
            closeOnEscape: configuration.closeOnEscape,
            closeOnOverlayClick: configuration.closeOnOverlayClick,
            onClose: () => {
                open = false;
                this.aloha.restoreSelection(true);
                closeResolve();
            },
        }, {
            configuration,
        } as any).then((ref) => {
            const ctl: ExtendedOverlayElementControl<T> = {
                close: () => {
                    if (open && ref?.instance) {
                        ref.instance.cancelFn(null, ModalClosingReason.API);
                    }
                },
                isOpen: () => open,
                value: ref.open(),
                onClose: closePromise,
            };

            this.openOverlays.push(ctl);

            return ctl;
        }).catch((err) => {
            if (err instanceof ModalCloseError) {
                this.aloha.restoreSelection();
            }
            throw err;
        });
    }
}
