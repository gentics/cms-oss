import { EventEmitter, Injectable } from '@angular/core';
import { Page, PagingSortOption } from '@gentics/cms-models';
import { FilterOptions } from '../../common/models/filter-options';
import { getSealedProxyObject, ObjectWithEvents } from '../../common/utils/get-sealed-proxy-object';

@Injectable({
    providedIn: 'root'
})
export class FilterService {

    private events$ = new EventEmitter<FilterOptions>();
    private initialized = false;

    protected readonly defaultFilterOptions: FilterOptions = {
        nodeId: null,
        editable: null,
        isCreator: false,
        isEditor: false,
        languages: [],
        page: 1,
        pageSize: 10,
        searchTerm: null,
        sortOptions: [],
        status: 'invalid',
        online: null,
    };

    protected filterOptions: FilterOptions & ObjectWithEvents<FilterOptions>;

    get options(): FilterOptions & ObjectWithEvents<FilterOptions> {
        return this.filterOptions;
    }

    init(): void {
        if (this.initialized) {
            throw new Error('The FilterService.init() method must be called only once.');
        }

        // Initialize filterOptions with default data
        this.reset();
        this.initialized = true;
    }

    reset(preset?: Partial<FilterOptions>): void {
        this.filterOptions = getSealedProxyObject({ ...this.defaultFilterOptions, ...preset }, undefined, this.events$);
    }

    /**
     * Saves the sorting options to user settings
     * @param sorting Sorting options
     */
    setSortingOptions(sorting: PagingSortOption<Page>[]): void {
        this.options.sortOptions = sorting;
    }
}
