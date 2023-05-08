import { TestBed, waitForAsync } from '@angular/core/testing';
import { CmsComponentsModule } from './cms-components.module';

describe('CmsComponentsModule', () => {
    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [CmsComponentsModule],
        }).compileComponents();
    }));

    it('should create', () => {
        expect(CmsComponentsModule).toBeDefined();
    });
});
