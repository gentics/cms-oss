import { TestBed } from '@angular/core/testing';

import { GroupDataService } from './group-data.service';

xdescribe('GroupDataService', () => {
    beforeEach(() => TestBed.configureTestingModule({}));

    it('should be created', () => {
        const service: GroupDataService = TestBed.inject(GroupDataService);
        expect(service).toBeTruthy();
    });
});
