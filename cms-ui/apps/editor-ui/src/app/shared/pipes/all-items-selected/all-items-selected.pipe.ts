import { Pipe, PipeTransform } from '@angular/core';
import { Item } from '@gentics/cms-models';

@Pipe({
    name: 'allItemsSelected',
    standalone: false
})
export class AllItemsSelectedPipe implements PipeTransform {
    transform(allItems: Item[], selectedItems: Item[]): boolean {
        if (!allItems || !selectedItems || allItems.length === 0) {
            return false;
        } else {
            return allItems.length === selectedItems.length;
        }
    }
}
