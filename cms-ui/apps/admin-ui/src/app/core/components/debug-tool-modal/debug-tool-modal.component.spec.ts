import { TestBed, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { componentTest, configureComponentTest } from '../../../../testing';
import { DebugToolService } from '../../providers/debug-tool/debug-tool.service';
import { DebugToolModalComponent } from './debug-tool-modal.component';

class MockDebugToolService implements Partial<DebugToolService> {
    init = jasmine.createSpy('init').and.stub();
    generateReport = jasmine.createSpy('generateReport').and.callFake(() => Promise.resolve({}));
    clearSiteData = jasmine.createSpy('clearSiteData').and.callFake(() => Promise.resolve('clear'));
}

describe('DebugToolModal', () => {

    let service: DebugToolService;

    beforeEach(() => {
        configureComponentTest({
            imports: [
                FormsModule,
            ],
            declarations: [
                DebugToolModalComponent,
            ],
            providers: [
                { provide: DebugToolService, useClass: MockDebugToolService },
            ],
        });

        service = TestBed.inject(DebugToolService) as any;
    });

    it('should call generateReport when Generate Report clicked', componentTest(() => DebugToolModalComponent, (fixture, instance) => {
        fixture.detectChanges();

        spyOn(instance, 'generateReport').and.callThrough();

        const btn = fixture.debugElement.query(By.css('gtx-button#debug-btn-report'));
        btn.triggerEventHandler('click', null);
        tick();

        fixture.detectChanges();

        expect(instance.generateReport).toHaveBeenCalled();
        expect(service.generateReport).toHaveBeenCalled();
    }));

    it('should call clearSiteData when Clear Local Data clicked', componentTest(() => DebugToolModalComponent, (fixture, instance) => {
        fixture.detectChanges();

        spyOn(instance, 'clearSiteData').and.callThrough();

        const btn = fixture.debugElement.query(By.css('gtx-button#debug-btn-clear'));
        btn.triggerEventHandler('click', null);
        tick();

        fixture.detectChanges();

        expect(instance.clearSiteData).toHaveBeenCalled();
        expect(service.clearSiteData).toHaveBeenCalled();
    }));

});
