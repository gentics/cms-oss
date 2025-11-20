import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'gtxEntries',
    standalone: false,
})
export class EntriesPipe implements PipeTransform {

    // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
    transform(value: any): any {
        if (value == null || typeof value !== 'object') {
            return value;
        }

        return Object.entries(value).map(([key, value]) => ({ key, value }));
    }
}
