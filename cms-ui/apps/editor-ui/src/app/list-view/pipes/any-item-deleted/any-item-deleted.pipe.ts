import { Pipe, PipeTransform } from '@angular/core';
import { Item } from '@gentics/cms-models';
import { EntityStateUtil } from '../../../shared/util/entity-states';

/**
 * Pipe that returns true if any of the passed items (>= 1) is "deleted".
 */
@Pipe({
    name: 'anyItemDeleted',
})
export class AnyItemDeletedPipe implements PipeTransform {
    transform(items: Item[]): boolean {
        if (!items || !items.length) {
            return false;
        }

        return items.some(item => EntityStateUtil.stateDeleted(item));
    }
}
