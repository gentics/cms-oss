import { fakeAsync, TestBed, tick } from '@angular/core/testing';

import { EventEmitter } from '@angular/core';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { BehaviorSubject, of } from 'rxjs';
import { FilterOptions } from '../../common/models/filter-options';
import { getSealedProxyObject, ObjectWithEvents } from '../../common/utils';
import { AppService } from '../app/app.service';
import { FilterService } from '../filter/filter.service';
import { UserSettingsService } from '../user-settings/user-settings.service';
import { LinkCheckerService } from './link-checker.service';

class MockUserSettingsService {

}

class MockGcmsApi {
    linkChecker = new class {
        getPages = jasmine.createSpy('getPages').and.returnValue(of({}));
    }();
}

class MockAppService {
    public updateInternal$ = new BehaviorSubject<boolean>(false);
    public update$ = this.updateInternal$.asObservable();
}

class MockFilterService {
    events$ = new EventEmitter<FilterOptions>();
    filterOptions = getSealedProxyObject({
        nodeId: null,
        editable: null,
        isCreator: false,
        isEditor: false,
        languages: [],
        page: 1,
        pageSize: 10,
        searchTerm: null,
        sortOptions: [],
        status: null,
        online: null
    }, undefined, this.events$);

    get options(): FilterOptions & ObjectWithEvents<FilterOptions> {
        return this.filterOptions;
    }
}

describe('LinkCheckerService', () => {
    const api = new MockGcmsApi();
    const appService = new MockAppService();
    const filterService = new MockFilterService();


    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                { provide: UserSettingsService, useClass: MockUserSettingsService },
                { provide: FilterService, useValue: filterService },
                { provide: GcmsApi, useValue: api },
                { provide: AppService, useValue: appService },
            ]
        });
    });

    it('should be created', () => {
        const service: LinkCheckerService = TestBed.inject(LinkCheckerService);
        expect(service).toBeTruthy();
    });

    describe('fetchFilteredPages()', () => {

        it('calls API with the correct values', fakeAsync(() => {
            const service: LinkCheckerService = TestBed.inject(LinkCheckerService);

            service.fetchFilteredPages();
            tick();

            filterService.options.nodeId = 1;
            filterService.options.page = 10;
            filterService.options.isEditor = true;
            filterService.options.languages = [1, 2, 3];
            filterService.options.online = true;
            tick(100);

            expect(api.linkChecker.getPages).toHaveBeenCalledWith({
                iscreator: false,
                iseditor: true,
                nodeId: 1,
                page: 10,
                pageSize: 10,
                language: [1, 2, 3],
                sort: [],
                online: true,
            });
        }));

        it('called on events', fakeAsync(() => {
            const service: LinkCheckerService = TestBed.inject(LinkCheckerService);

            service.fetchFilteredPages();
            tick();

            // Call at first time
            expect(api.linkChecker.getPages).toHaveBeenCalledTimes(1);

            // Update signal
            appService.updateInternal$.next(true);
            tick(100);

            // Called second time
            expect(api.linkChecker.getPages).toHaveBeenCalledTimes(2);
        }));

    });
});
