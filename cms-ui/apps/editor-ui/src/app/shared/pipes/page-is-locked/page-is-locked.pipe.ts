import { Pipe, PipeTransform } from '@angular/core';
import { Page } from '@gentics/cms-models';
import { ApplicationStateService } from '../../../state';

/**
 * Returns true if the page is locked by a different user than the current logged in user.
 * If this is the case, the page should not be editable.
 */
@Pipe({
    name: 'pageIsLocked',
    standalone: false
})
export class PageIsLockedPipe implements PipeTransform {

    constructor(private state: ApplicationStateService) { }

    transform(page: Page): boolean {
        const currentUserId = this.state.now.auth.user?.id;
        if (page && page.type === 'page') {
            if (page.locked) {
                const lockedById = typeof page.lockedBy === 'object' ? page.lockedBy.id : page.lockedBy;
                return lockedById !== currentUserId;
            }
        }
        return false;
    }
}
