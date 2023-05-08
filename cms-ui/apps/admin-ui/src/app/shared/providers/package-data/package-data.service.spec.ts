import { TestBed } from '@angular/core/testing';
import { PackageDataService } from './package-data.service';

xdescribe('PackageDataService', () => {
    beforeEach(() => TestBed.configureTestingModule({}));

    it('should be created', () => {
        const service: PackageDataService = TestBed.get(PackageDataService);
        expect(service).toBeTruthy();
    });
});
