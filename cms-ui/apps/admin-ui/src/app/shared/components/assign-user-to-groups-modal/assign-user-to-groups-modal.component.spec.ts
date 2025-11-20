import {
    EntityManagerService,
    ErrorHandler,
    GroupOperations,
    PermissionsService,
    UserOperations,
} from '@admin-ui/core';
import { Component, NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Group, ModelType } from '@gentics/cms-models';
import { getExampleUserData } from '@gentics/cms-models/testing';
import { GenticsUICoreModule, NotificationService } from '@gentics/ui-core';
import { NgxsModule } from '@ngxs/store';
import { Observable, of } from 'rxjs';
import { componentTest } from '../../../../testing';
import { createDelayedObservable } from '../../../../testing/utils/rxjs-utils';
import { InterfaceOf } from '../../../common/utils/util-types/util-types';
import { MockEntityManagerService } from '../../../core/providers/entity-manager/entity-manager.service.mock';
import { AppStateService } from '../../../state/providers/app-state/app-state.service';
import { OPTIONS_CONFIG } from '../../../state/state-store.config';
import { STATE_MODULES } from '../../../state/state.module';
import { TestAppState } from '../../../state/utils/test-app-state/test-app-state.mock';
import { ActionAllowedDirective } from '../../directives/action-allowed/action-allowed.directive';
import {
    ContextMenuService,
    GroupDataService,
    GroupUserDataService,
    SubgroupDataService,
    UserDataService,
} from '../../providers';
import { GroupTrableComponent } from '../group-trable/group-trable.component';
import { AssignUserToGroupsModal } from './assign-user-to-groups-modal.component';

const PARENT_NODE_ID = 2;
const FIRST_PARENT_NODE_ID = 4;
const SECOND_PARENT_NODE_ID = 5;

const MOCK_USER = getExampleUserData({ id: 1 });

const MOCK_PARENT_GROUPS: Group<ModelType>[] = [
    {
        id: PARENT_NODE_ID,
        name: 'Node Super Admin',
        description: '',
        children: [
            {
                id: 3,
                name: 'Admins',
                description: '',
                children: [
                    {
                        id: FIRST_PARENT_NODE_ID,
                        name: 'Gentics',
                        description: '',
                        children: [],
                    },
                    {
                        id: SECOND_PARENT_NODE_ID,
                        name: 'Customer',
                        description: '',
                        children: [
                            {
                                id: 6,
                                name: 'Editors',
                                description: '',
                                children: [
                                    {
                                        id: 7,
                                        name: 'Limited Editors',
                                        description: '',
                                        children: [],
                                    },
                                ],
                            },
                            {
                                id: 8,
                                name: 'Translators',
                                description: '',
                                children: [],
                            },
                        ],
                    },
                ],
            },
        ],
    },
];

class MockActivatedRoute {}

class MockErrorHandler {}

class MockGroupOperations {
    getAll = jasmine.createSpy('getAll').and.callFake(() => createDelayedObservable(MOCK_PARENT_GROUPS));
}

class MockNotificationService {}

class MockPermissionsService implements Partial<InterfaceOf<PermissionsService>> {
    checkPermissions(): Observable<boolean> {
        return createDelayedObservable(true);
    }
}

class MockRouter {}

class MockUserOperations {}

@Pipe({
    name: 'i18n',
    standalone: false,
})
class MockI18nPipe implements PipeTransform {
    transform(key: string, params: object): string {
        return key + (params ? ':' + JSON.stringify(params) : '');
    }
}

@Component({
    template: `
        <gtx-assign-user-to-groups-modal></gtx-assign-user-to-groups-modal>
    `,
    standalone: false,
})
class TestComponent {
    userIds: number[] = [];
    userGroupIds: number[] = [];

    allIsValid = jasmine.createSpy('allIsValid').and.stub();
    buttonAssignUserToGroupsClicked = jasmine.createSpy('buttonAssignUserToGroupsClicked').and.stub();
    getModalTitle = jasmine.createSpy('getModalTitle').and.stub();
    changeGroupsOfUsers = jasmine.createSpy('changeGroupsOfUsers').and.stub();
}

// TODO: Finish Unit Tests
xdescribe('AssignUserToGroupsModalComponent', () => {
    let component: TestComponent;
    let fixture: ComponentFixture<TestComponent>;
    let userData: UserDataService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                FormsModule,
                GenticsUICoreModule.forRoot(),
                NgxsModule.forRoot(STATE_MODULES, OPTIONS_CONFIG),
            ],
            declarations: [
                ActionAllowedDirective,
                AssignUserToGroupsModal,
                GroupTrableComponent,
                MockI18nPipe,
                TestComponent,
            ],
            providers: [
                { provide: ActivatedRoute, useClass: MockActivatedRoute },
                { provide: AppStateService, useClass: TestAppState },
                ContextMenuService,
                { provide: EntityManagerService, useClass: MockEntityManagerService },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                GroupDataService,
                GroupUserDataService,
                { provide: GroupOperations, useClass: MockGroupOperations },
                { provide: NotificationService, useClass: MockNotificationService },
                { provide: PermissionsService, useClass: MockPermissionsService },
                { provide: Router, useClass: MockRouter },
                SubgroupDataService,
                UserDataService,
                { provide: UserOperations, useClass: MockUserOperations },
            ],
            schemas: [NO_ERRORS_SCHEMA],
        }).compileComponents();

        fixture = TestBed.createComponent(TestComponent);
        component = fixture.componentInstance;
        userData = TestBed.inject(UserDataService);
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should return the right data after calling buttonAssignUserToGroupsClicked()',
        componentTest(() => TestComponent, (fixture, instance) => {
            const userDataSpy = spyOn(userData, 'changeGroupsOfUsers').and.returnValue(
                of(MOCK_USER) as any,
            );

            fixture.detectChanges();
            tick();

            instance.buttonAssignUserToGroupsClicked();

            fixture.detectChanges();
            tick();

            expect(userDataSpy).toHaveBeenCalled();
            expect(userDataSpy).toHaveBeenCalledWith(instance.userIds, instance.userGroupIds);
        }),
    );
});
