import { Pipe, PipeTransform } from '@angular/core';
import { Form, Page } from '@gentics/cms-models';
import { PublishableStateUtil } from '../../../shared/util/entity-states';

/**
 * Pipe that returns true if any of the passed pages (>= 1) is "published".
 */
@Pipe({
    name: 'anyItemPublished',
})
export class AnyItemPublishedPipe implements PipeTransform {
    transform(items: (Page | Form)[]): boolean {
        if (!items || !items.length) {
            return false;
        }

        for (let item of items) {
            if (!item || (item.type !== 'page' && item.type !== 'form')) {
                return false;
            }

            if (PublishableStateUtil.statePublished(item)) {
                return true;
            }
        }

        return false;
    }
}
