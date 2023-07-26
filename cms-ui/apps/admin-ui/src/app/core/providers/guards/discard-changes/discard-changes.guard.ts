import { BooleanFn, OnDiscardChanges } from '@admin-ui/common';
import { DiscardChangesModalComponent } from '@admin-ui/core/components/discard-changes-modal';
import { AppStateService, CloseEditor } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ModalService } from '@gentics/ui-core';

/**
 * A route guard checking for changes made by the user in an component with changeable elements.
 */
@Injectable()
export class DiscardChangesGuard {

    constructor(
        private modalService: ModalService,
        private appState: AppStateService,
    ) {}

    async canDeactivate(comp: OnDiscardChanges): Promise<boolean> {
        const userHasEdited = this.evaluateBooleanValue(comp, comp.userHasEdited);
        let doClose = !userHasEdited;

        if (userHasEdited) {
            const modal = await this.modalService.fromComponent(
                DiscardChangesModalComponent,
                { closeOnOverlayClick: false },
                {
                    changesValid: this.evaluateBooleanValue(comp, comp.changesValid),
                    updateEntity: comp.updateEntity.bind(comp),
                    resetEntity: comp.resetEntity.bind(comp),
                },
            );
            doClose = await modal.open();
        }

        // If it will close and the editor is still open, then we need to close it
        // if (doClose && this.appState.now.ui.editorIsOpen) {
        //     this.appState.dispatch(new CloseEditor());
        // }

        return doClose;
    }

    private evaluateBooleanValue(obj: any, valueOrFn: boolean | BooleanFn): boolean {
        if (typeof valueOrFn === 'function') {
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            return valueOrFn.bind(obj)();
        } else {
            return valueOrFn;
        }
    }
}
