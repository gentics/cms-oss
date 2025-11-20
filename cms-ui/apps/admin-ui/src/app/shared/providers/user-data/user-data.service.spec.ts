import { TestBed } from '@angular/core/testing';
import { UserDataService } from './user-data.service';

xdescribe('UserDataService', () => {
    beforeEach(() => TestBed.configureTestingModule({}));

    it('should be created', () => {
        const service: UserDataService = TestBed.inject(UserDataService);
        expect(service).toBeTruthy();
    });
});
