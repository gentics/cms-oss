import { DiscardChangesModalComponent } from '@admin-ui/core/components/discard-changes-modal';
import { AppStateService, CloseEditor } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { ModalService } from '@gentics/ui-core';

type BooleanFn = () => boolean;

/** Interface necessary to implement for the guarded component */
export interface OnDiscardChanges {
    /** Returns TRUE if user has changed something */
    userHasEdited: boolean | BooleanFn;
    /** Returns TRUE if the changes are valid */
    changesValid: boolean | BooleanFn;
    /** Update entity data of invoking component */
    updateEntity: () => Promise<void>;
    /** Reset entity data of invoking component */
    resetEntity: () => Promise<void>;
}

/**
 * A route guard checking for changes made by the user in an component with changeable elements.
 */
@Injectable()
export class DiscardChangesGuard<T extends OnDiscardChanges>  {
    constructor(
        private modalService: ModalService,
        private appState: AppStateService,
    ) {}

    async canDeactivate(comp: T, route: ActivatedRouteSnapshot, state: RouterStateSnapshot, nextState: RouterStateSnapshot): Promise<boolean> {
        const userHasEdited = this.evaluateBooleanValue(comp, comp.userHasEdited);
        let doClose = !userHasEdited;

        if (userHasEdited) {
            doClose = await this.modalService.fromComponent(
                DiscardChangesModalComponent,
                { closeOnOverlayClick: false },
                {
                    changesValid: this.evaluateBooleanValue(comp, comp.changesValid),
                    updateEntity: comp.updateEntity.bind(comp),
                    resetEntity: comp.resetEntity.bind(comp),
                },
            )
            .then(modal => modal.open())
            .then((canDeactivate: boolean) => canDeactivate);
        }

        // If it will close and the editor is still open, then we need to close it
        if (doClose && this.appState.select(s => s.ui.editorIsOpen)) {
            this.appState.dispatch(new CloseEditor());
        }

        return doClose;
    }

    private evaluateBooleanValue(obj: any, valueOrFn: boolean | BooleanFn): boolean {
        if (typeof valueOrFn === 'function') {
            return valueOrFn.bind(obj)();
        } else {
            return valueOrFn;
        }
    }
}
