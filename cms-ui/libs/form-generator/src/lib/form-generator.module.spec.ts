import { TestBed, waitForAsync } from '@angular/core/testing';
import { FormGeneratorModule } from './form-generator.module';

describe('CmsComponentsModule', () => {
    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [
                FormGeneratorModule,
            ],
        }).compileComponents();
    }));

    it('should create', () => {
        expect(FormGeneratorModule).toBeDefined();
    });
});
