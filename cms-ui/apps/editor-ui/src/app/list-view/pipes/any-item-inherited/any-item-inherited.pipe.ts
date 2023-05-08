import { Pipe, PipeTransform } from '@angular/core';
import { InheritableItem } from '@gentics/cms-models';

/**
 * Pipe that returns true if any of the passed items (>= 1) is "inherited".
 */
@Pipe({
    name: 'anyItemInherited'
})
export class AnyItemInheritedPipe implements PipeTransform {
    transform(items: InheritableItem[]): boolean {
        if (!items || !items.length) {
            return false;
        }

        for (let item of items) {
            if (!item) {
                return false;
            }

            if (item.inherited === true) {
                return true;
            }
        }

        return false;
    }
}
