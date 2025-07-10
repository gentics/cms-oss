import { ObservableStopper } from '@admin-ui/common';
import {
    ChangeDetectionStrategy,
    Component,
    ComponentFactoryResolver,
    OnDestroy,
    OnInit,
    Type,
    ViewChild,
    ViewContainerRef,
} from '@angular/core';
import { IModalDialog } from '@gentics/ui-core';
import { takeUntil } from 'rxjs/operators';
import { Wizard } from '../../providers/wizard/wizard.service';

@Component({
    selector: 'gtx-wizard-modal',
    templateUrl: './wizard-modal.component.html',
    styleUrls: ['./wizard-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class WizardModalComponent<T extends Wizard<R>, R> implements OnInit, OnDestroy, IModalDialog {

    /**
     * The type of component that contains the `gtx-wizard`.
     *
     * This must be set when instantiating the `WizardModalComponent`
     * using `ModalService.fromComponent()`.
     */
    wizardType: Type<T>;

    /**
     * Properties to be assigned to the wizard host instance.
     */
    wizardHostProperties: Partial<T>;

    @ViewChild('wizardContainer', { read: ViewContainerRef, static: true })
    wizardContainer: ViewContainerRef;

    closeFn: (val: R) => void;
    cancelFn: (val?: any) => void;

    private stopper = new ObservableStopper();

    constructor(
        private componentFactoryResolver: ComponentFactoryResolver,
    ) { }

    ngOnInit(): void {
        const componentFactory = this.componentFactoryResolver.resolveComponentFactory(this.wizardType);
        const wizardHost = this.wizardContainer.createComponent(componentFactory);
        this.applyProperties(wizardHost.instance);

        // Force a change detection to trigger ngOnInit(), etc in the wizardHost.
        wizardHost.changeDetectorRef.detectChanges();

        wizardHost.instance.wizard.wizardFinish.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe(output => this.closeFn(output));

        wizardHost.instance.wizard.wizardCancel.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe(() => this.cancelFn());
    }

    ngOnDestroy(): void {
        this.stopper.stop();
    }

    registerCloseFn(close: (val: R) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val: any) => void): void {
        this.cancelFn = cancel;
    }

    private applyProperties(wizardHost: T): void {
        if (this.wizardHostProperties) {
            Object.keys(this.wizardHostProperties).forEach(key => {
                wizardHost[key] = this.wizardHostProperties[key];
            });
        }
    }

}
