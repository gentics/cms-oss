import { ComponentRef, Injectable } from '@angular/core';
import {
    DynamicDialogConfiguration,
    DynamicDropdownConfiguration,
    DynamicFormModalConfiguration,
    OverlayElementControl,
} from '@gentics/aloha-models';
import { ModalCloseError, ModalClosingReason } from '@gentics/cms-integration-api-models';
import { ModalService, OverlayHostService } from '@gentics/ui-core';
import { DynamicDropdownComponent } from '../../components';
import { DynamicFormModal } from '../../components/dynamic-form-modal/dynamic-form-modal.component';
import { AlohaIntegrationService } from '../aloha-integration/aloha-integration.service';

@Injectable()
export class DynamicOverlayService {

    private openOverlays: OverlayElementControl<any>[] = [];

    constructor(
        protected aloha: AlohaIntegrationService,
        protected modals: ModalService,
        protected overlayHost: OverlayHostService,
    ) {}

    public closeRemaining(): void {
        this.openOverlays.forEach(ctl => ctl.close());
        this.openOverlays = [];
    }

    public async openDialog<T>(config: DynamicDialogConfiguration<T>): Promise<OverlayElementControl<T>> {
        const dialog = await this.modals.dialog(config);

        let open = true;

        const ctl: OverlayElementControl<T> = {
            isOpen: () => open,
            close: () => {
                dialog?.instance?.cancelFn(new ModalCloseError(ModalClosingReason.API));
                open = false;
            },
            value: dialog.open()
                .catch(err => {
                    if (err instanceof ModalCloseError && err.reason !== ModalClosingReason.ERROR) {
                        return;
                    }
                    throw err;
                })
                .finally(() => {
                    open = false;
                }),
        };

        this.openOverlays.push(ctl);

        return ctl;
    }

    public async openDynamicDropdown<T>(configuration: DynamicDropdownConfiguration<T>, slot?: string): Promise<OverlayElementControl<T>> {
        const host = await this.overlayHost.getHostView();
        let dropdownRef: ComponentRef<DynamicDropdownComponent<T>> = host.createComponent(DynamicDropdownComponent) as any;
        const instance = dropdownRef.instance;
        let open = true;

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
        };

        const ctl: OverlayElementControl<T> = {
            close: () => closeDropdown,
            isOpen: () => open,
            value: new Promise((resolve, reject) => {
                instance.registerCloseFn(value => {
                    closeDropdown();
                    resolve(value);
                });
                instance.registerErrorFn(error => {
                    closeDropdown();
                    if (error instanceof ModalCloseError && error.reason !== ModalClosingReason.ERROR) {
                        return;
                    }
                    reject(error);
                });
            }),
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

    public openDynamicModal<T>(configuration: DynamicFormModalConfiguration<T>): Promise<OverlayElementControl<T>> {
        let open = true;

        return this.modals.fromComponent(DynamicFormModal, {
            closeOnEscape: configuration.closeOnEscape,
            closeOnOverlayClick: configuration.closeOnOverlayClick,
            onClose: () => {
                open = false;
            },
        }, {
            configuration,
        } as any).then(ref => {
            const ctl: OverlayElementControl<T> = {
                close: () => {
                    if (open && ref?.instance) {
                        ref.instance.cancelFn(null, ModalClosingReason.API);
                    }
                },
                isOpen: () => open,
                value: ref.open(),
            };

            this.openOverlays.push(ctl);

            return ctl;
        });
    }
}
