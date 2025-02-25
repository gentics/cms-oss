import { Pipe, PipeTransform } from '@angular/core';
import { PAGES } from '../../common/page-list';

@Pipe({
    name: 'gtxLinkToPage',
})
export class LinkToPagePipe implements PipeTransform {

    transform(id: string): string[] {
        const page = PAGES[id];
        return page ? [`/${page.path}`] : [];
    }
}
