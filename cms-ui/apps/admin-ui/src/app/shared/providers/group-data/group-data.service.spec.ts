import { TestBed } from '@angular/core/testing';

import { GroupDataService } from './group-data.service';

xdescribe('GroupDataService', () => {
    beforeEach(() => TestBed.configureTestingModule({}));

    it('should be created', () => {
        const service: GroupDataService = TestBed.get(GroupDataService);
        expect(service).toBeTruthy();
    });
});
