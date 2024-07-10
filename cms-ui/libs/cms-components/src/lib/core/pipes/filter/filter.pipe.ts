import { Pipe, PipeTransform } from '@angular/core';

export type FilterFn = (value: any) => boolean;

const DEFAULT_FILTER: FilterFn = (v) => v != null && (typeof v !== 'number' || (!isNaN(v) && isFinite(v)));

@Pipe({
    name: 'gtxFilter',
})
export class FilterPipe implements PipeTransform {
    transform(value: any, fn: FilterFn = DEFAULT_FILTER): any {
        if (typeof fn !== 'function') {
            return value;
        }

        if (Array.isArray(value)) {
            return value.filter(v => fn(v));
        } else if (fn(value)) {
            return value;
        }
    }
}
