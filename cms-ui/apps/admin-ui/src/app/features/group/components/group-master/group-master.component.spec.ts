import { InterfaceOf, ObservableStopper, USER_ACTION_PERMISSIONS, USER_ACTION_PERMISSIONS_DEF } from '@admin-ui/common';
import {
    FileOperations,
    FolderOperations,
    FormOperations,
    GroupOperations,
    ImageOperations,
    NodeOperations,
    PageOperations,
    PermissionsService,
    UserOperations,
} from '@admin-ui/core';
import { EntityManagerService } from '@admin-ui/core/providers/entity-manager';
import { ErrorHandler } from '@admin-ui/core/providers/error-handler';
import { MockErrorHandler } from '@admin-ui/core/providers/error-handler/error-handler.mock';
import { I18nService } from '@admin-ui/core/providers/i18n';
import { I18nNotificationService } from '@admin-ui/core/providers/i18n-notification';
import { MockI18nNotificationService } from '@admin-ui/core/providers/i18n-notification/i18n-notification.service.mock';
import { MockI18nServiceWithSpies } from '@admin-ui/core/providers/i18n/i18n.service.mock';
import {
    GroupDataService,
    GroupUserDataService,
    NodeDataService,
    NotificationService,
    SubgroupDataService,
} from '@admin-ui/shared';
import { GroupTrableComponent } from '@admin-ui/shared/components/group-trable/group-trable.component';
import { LoadingTriggerComponent } from '@admin-ui/shared/components/loading-trigger/loading-trigger.component';
import { ActionAllowedDirective } from '@admin-ui/shared/directives/action-allowed/action-allowed.directive';
import { AppStateService } from '@admin-ui/state';
import { TEST_APP_STATE, TestAppState, assembleTestAppStateImports } from '@admin-ui/state/utils/test-app-state';
import { createDelayedObservable } from '@admin-ui/testing';
import { CommonModule } from '@angular/common';
import { Component, EventEmitter, NO_ERRORS_SCHEMA, Pipe, PipeTransform, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { GcmsPermission, Group, IndexById, ModelType, Normalized } from '@gentics/cms-models';
import { TranslateService } from '@ngx-translate/core';
import { GroupMasterComponent } from './group-master.component';

const ROOT_GROUP_ID = 2;
const NON_ROOT_GROUP_ID = 3;

const MOCK_GROUPS: IndexById<Group<Normalized>> = {
    [ROOT_GROUP_ID]: {
        id: ROOT_GROUP_ID,
        name: 'Node Super Admin',
        description: '',
        children: [ 3 ],
    },
    [NON_ROOT_GROUP_ID]: {
        id: NON_ROOT_GROUP_ID,
        name: 'Admins',
        description: '',
        children: [ 4, 5 ],
    },
    4: {
        id: 4,
        name: 'Gentics',
        description: '',
    },
    5: {
        id: 5,
        name: 'Customer',
        description: '',
        children: [ 6, 8 ],
    },
    6: {
        id: 6,
        name: 'Editors',
        description: '',
        children: [ 7 ],
    },
    7: {
        id: 7,
        name: 'Limited Editors',
        description: '',
    },
    8: {
        id: 8,
        name: 'Translators',
        description: '',
    },
};

const MOCK_WATCH_GROUPS: Group<ModelType>[] = [
    {
        id: ROOT_GROUP_ID,
        name: 'Node Super Admin',
        description: '',
        children: [
            {
                id: NON_ROOT_GROUP_ID,
                name: 'Admins',
                description: '',
            },
        ],
    },
    {
        id: NON_ROOT_GROUP_ID,
        name: 'Admins',
        description: '',
        children: [
            {
                id: 4,
                name: 'Gentics',
                description: '',
            },
            {
                id: 5,
                name: 'Customer',
                description: '',
            },
        ],
    },
    {
        id: 4,
        name: 'Gentics',
        description: '',
        children: [],
    },
    {
        id: 5,
        name: 'Customer',
        description: '',
        children: [
            {
                id: 6,
                name: 'Editors',
                description: '',
            },
            {
                id: 8,
                name: 'Translators',
                description: '',
            },
        ],
    },
    {
        id: 6,
        name: 'Editors',
        description: '',
        children: [
            {
                id: 7,
                name: 'Limited Editors',
                description: '',
            },
        ],
    },
    {
        id: 7,
        name: 'Limited Editors',
        description: '',
        children: [],
    },
    {
        id: 8,
        name: 'Translators',
        description: '',
        children: [],
    },
];

const MOCK_GROUP_PERMISSIONS: {[key: number]: string[]} = {
    2: [GcmsPermission.VIEW, GcmsPermission.EDIT],
    3: [GcmsPermission.VIEW, GcmsPermission.EDIT],
    4: [GcmsPermission.VIEW, GcmsPermission.EDIT],
    5: [GcmsPermission.VIEW, GcmsPermission.EDIT],
    6: [GcmsPermission.VIEW, GcmsPermission.EDIT],
    7: [GcmsPermission.VIEW, GcmsPermission.EDIT],
    8: [GcmsPermission.VIEW, GcmsPermission.EDIT],
}

const PARENT_NODE_ID = 2;
const FIRST_PARENT_NODE_ID = 4;
const SECOND_PARENT_NODE_ID = 5;

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

class MockEntityManagerService {
    watchDenormalizedEntitiesList = jasmine.createSpy('watchDenormalizedEntitiesList').and.callFake(() => createDelayedObservable(MOCK_WATCH_GROUPS));
}

class MockGroupOperations {
    getAll = jasmine.createSpy('getAll').and.callFake(() => createDelayedObservable(MOCK_PARENT_GROUPS));
    getGroupPermissions = jasmine.createSpy('getGroupPermissions').and.callFake(() => createDelayedObservable(MOCK_GROUP_PERMISSIONS));
}

class MockPermissionsService {
    checkPermissions = jasmine.createSpy('checkPermissions').and.callFake(() => createDelayedObservable(null));
}

class MockRouter {}

class MockTranslateService {
    instant = jasmine.createSpy('instant').and.callFake((key: string, params: any) => {
        return `${key}_translated`;
    });
    onDefaultLangChange = new EventEmitter<any>();
    onLangChange = new EventEmitter<any>();
    onTranslationChange = new EventEmitter<any>();
    get = jasmine.createSpy('get').and.callFake(() => createDelayedObservable(null));
}

class MockConstructOperations {}

class MockUserOperations {}

class MockNodeOperations {}

class MockFolderOperations {}

@Pipe({
    name: 'i18n',
    standalone: false,
})
class MockI18nPipe implements PipeTransform {
    transform(key: string, params: object): string {
        return key + (params ? ':' + JSON.stringify(params) : '');
    }
}

function refresh<T>(fixture: ComponentFixture<T>, amount: number = 1): void {
    for (let i = amount; i > 0; i--) {
        fixture.detectChanges();
        tick(101);
    }
}

@Component({
    template: '<gtx-group-master></gtx-group-master>',
    standalone: false,
})
class TestComponent {

    @ViewChild(GroupMasterComponent, { static: false })
    groupMaster: GroupMasterComponent;

    createSubgroupSpy = jasmine.createSpy('createSubgroup').and.stub();
    deleteGroupsSpy = jasmine.createSpy('deleteGroup').and.stub();
    moveGroupsSpy = jasmine.createSpy('moveGroup').and.stub();
    onClickableCellClickSpy = jasmine.createSpy('onClickableCellClickSpy').and.stub();
    openEntityDetailsSpy = jasmine.createSpy('openEntityDetail').and.stub();

    // listActionsCommon: TableActionsMenuConfig<void> = {
    //     buttons: [],
    //     menuItems: [],
    // };

    // listActionsBulk: TableActionsMenuConfig<Group[]> = {
    //     buttons: [
    //         {
    //             icon: 'subdirectory_arrow_right',
    //             tooltip: 'shared.move',
    //             action: (entities) => this.moveGroupsSpy(entities),
    //             allowedActionId: 'group.moveGroup',
    //         },
    //         {
    //             icon: 'delete',
    //             tooltip: 'shared.delete',
    //             action: (entities) => this.deleteGroupsSpy(entities),
    //             allowedActionId: 'group.deleteGroup',
    //         },
    //     ],
    //     menuItems: [],
    // };

    // listActionsSingle: TableActionsMenuConfig<Group> = {
    //     buttons: [
    //         {
    //             icon: 'edit',
    //             tooltip: 'common.edit',
    //             action: (entity) => this.openEntityDetailsSpy(entity.id),
    //         },
    //         {
    //             icon: 'add',
    //             tooltip: 'shared.create_new_sub_group_button',
    //             action: (entity) => this.createSubgroupSpy(entity),
    //             allowedActionId: 'group.createGroup',
    //         },
    //     ],
    //     menuItems: [
    //         {
    //             icon: 'more_vert',
    //             items: [
    //                 {
    //                     icon: 'subdirectory_arrow_right',
    //                     text: 'shared.move',
    //                     action: (entity) => this.moveGroupsSpy([entity]),
    //                     allowedActionId: 'group.moveGroup',
    //                 },
    //                 {
    //                     icon: 'delete',
    //                     text: 'shared.delete',
    //                     action: (entity) => this.deleteGroupsSpy([entity]),
    //                     allowedActionId: 'group.deleteGroup',
    //                 },
    //             ],
    //         },
    //     ],
    // };
}

class MockFileOperations implements Partial<InterfaceOf<FileOperations>> {}
class MockImageOperations implements Partial<InterfaceOf<ImageOperations>> {}
class MockFormOperations implements Partial<InterfaceOf<FormOperations>> {}
class MockPageOperations implements Partial<InterfaceOf<PageOperations>> {}

xdescribe('GroupMasterComponent', () => {
    let appState: TestAppState;
    let component: TestComponent;
    let fixture: ComponentFixture<TestComponent>;
    let stopper: ObservableStopper;

    const FakeAsyncTestZoneSpec = (Zone as any).FakeAsyncTestZoneSpec;
    let testZoneSpec: any;
    let fakeAsyncTestZone: Zone;

    beforeEach(() => {
        testZoneSpec = new FakeAsyncTestZoneSpec('name');
        fakeAsyncTestZone = Zone.current.fork(testZoneSpec);

        stopper = new ObservableStopper();
        TestBed.configureTestingModule({
            imports: [
                CommonModule,
                FormsModule,
                ...assembleTestAppStateImports(),
            ],
            declarations: [
                ActionAllowedDirective,
                GroupMasterComponent,
                GroupTrableComponent,
                LoadingTriggerComponent,
                MockI18nPipe,
                TestComponent,
            ],
            providers: [
                { provide: ActivatedRoute, useClass: MockActivatedRoute },
                { provide: AppStateService, useClass: TestAppState },
                { provide: EntityManagerService, useClass: MockEntityManagerService },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: GroupOperations, useClass: MockGroupOperations },
                { provide: I18nNotificationService, useClass: MockI18nNotificationService },
                { provide: I18nService, useClass: MockI18nServiceWithSpies },
                { provide: PermissionsService, useClass: MockPermissionsService },
                { provide: Router, useClass: MockRouter },
                { provide: TranslateService, useClass: MockTranslateService },
                { provide: USER_ACTION_PERMISSIONS, useValue: USER_ACTION_PERMISSIONS_DEF },
                { provide: UserOperations, useClass: MockUserOperations },
                { provide: NodeOperations, useClass: MockNodeOperations },
                { provide: FolderOperations, useClass: MockFolderOperations },
                { provide: FileOperations, useClass: MockFileOperations },
                { provide: ImageOperations, useClass: MockImageOperations },
                { provide: FormOperations, useClass: MockFormOperations },
                { provide: PageOperations, useClass: MockPageOperations },
                GroupDataService,
                GroupUserDataService,
                NotificationService,
                SubgroupDataService,
                NodeDataService,
                TEST_APP_STATE,
            ],
            schemas: [NO_ERRORS_SCHEMA],
        }).compileComponents();

        appState = TestBed.get(AppStateService);
        fixture = TestBed.createComponent(TestComponent);
        component = fixture.componentInstance;
    });

    beforeEach(() => {
        appState.mockState({
            auth: {
                user: {
                    id: 1,
                } as any,
            },
            entity: {
                group: {
                    ...MOCK_GROUPS,
                },
            },
            ui: {
                settings: {
                    1: { },
                },
            },
        });
    });

    afterEach(() => {
        stopper.stop();
    });

    it('should create', fakeAsync(() => {
        refresh(fixture);
        expect(component).toBeTruthy();
    }));

    // it('should show the right data for tree view if showTrable is true', fakeAsync(() => {
    //     refresh(fixture, 2);

    //     const groupTrable = fixture.debugElement.nativeElement.querySelector('gtx-group-trable');

    //     expect(groupTrable).toBeTruthy();

    //     const entityTrable = component.groupMaster.groupTrable.entityTrable;

    //     const trableData = {
    //         id: entityTrable.dataSource.items()[0].data.id,
    //         name: entityTrable.dataSource.items()[0].data.name,
    //         description: entityTrable.dataSource.items()[0].data.description,
    //         children: entityTrable.dataSource.items()[0].data.children,
    //     };

    //     refresh(fixture, 2);

    //     expect(trableData).toEqual(MOCK_GROUPS[ROOT_GROUP_ID]);
    // }));

    it('should save the right data to appState for list view if showTrable is false', () => {
        fakeAsyncTestZone.run(() => {
            refresh(fixture, 2);

            appState.mockState({
                ui: {
                    settings: {
                        1: {},
                    },
                },
            });

            refresh(fixture, 2);

            const groupList = fixture.debugElement.nativeElement.querySelector('gtx-group-list');

            expect(groupList).toBeTruthy();

            expect(appState.now.entity.group).toEqual(MOCK_GROUPS);
        });
    });
});
