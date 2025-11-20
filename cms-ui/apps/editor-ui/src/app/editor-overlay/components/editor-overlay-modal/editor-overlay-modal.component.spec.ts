import { Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { File as FileModel, Folder, Image, Node, Page } from '@gentics/cms-models';
import { GenticsUICoreModule, ModalService } from '@gentics/ui-core';
import { NgxsModule } from '@ngxs/store';
import { take } from 'rxjs/operators';
import { ApplicationStateService, STATE_MODULES } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { EditorOverlayModal } from './editor-overlay-modal.component';

@Component({
    selector: 'test-component',
    template: '<gtx-overlay-host></gtx-overlay-host>',
    standalone: false,
})
class TestComponent extends EditorOverlayModal {
    currentItem: Page | FileModel | Folder | Image | Node;
    saveAndClose(): void { }
}

let appState: TestApplicationState;

describe('EditorOverlayModal', () => {
    let component: TestComponent;
    let fixture: ComponentFixture<TestComponent>;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [
                NgxsModule.forRoot(STATE_MODULES),
                GenticsUICoreModule.forRoot(),
            ],
            declarations: [
                TestComponent,
            ],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: ModalService, useClass: MockModalService },
            ],
            schemas: [NO_ERRORS_SCHEMA],
        }).compileComponents();

        appState = TestBed.inject(ApplicationStateService) as any;
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(TestComponent);
        component = fixture.componentInstance;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should respect appState.ui.language', fakeAsync(() => {
        uiLanguageState(fixture);
        tick();
        fixture.detectChanges();

        component.uiLanguage$.pipe(take(1)).subscribe(language => {
            expect(language).toBe('de');
        });
    }));

    it('should register guarded cancel function onInit', fakeAsync(() => {
        spyOn(component, 'registerCancelFn');

        fixture.detectChanges();

        expect(component.registerCancelFn).toHaveBeenCalled();
    }));
});

function uiLanguageState(fixture: ComponentFixture<any>): void {
    appState.mockState({
        ui: {
            language: 'de',
        },
    });
    fixture.detectChanges();
}

class MockModalService { }
