import { Pipe, PipeTransform } from '@angular/core';
import { File, Image, Page } from '@gentics/cms-models';

/**
 * Returns the path of an item, stripping out the rootName if supplied.
 */
@Pipe({
    name: 'itemPath',
    standalone: false
})
export class ItemPathPipe implements PipeTransform {

    transform(item: Page | File | Image): string {
        return item.publishPath;
    }
}
