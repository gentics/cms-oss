import { Pipe, PipeTransform } from '@angular/core';
import { Item } from '@gentics/cms-models';


/** Used to always return the same fallback value. */
const emptyFallbackArray: any[] = [];

/**
 * Filters a list of items by a filter term.
 * Tries to return the same reference when the result is unchanged.
 */
@Pipe({ name: 'filterItems' })
export class FilterItemsPipe implements PipeTransform {
    private lastItems: Item[];
    private lastFilter: string;
    private lastResult: any[];
    private lastShowPath: boolean;

    constructor() {}

    transform<T extends Item & { fileName?: string }>(items: T[], filterTerm: string, showPath: boolean): T[] {
        filterTerm = typeof filterTerm === 'string' ? filterTerm : '';

        if (!items || !items.length) {
            return emptyFallbackArray;
        }

        if (showPath === this.lastShowPath && filterTerm === this.lastFilter && arraysAreEqual(this.lastItems, items)) {
            return this.lastResult;
        }

        this.lastItems = items;
        this.lastFilter = filterTerm;
        this.lastShowPath = showPath;

        if (!filterTerm) {
            return this.lastResult = items;
        }

        const filterLowercase = filterTerm.toLowerCase();
        const filtered = items.filter(item =>
            item.name.toLowerCase().indexOf(filterLowercase) >= 0 ||
            (showPath && item.fileName && item.fileName.toLowerCase().indexOf(filterLowercase) >= 0)
        );

        if (!filtered.length) {
            return this.lastResult = emptyFallbackArray;
        } else if (filtered.length === items.length) {
            return this.lastResult = items;
        } else if (arraysAreEqual(this.lastResult, filtered)) {
            return this.lastResult;
        } else {
            return this.lastResult = filtered;
        }
    }
}

function arraysAreEqual<T>(first: T[] | undefined, second: T[]): boolean {
    if (!first || !second || first.length !== second.length) {
        return false;
    }
    return first === second || first.every((item, index) => second[index] === item);
}
