import { Component } from '@angular/core';
import { inject, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ModalService } from '@gentics/ui-core';
import { CoreModule } from '../../../core.module';
import { DiscardChangesGuard, OnDiscardChanges } from './discard-changes.guard';

class MockModalService {
    fromComponent = jasmine.createSpy('ModalService.fromComponent')
        .and.returnValue(new Promise(neverResolve => {}));
}

@Component({
    template: '',
})
class TestComponent implements OnDiscardChanges {
    userHasEdited: boolean;
    changesValid: boolean;
    updateEntity: () => Promise<any>;
    resetEntity: () => Promise<any>;
    navigate(): void { }
}

xdescribe('DiscardChangesGuard', () => {
    let modalService: MockModalService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                CoreModule,
                RouterTestingModule,
            ],
            declarations: [
                TestComponent,
            ],
            providers: [
                { provide: ModalService, useClass: MockModalService },
            ],
        }).compileComponents();

        modalService = TestBed.get(ModalService);
    });

    it('should inject', inject([DiscardChangesGuard], (guard: DiscardChangesGuard<TestComponent>) => {
        expect(guard).toBeTruthy();
    }));

    xit('should allow navigation if no edits have been made', () => {
        // TODO: write test
    });

    xit('should call a modal if edits have been made', () => {
        // TODO: write test
    });

    xit('should allow navigation if the user confirms discarding changes', () => {
        // TODO: write test
    });

    xit('should prevent navigation if the user cancels discarding changes', () => {
        // TODO: write test
    });
});
