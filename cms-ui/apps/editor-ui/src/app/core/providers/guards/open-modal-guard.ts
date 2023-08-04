import {Injectable, Type} from '@angular/core';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import {ModalService} from '@gentics/ui-core';

/**
 * This guard prevents navigation if there are modals open which should not allow navigation to happen
 * beneath them.
 */
@Injectable()
export class OpenModalGuard  {

    constructor(private modalService: ModalService) {}

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
        return this.modalService.openModals.length === 0;
    }

    canActivateChild(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
        return this.modalService.openModals.length === 0;
    }

    canDeactivate(component: Type<any>, route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
        return this.modalService.openModals.length === 0;
    }
}
