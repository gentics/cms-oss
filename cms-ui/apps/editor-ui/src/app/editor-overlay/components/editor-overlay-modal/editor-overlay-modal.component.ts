import { Injectable, OnInit } from '@angular/core';
import { GcmsUiLanguage } from '@gentics/cms-integration-api-models';
import { File as FileModel, Folder, Image, Node, Page } from '@gentics/cms-models';
import { IModalDialog, ModalService } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { ApplicationStateService } from '../../../state';
import { ConfirmCloseModal } from '../confirm-close-modal/confirm-close-modal.component';

/**
 * This is a base class for EditorOverlay type modals. The base class provides an
 * uiLanguage Observer, which can be used in modal to change external components
 * translations. Also the base class guards the modal from cancel if some editing tasks
 * are made in it. To use this feature, you need to update the isModified property of the
 * child class.
 *
 * For the guard, there is a saveAndClose() abstract, which need to be implemented to allow
 * user to save the current state of editing and close the modal.
 *
 * Example:
 *
 * ```TypeScript
 * @Component({
 *   selector: 'example-editor-modal',
 *   templateUrl: './example-editor-modal.component.html',
 *   styleUrls: ['./example-editor-modal.component.scss']
 * })
 * export class ExampleEditorModalComponent extends EditorOverlayModal {
 *     params: Array<string>;
 *
 *     saveAndClose(): void {
 *         this.closeFn(this.params);
 *     }
 * }
 * ```
 *
 * For more modal informations see examples at https://gentics.github.io/@gentics/ui-core/#/modal-service
 *
 */
@Injectable()
export abstract class EditorOverlayModal implements OnInit, IModalDialog {
    closeFn: (val: any) => void;
    cancelFn: (val?: any) => void;
    uiLanguage$: Observable<GcmsUiLanguage>;

    protected isModified: boolean;

    /** The item that is currently being edited. */
    abstract get currentItem(): Page | FileModel | Folder | Image | Node;

    /** Saves the changes and closes the modal. */
    abstract saveAndClose(): void;

    constructor(
        protected appState: ApplicationStateService,
        protected modalService: ModalService
    ) { }

    ngOnInit(): void {
        this.uiLanguage$ = this.appState.select(state => state.ui.language);
        const originalCancelFn = this.cancelFn;
        const cancelFn = (val?: any) => {
            if (this.isModified) {
                this.canCancel(this.isModified)
                    .then((status) => {
                        switch (status) {
                            case 'save':
                                this.saveAndClose();
                                break;
                            case true:
                                this.cancel(originalCancelFn);
                                break;
                            default:
                        }
                    });
            } else {
                this.cancel(originalCancelFn);
            }

        };
        this.registerCancelFn(cancelFn);
    }

    canCancel(state: boolean): Promise<ConfirmCloseResult> {
        if (state) {
            const options = {
                closeOnOverlayClick: false
            };

            return this.modalService.fromComponent(ConfirmCloseModal, options, { currentModal: this })
                .then(modal => modal.open() as Promise<ConfirmCloseResult>);
        }
        return Promise.resolve(true);
    }

    registerCloseFn(close: (val: any) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val: any) => void): void {
        this.cancelFn = cancel;
    }

    cancel(cancel: (val: any) => void): void {
        this.registerCancelFn(cancel);
        this.cancelFn();
    }
}
