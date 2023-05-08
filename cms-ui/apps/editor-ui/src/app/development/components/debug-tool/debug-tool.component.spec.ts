import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { ApplicationStateService } from '@editor-ui/app/state';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { configureComponentTest } from '../../../../testing';
import { DetailChip } from '../../../shared/components/detail-chip/detail-chip.component';
import { DebugToolService } from '../../providers/debug-tool.service';
import { DebugTool } from './debug-tool.component';

describe('DebugToolComponent', () => {
    let fixture: ComponentFixture<DebugTool>;
    beforeEach(() => {
        configureComponentTest({
            imports: [
                GenticsUICoreModule,
                FormsModule,
            ],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
            declarations: [
                DebugTool,
                DetailChip,
            ],
        });

        spyOn(DebugToolService.prototype, 'initialize').and.stub();
        let debugToolService = new DebugToolService(null, null, null, null, null, null, null, null);

        fixture = TestBed.createComponent(DebugTool);
        fixture.componentInstance.debugToolService = debugToolService;
    });

    it('should call generateReport when Generate Report clicked', fakeAsync(() => {
        fixture.detectChanges();

        spyOn(fixture.componentInstance, 'generateReport').and.callThrough();
        spyOn(fixture.componentInstance.debugToolService, 'generateReport').and.returnValue(Promise.resolve({}));

        let btn = fixture.debugElement.query(By.css('gtx-button#debug-btn-report'));
        btn.triggerEventHandler('click', null);
        tick();

        fixture.detectChanges();

        expect(fixture.componentInstance.generateReport).toHaveBeenCalled();
        expect(fixture.componentInstance.debugToolService.generateReport).toHaveBeenCalled();
    }));

    it('should call clearSiteData when Clear Local Data clicked', fakeAsync(() => {
        fixture.detectChanges();

        spyOn(fixture.componentInstance, 'clearSiteData').and.callThrough();
        spyOn(fixture.componentInstance.debugToolService, 'clearSiteData').and.returnValue(Promise.resolve('clear'));

        let btn = fixture.debugElement.query(By.css('gtx-button#debug-btn-clear'));
        btn.triggerEventHandler('click', null);
        tick();

        fixture.detectChanges();

        expect(fixture.componentInstance.clearSiteData).toHaveBeenCalled();
        expect(fixture.componentInstance.debugToolService.clearSiteData).toHaveBeenCalled();
    }));

});
