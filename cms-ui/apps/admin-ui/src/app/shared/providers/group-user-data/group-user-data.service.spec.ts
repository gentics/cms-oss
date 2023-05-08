import { TestBed } from '@angular/core/testing';
import { GroupUserDataService } from './group-user-data.service';


xdescribe('GroupDataService', () => {
    beforeEach(() => TestBed.configureTestingModule({}));

    it('should be created', () => {
        const service: GroupUserDataService = TestBed.get(GroupUserDataService);
        expect(service).toBeTruthy();
    });
});
