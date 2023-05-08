import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'entries',
})
export class EntriesPipe implements PipeTransform {
    transform(value: any): any {
        if (value == null || typeof value !== 'object') {
            return value;
        }

        return Object.entries(value).map(([key, value]) => ({ key, value }));
    }
}
