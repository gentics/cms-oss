import { Pipe, PipeTransform } from '@angular/core';

interface Entry<T> {
    key: string;
    value: T;
}

@Pipe({
    name: 'gtxEntries',
    standalone: false,
})
export class EntriesPipe implements PipeTransform {

    transform<T>(value: Record<string, T> | null | undefined): Entry<T>[] {
        if (value == null || typeof value !== 'object') {
            return [];
        }

        return Object.entries(value).map(([key, value]) => ({ key, value }));
    }
}
