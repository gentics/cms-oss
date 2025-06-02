import { Pipe, PipeTransform } from '@angular/core';
import { File, Folder, Image, Item, Page } from '@gentics/cms-models';
import { itemIsLocal } from '../../../common/utils/item-is-local';

@Pipe({
    name: 'itemIsLocal',
    standalone: false
})
export class ItemIsLocalPipe implements PipeTransform {
    transform(item: Folder | Page | File | Image | Item): boolean {
        return itemIsLocal(item);
    }
}
