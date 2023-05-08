import { ChangeDetectionStrategy, Component } from '@angular/core';
import { BaseModal, IModalOptions, ModalService } from '@gentics/ui-core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    selector: 'my-modal-component',
    template: `
        <div class="modal-title">
            <h4>A Custom Component</h4>
        </div>
        <div class="modal-content">
            <h5>{{ greeting }}</h5>
        </div>
        <div class="modal-footer">
            <a (click)="closeFn('link was clicked')">Close me</a>
        </div>`,
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyModalComponent extends BaseModal<string> {
    greeting: string;

    constructor() {
        super();
        console.log('constructor()', this.greeting);
    }
}

@Component({
    templateUrl: './modal-service-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ModalServiceDemoPage {

    @InjectDocumentation('modal.service')
    documentation: IDocumentation;

    padding = true;
    width = '400px';
    closeOnOverlayClick = true;
    closeOnEscape = true;

    constructor(public modalService: ModalService) {}

    showBasicDialog(): void {
        this.modalService.dialog({
            title: 'A Basic Dialog',
            body: 'Are you <strong>sure</strong> you want to do the thing?',
            buttons: [
                { label: 'Cancel', type: 'secondary', flat: true, returnValue: false, shouldReject: true },
                { label: 'Okay', type: 'default', returnValue: true },
            ],
        })
            .then(dialog => dialog.open())
            .then(result => console.log('result:', result))
            .catch(reason => console.log('rejected', reason));
    }

    showDialogWithOptions(): void {
        const options: IModalOptions = {
            onOpen: (): void => console.log('Modal was opened.'),
            onClose: (): void => console.log('Modal was closed.'),
            padding: this.padding,
            width: this.width,
            closeOnOverlayClick: this.closeOnOverlayClick,
            closeOnEscape: this.closeOnEscape,
        };

        this.modalService.dialog({
            title: 'Another Dialog',
            body: 'Are you <strong>sure</strong> you want to do the thing?',
            buttons: [
                { label: 'Cancel', type: 'secondary', flat: true, returnValue: false, shouldReject: true },
                { label: 'Okay', type: 'default', returnValue: true },
            ],
        }, options)
            .then(dialog => dialog.open())
            .then(result => console.log('result:', result))
            .catch(reason => console.log('rejected', reason));
    }

    showCustomModal(): void {
        this.modalService.fromComponent(MyModalComponent, {}, { greeting: 'Hello!' })
            .then(modal => modal.open())
            .then(result => console.log('result:', result))
            .catch(reason => console.log('rejected', reason));
    }
}
