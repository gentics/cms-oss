import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanDeactivate, RouterStateSnapshot, ParamMap} from '@angular/router';
import {ModalService} from '@gentics/ui-core';
import {ContentFrame} from '../../components/content-frame/content-frame.component';
import {ConfirmNavigationModal} from '../../components/confirm-navigation-modal/confirm-navigation-modal.component';
import { EditorStateUrlOptions } from '../../../state';

/**
 * This guard prevents navigating away from the current ContentFrame route if some content is modified.
 */
@Injectable()
export class ContentFrameGuard implements CanDeactivate<ContentFrame> {

    constructor(private modalService: ModalService) {}

    canDeactivate(contentFrame: ContentFrame, route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean> {
        if (!(contentFrame.contentModified || contentFrame.objectPropertyModified)) {
            return Promise.resolve(true);
        }

        // extract readOnly parameter
        const params: ParamMap = route.paramMap;
        const urlOptions = params.get('options');
        const urlOptionsDecoded: EditorStateUrlOptions = JSON.parse(atob(urlOptions));
        const readOnly = urlOptionsDecoded.readOnly;

        const options = {
            closeOnOverlayClick: false,
        };

        const allowSaving = (!contentFrame.objectPropertyModified || contentFrame.modifiedObjectPropertyValid)
            && !readOnly
            && (!contentFrame.currentItem || contentFrame.currentItem.type !== 'form' || contentFrame.itemValid);

        return this.modalService.fromComponent(
            ConfirmNavigationModal,
            options,
            { contentFrame, allowSaving },
        ).then(modal => modal.open());
    }
}
