import { Injectable } from '@angular/core';
import { DynamicFormModal } from '@editor-ui/app/shared/components/dynamic-form-modal/dynamic-form-modal.component';
import {
    DynamicDropdownConfiguration,
    DynamicFormModalConfiguration,
    OverlayElementControl,
} from '@gentics/cms-integration-api-models';
import { ModalClosingReason, ModalService, OverlayHostService } from '@gentics/ui-core';
import { AlohaIntegrationService } from '../aloha-integration/aloha-integration.service';
import { DynamicDropdownComponent } from '../../components';

@Injectable()
export class DynamicOverlayService {

    constructor(
        protected aloha: AlohaIntegrationService,
        protected modals: ModalService,
        protected overlayHost: OverlayHostService,
    ) {}

    public async openDynamicDropdown<T>(configuration: DynamicDropdownConfiguration<T>, slot?: string): Promise<OverlayElementControl<T>> {
        const host = await this.overlayHost.getHostView();
        let dropdownRef = host.createComponent(DynamicDropdownComponent);
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

        return {
            close: () => {
                if (dropdownRef != null) {
                    // Rest will be handled by the on-destroy handler above
                    dropdownRef.destroy();
                }
            },
            isOpen: () => open,
            value: new Promise((resolve, reject) => {
                instance.registerCloseFn(resolve);
                instance.registerErrorFn(reject);
            }),
        };
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
            return {
                close: () => {
                    if (open && ref?.instance) {
                        ref.instance.cancelFn(null, ModalClosingReason.API);
                    }
                },
                isOpen: () => open,
                value: ref.open(),
            }
        });
    }
}
