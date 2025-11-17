import { ChangeDetectorRef, Component, Directive, ViewChild } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';
import { ButtonComponent, CheckboxComponent, IconDirective, ModalService } from '@gentics/ui-core';
import { componentTest, configureComponentTest } from '../../../../testing';
import { IconCheckbox } from '../../../shared/components/icon-checkbox/icon-checkbox.component';
import { ImageThumbnailComponent } from '../../../shared/components/image-thumbnail/image-thumbnail.component';
import { SendMessageModal } from '../../../shared/components/send-message-modal/send-message-modal.component';
import { ApplicationStateService, FolderActionsService, MessageActionsService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { EntityResolver } from '../../providers/entity-resolver/entity-resolver';
import { NavigationService } from '../../providers/navigation/navigation.service';
import { PermissionService } from '../../providers/permissions/permission.service';
import { MessageBody } from '../message-body/message-body.component';
import { MessageList } from '../message-list/message-list.component';
import { MessageInboxComponent } from './message-inbox.component';

describe('MessageInboxComponent', () => {

    let appState: TestApplicationState;
    let modalService: MockModalService;

    beforeEach(() => {
        configureComponentTest({
            imports: [RouterTestingModule],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: NavigationService, useClass: MockNavigationService },
                { provide: EntityResolver, useClass: MockEntityResolver },
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: MessageActionsService, useClass: MockMessageActions },
                { provide: ChangeDetectorRef, useClass: MockChangeDetectorRef },
                { provide: PermissionService, useClass: MockPermissionService },
                { provide: ModalService, useClass: MockModalService },
            ],
            declarations: [
                ButtonComponent,
                IconDirective,
                CheckboxComponent,
                MessageBody,
                MessageInboxComponent,
                MessageList,
                IconCheckbox,
                ImageThumbnailComponent,
                MockOverrideSlotDirective,
                TestComponent,
            ],
        });

        appState = TestBed.inject(ApplicationStateService) as any;
        modalService = TestBed.inject(ModalService) as any;
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

            expect(modalService.fromComponent).toHaveBeenCalledWith(SendMessageModal);
        }),
    );

});

@Component({
    template: `
        <message-inbox (navigate)="navigate($event)">
        </message-inbox>`,
    standalone: false,
})
class TestComponent {
    @ViewChild(MessageInboxComponent, { static: true }) messageInbox: MessageInboxComponent;

    navigate(): void { }
}

@Directive({
    selector: '[overrideSlot],[overrideParams]',
    standalone: false,
})
class MockOverrideSlotDirective {}

class MockNavigationService {}
class MockEntityResolver {}
class MockFolderActions {}
class MockMessageActions {}
class MockChangeDetectorRef {}
class MockPermissionService {}

class MockModalService {
    fromComponent = jasmine.createSpy('ModalService.fromComponent')
        .and.returnValue(new Promise((neverResolve) => {}));
}

class MockI18nService {
    translate = jasmine.createSpy('I18nService.get').and.returnValue('');
}
