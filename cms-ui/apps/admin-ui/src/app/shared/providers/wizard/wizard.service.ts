import { Injectable, Type } from '@angular/core';
import { IModalInstance, ModalService } from '@gentics/ui-core';
import { WizardModalComponent } from '../../components/wizard-modal/wizard-modal.component';
import { WizardComponent } from '../../components/wizard/wizard.component';
import { ServiceBase } from '../service-base/service.base';

/**
 * This interface needs to be implemented by a component that defines a wizard using a `gtx-wizard`.
 *
 * @param O The output type of this wizard, i.e., the type that the promise of the last step's
 * `WizardStepNextClickFn` resolves to.
 */
export interface Wizard<O> {

    /** Gets the `gtx-wizard` instance that is used by this wizard. */
    wizard: WizardComponent;

}

/**
 * Used for showing wizards in a modal.
 */
@Injectable()
export class WizardService extends ServiceBase {

    constructor(
        private modalService: ModalService,
    ) {
        super();
    }

    /**
     * Shows a wizard modal using the specified component.
     *
     * @returns A Promise that resolves to the output of the last wizard step's `WizardStepNextClickFn`.
     */
    showWizard<T extends Wizard<R>, R>(component: Type<T>, properties?: Partial<T>): Promise<R> {
        return this.modalService.fromComponent(
            WizardModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
            { wizardType: component, wizardHostProperties: properties },
        ).then((modal: IModalInstance<WizardModalComponent<T, R>>) => modal.open());
    }

}
