import { TestBed } from '@angular/core/testing';
import { FilterOptions } from '../../common/models/filter-options';
import { FilterService } from './filter.service';

describe('FilterService', () => {
    let filterService: FilterService;

    beforeEach(() => {
        TestBed.configureTestingModule({});

        filterService = TestBed.inject(FilterService);
    });

    it('should be created', () => {
        expect(filterService).toBeTruthy();
    });

    it('should build correctly from default settings', () => {
        const defaultFilterOptions: FilterOptions = {
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
            online: null
        };

        filterService.init();

        expect(filterService.options).toEqual(jasmine.objectContaining(defaultFilterOptions));
    });
});
