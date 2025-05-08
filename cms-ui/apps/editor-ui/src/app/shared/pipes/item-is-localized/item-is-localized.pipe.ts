import { Pipe, PipeTransform } from '@angular/core';
import { File, Folder, Image, Item, Page } from '@gentics/cms-models';
import { itemIsLocalized } from '../../../common/utils/item-is-localized';

@Pipe({
    name: 'itemIsLocalized',
    standalone: false
})
export class ItemIsLocalizedPipe implements PipeTransform {
    transform(item: Folder | Page | File | Image | Item): boolean {
        return itemIsLocalized(item);
    }
}
