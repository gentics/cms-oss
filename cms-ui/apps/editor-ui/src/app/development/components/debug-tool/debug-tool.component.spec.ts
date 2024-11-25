import { TestBed, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { ApplicationStateService } from '@editor-ui/app/state';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { componentTest, configureComponentTest } from '../../../../testing';
import { DetailChip } from '../../../shared/components/detail-chip/detail-chip.component';
import { DebugToolService } from '../../providers/debug-tool.service';
import { DebugTool } from './debug-tool.component';

class MockDebugToolService implements Partial<DebugToolService> {
    initialize = jasmine.createSpy('initialize').and.stub();
    generateReport = jasmine.createSpy('generateReport').and.callFake(() => Promise.resolve({}));
    clearSiteData = jasmine.createSpy('clearSiteData').and.callFake(() => Promise.resolve('clear'));
}

describe('DebugToolComponent', () => {

    let service: DebugToolService;

    beforeEach(() => {
        configureComponentTest({
            imports: [
                GenticsUICoreModule,
                FormsModule,
            ],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: DebugToolService, useClass: MockDebugToolService },
            ],
            declarations: [
                DebugTool,
                DetailChip,
            ],
        });

        service = TestBed.inject(DebugToolService);
    });

    it('should call generateReport when Generate Report clicked', componentTest(() => DebugTool, (fixture, instance) => {
        fixture.detectChanges();

        spyOn(instance, 'generateReport').and.callThrough();

        const btn = fixture.debugElement.query(By.css('gtx-button#debug-btn-report'));
        btn.triggerEventHandler('click', null);
        tick();

        fixture.detectChanges();

        expect(instance.generateReport).toHaveBeenCalled();
        expect(service.generateReport).toHaveBeenCalled();
    }));

    it('should call clearSiteData when Clear Local Data clicked', componentTest(() => DebugTool, (fixture, instance) => {
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
