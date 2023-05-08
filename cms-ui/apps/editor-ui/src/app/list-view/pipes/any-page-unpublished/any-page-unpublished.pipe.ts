import { Pipe, PipeTransform } from '@angular/core';
import { Page } from '@gentics/cms-models';

/**
 * Pipe that returns true if any of the passed pages (>= 1) is not "published".
 */
@Pipe({
    name: 'anyPageUnpublished'
})
export class AnyPageUnpublishedPipe implements PipeTransform {
    transform(pages: Page[]): boolean {
        if (!pages || !pages.length) {
            return false;
        }

        for (let page of pages) {
            if (!page || page.type !== 'page') {
                return false;
            }

            if (page.online !== true) {
                return true;
            }
        }

        return false;
    }
}
