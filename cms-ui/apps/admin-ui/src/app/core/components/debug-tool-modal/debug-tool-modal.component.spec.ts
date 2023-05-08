import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { configureComponentTest } from '../../../../testing';
import { DebugToolService } from '../../providers/debug-tool/debug-tool.service';
import { DebugToolModalComponent } from './debug-tool-modal.component';

describe('DebugToolModal', () => {
    let fixture: ComponentFixture<DebugToolModalComponent>;
    beforeEach(() => {
        configureComponentTest({
            imports: [
                FormsModule,
            ],
            declarations: [
                DebugToolModalComponent,
            ],
        });

        spyOn(DebugToolService.prototype, 'init').and.stub();
        const debugToolService = new DebugToolService(null, null, null, null, null, null);

        fixture = TestBed.createComponent(DebugToolModalComponent);
        fixture.componentInstance.debugToolService = debugToolService;
    });

    it('should call generateReport when Generate Report clicked', fakeAsync(() => {
        fixture.detectChanges();

        spyOn(fixture.componentInstance, 'generateReport').and.callThrough();
        spyOn(fixture.componentInstance.debugToolService, 'generateReport').and.returnValue(Promise.resolve({}));

        const btn = fixture.debugElement.query(By.css('gtx-button#debug-btn-report'));
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

        const btn = fixture.debugElement.query(By.css('gtx-button#debug-btn-clear'));
        btn.triggerEventHandler('click', null);
        tick();

        fixture.detectChanges();

        expect(fixture.componentInstance.clearSiteData).toHaveBeenCalled();
        expect(fixture.componentInstance.debugToolService.clearSiteData).toHaveBeenCalled();
    }));

});
