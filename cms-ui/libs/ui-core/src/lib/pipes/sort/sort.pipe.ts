import { Pipe, PipeTransform } from '@angular/core';

enum SortOrder {
    ASC = 'asc',
    DESC = 'desc',
}

type SortFunction<T> = (a: T, b: T, order: SortOrder) => number;

function defaultSort<T>(a: T, b: T, order: SortOrder): number {
    if (a == null && b == null) {
        return 0;
    }
    if (a == null) {
        return order === SortOrder.ASC ? -1 : 1;
    }
    if (b == null) {
        return order === SortOrder.ASC ? 1 : -1;
    }
    if (typeof a === 'number' && typeof b === 'number') {
        return order === SortOrder.ASC ? a - b : b - a;
    }
    if (typeof a !== 'string') {
        a = a.toString() as any;
    }
    if (typeof b !== 'string') {
        b = b.toString() as any;
    }

    return order === SortOrder.ASC
        ? (a as string).localeCompare(b as string)
        : (b as string).localeCompare(a as string);
}

@Pipe({
    name: 'gtxSort',
})
export class SortPipe implements PipeTransform {
    transform<T>(value: T[] | Set<T>, orderOrFunction: SortOrder | SortFunction<T> = defaultSort<T>, order: SortOrder = SortOrder.ASC): T[] {
        if (value == null) {
            return [];
        }
        if ((value as any).__proto__ === Set.prototype) {
            value = Array.from(value);
        } else {
            value = (value as T[]).slice(0);
        }

        let sorter: SortFunction<T>;

        if (orderOrFunction == null) {
            sorter = defaultSort;
        } else if (typeof orderOrFunction === 'function') {
            sorter = orderOrFunction;
        } else if (typeof orderOrFunction === 'string') {
            sorter = defaultSort;
            order = orderOrFunction;
        }

        return value.sort((a, b) => sorter(a, b, order));
    }
}
