import { TestBed } from '@angular/core/testing';
import { UserGroupDataService } from './user-group-data.service';


xdescribe('UserGroupDataService', () => {
    beforeEach(() => TestBed.configureTestingModule({}));

    it('should be created', () => {
        const service: UserGroupDataService = TestBed.inject(UserGroupDataService);
        expect(service).toBeTruthy();
    });
});
