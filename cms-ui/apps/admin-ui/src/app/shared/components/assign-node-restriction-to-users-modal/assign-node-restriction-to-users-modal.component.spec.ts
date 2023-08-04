import {
    ContentRepositoryHandlerService,
    EntityManagerService,
    ErrorHandler,
    GroupOperations,
    I18nNotificationService,
    I18nService,
    NodeOperations,
    PermissionsService,
    UserOperations,
} from '@admin-ui/core';
import { MockI18nServiceWithSpies } from '@admin-ui/core/providers/i18n/i18n.service.mock';
import { Component, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { NgxsModule } from '@ngxs/store';
import { Observable, of } from 'rxjs';
import { GcmsTestData } from '../../../../../../../libs/cms-models/src/lib/testing';
import { componentTest } from '../../../../testing';
import { createDelayedObservable } from '../../../../testing/utils/rxjs-utils';
import { USER_ACTION_PERMISSIONS, USER_ACTION_PERMISSIONS_DEF } from '../../../common/user-action-permissions/user-action-permissions';
import { InterfaceOf } from '../../../common/utils/util-types/util-types';
import { MockEntityManagerService } from '../../../core/providers/entity-manager/entity-manager.service.mock';
import { MockI18nNotificationService } from '../../../core/providers/i18n-notification/i18n-notification.service.mock';
import { AppStateService } from '../../../state/providers/app-state/app-state.service';
import { OPTIONS_CONFIG } from '../../../state/state-store.config';
import { STATE_MODULES } from '../../../state/state.module';
import { TestAppState } from '../../../state/utils/test-app-state/test-app-state.mock';
import { ActionAllowedDirective } from '../../directives';
import { GroupDataService, GroupUserDataService, NodeDataService, NotificationService, WizardService } from '../../providers';
import { BooleanIconComponent } from '../boolean-icon/boolean-icon.component';
import { IconComponent } from '../icon/icon.component';
import { NodeTableComponent } from '../node-table/node-table.component';
import { AssignNodeRestrictionsToUsersModalComponent } from './assign-node-restriction-to-users-modal.component';

const MOCK_NODES = [
    GcmsTestData.getExampleNodeData({ id: 1 }),
    GcmsTestData.getExampleNodeData({ id: 2 }),
];

const MOCK_USER_RESTRICTIONS = [
    {
        nodeIds: [1, 2],
        hidden: 1,
    },
];

class MockActivatedRoute {}

class MockErrorHandler {}

class MockGroupOperations {}

class MockNodeOperations {
    getAll = jasmine.createSpy('getAll').and.callFake(() => createDelayedObservable(MOCK_NODES));
}

class MockNotificationService {}

class MockPermissionsService implements Partial<InterfaceOf<PermissionsService>> {
    checkPermissions(): Observable<boolean> {
        return createDelayedObservable(true);
    }
}

class MockRouter {}

class MockUserOperations {
    getUserNodeRestrictions = jasmine.createSpy('getUserNodeRestrictions').and.callFake(() => createDelayedObservable(MOCK_USER_RESTRICTIONS));
}

class MockWizardService {}

@Pipe({ name: 'i18n' })
class MockI18nPipe implements PipeTransform {
    transform(key: string, params: object): string {
        return key + (params ? ':' + JSON.stringify(params) : '');
    }
}

@Component({
    template: `
        <gtx-assign-node-restriction-to-users-modal></gtx-assign-node-restriction-to-users-modal>
    `,
})
class TestComponent {
    userId: number;
    groupId: number;
    nodeIdsInitial: number[];
    nodeIdsSelected: number[];

    buttonAssignNodeRestrictonsClicked = jasmine.createSpy('buttonAssignNodeRestrictonsClicked').and.stub();
    getModalTitle = jasmine.createSpy('getModalTitle').and.stub();
}

// TODO: Finish Unit Tests
xdescribe('AssignNodeRestrictionsToUsersModalComponent', () => {
    let component: TestComponent;
    let fixture: ComponentFixture<TestComponent>;
    let groupUserData: GroupUserDataService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                GenticsUICoreModule.forRoot(),
                FormsModule,
                NgxsModule.forRoot(STATE_MODULES, OPTIONS_CONFIG),
            ],
            declarations: [
                ActionAllowedDirective,
                AssignNodeRestrictionsToUsersModalComponent,
                BooleanIconComponent,
                IconComponent,
                MockI18nPipe,
                NodeTableComponent,
                TestComponent,
            ],
            providers: [
                { provide: ActivatedRoute, useClass: MockActivatedRoute },
                { provide: AppStateService, useClass: TestAppState },
                ContentRepositoryHandlerService,
                { provide: EntityManagerService, useClass: MockEntityManagerService },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                GroupDataService,
                { provide: GroupOperations, useClass: MockGroupOperations },
                GroupUserDataService,
                { provide: I18nNotificationService, useClass: MockI18nNotificationService },
                { provide: I18nService, useClass: MockI18nServiceWithSpies },
                NodeDataService,
                { provide: NodeOperations, useClass: MockNodeOperations },
                { provide: NotificationService, useClass: MockNotificationService },
                { provide: PermissionsService, useClass: MockPermissionsService },
                { provide: Router, useClass: MockRouter },
                { provide: USER_ACTION_PERMISSIONS, useValue: USER_ACTION_PERMISSIONS_DEF },
                { provide: UserOperations, useClass: MockUserOperations },
                { provide: WizardService, useClass: MockWizardService },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(TestComponent);
        component = fixture.componentInstance;
        groupUserData = TestBed.get(GroupUserDataService);
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should return the right data after calling buttonAssignNodeRestrictonsClicked()',
        componentTest(() => TestComponent, (fixture, instance) => {
            const groupUserDataSpy = spyOn(groupUserData, 'changeUserNodeRestrictions').and.returnValue(
                of(MOCK_USER_RESTRICTIONS) as any,
            );

            fixture.detectChanges();
            tick();

            instance.buttonAssignNodeRestrictonsClicked();

            fixture.detectChanges();
            tick();

            expect(groupUserData.changeUserNodeRestrictions).toHaveBeenCalled();
            expect(groupUserDataSpy).toHaveBeenCalledWith(instance.userId, instance.groupId, instance.nodeIdsSelected, instance.nodeIdsInitial);
        }),
    );
});
