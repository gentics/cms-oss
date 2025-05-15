import { ChangeDetectorRef, Component, Directive, ViewChild } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';
import { ModalService } from '@gentics/ui-core';
import { componentTest, configureComponentTest } from '../../../../testing';
import { IconCheckboxComponent } from '../../../shared/components/icon-checkbox/icon-checkbox.component';
import { SendMessageModalComponent } from '../../../shared/components/send-message-modal/send-message-modal.component';
import { AppStateService } from '../../../state';
import { TestAppState, assembleTestAppStateImports } from '../../../state/utils/test-app-state';
import { EntityManagerService, I18nService, MessageService } from '../../providers';
import { MessageBodyComponent } from '../message-body/message-body.component';
import { MessageListComponent } from '../message-list/message-list.component';
import { MessageInboxComponent } from './message-inbox.component';

class MockI18nService {
    instant = jasmine.createSpy('I18nService.instant').and.returnValue('');
}

@Component({
    template: `
        <gtx-message-inbox (navigate)="navigate($event)">
        </gtx-message-inbox>`,
    standalone: false,
})
class TestComponent {
    @ViewChild(MessageInboxComponent) messageInbox: MessageInboxComponent;

    navigate(): void { }
}


// tslint:disable-next-line: directive-selector
@Directive({
    selector: '[overrideSlot],[overrideParams]',
    standalone: false,
})
class MockOverrideSlotDirective {}
class MockChangeDetectorRef {}

class MockModalService {
    fromComponent = jasmine.createSpy('ModalService.fromComponent')
        .and.returnValue(new Promise(neverResolve => {}));
}


describe('MessageInbox', () => {

    let modalService: MockModalService;

    beforeEach(() => {
        configureComponentTest({
            imports: [
                assembleTestAppStateImports(),
                RouterTestingModule,
            ],
            providers: [
                { provide: MessageService, useValue: {} },
                { provide: EntityManagerService, useValue: {} },
                { provide: AppStateService, useClass: TestAppState },
                { provide: ChangeDetectorRef, useClass: MockChangeDetectorRef },
                { provide: ModalService, useClass: MockModalService },
                { provide: I18nService, useClass: MockI18nService },
            ],
            declarations: [
                IconCheckboxComponent,
                MessageBodyComponent,
                MessageInboxComponent,
                MessageListComponent,
                MockOverrideSlotDirective,
                TestComponent,
            ],
        });

        modalService = TestBed.get(ModalService);
    });

    it('is created ok',
        componentTest(() => TestComponent, (fixture, testComponent) => {
            fixture.detectChanges();
            expect(testComponent.messageInbox).toBeDefined();
        }),
    );

    it('clicking the "new message" button opens SendMessageModal',
        componentTest(() => TestComponent, (fixture, testComponent) => {
            fixture.detectChanges();

            fixture.debugElement.query(By.css('.new-message-button')).triggerEventHandler('click', {});

            expect(modalService.fromComponent).toHaveBeenCalledWith(SendMessageModalComponent);
        }),
    );

});
