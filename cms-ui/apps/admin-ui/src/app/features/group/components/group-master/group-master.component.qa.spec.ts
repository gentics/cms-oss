/*
 * QA spec for `GroupMasterComponent`.
 *
 * The only other spec for this component (`group-master.component.spec.ts`) is `xdescribe`-
 * disabled, which is why the fix round reported F2 (permission dependent landing tab) and F6
 * (validation of the persisted `groupViewMode`) as "no unit test". This spec closes exactly that
 * gap without touching or re-enabling the disabled one: the component template is replaced with
 * an empty one, so none of the child components of the disabled spec are needed.
 *
 * Brief mapping:
 * - fix round F2      -> describe('landing tab (fix round F2)')
 * - fix round F6      -> describe('groupViewMode validation (fix round F6)')
 * - developer AC 1    -> describe('view mode toggle (developer AC 1)')
 * - refactoring C2    -> 'list mode navigation stays tab free'
 * - security T8       -> the bogus values in the F6 describe
 */
import { ChangeDetectorRef } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { BehaviorSubject, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { GroupDetailTabs, ROUTE_ENTITY_LOADED, ROUTE_ENTITY_RESOLVER_KEY } from '../../../../common';
import { PermissionsService } from '../../../../core';
import { AppStateService, GROUP_VIEW_MODE_LIST, GROUP_VIEW_MODE_TREE, SetUserSettingAction } from '../../../../state';
import { GroupMasterComponent } from './group-master.component';

const USER_ID = 7;
const GROUP_ID = 5;

function createState(groupViewMode: any, focusEntityId: any = null): any {
    return {
        auth: {
            isLoggedIn: true,
            user: { id: USER_ID },
        },
        ui: {
            focusEntityType: 'group',
            focusEntityId,
            settings: {
                [USER_ID]: { groupViewMode },
            },
        },
    };
}

class MockAppStateService {
    public state$ = new BehaviorSubject<any>(createState(undefined));
    public select = jasmine.createSpy('select').and.callFake((selector: (state: any) => any) =>
        this.state$.asObservable().pipe(map(selector)));

    public dispatch = jasmine.createSpy('dispatch').and.returnValue(of(null));

    public get now(): any {
        return this.state$.value;
    }

    public mock(state: any): void {
        this.state$.next(state);
    }
}

describe('GroupMasterComponent (QA)', () => {

    let fixture: ComponentFixture<GroupMasterComponent>;
    let component: GroupMasterComponent;
    let appState: MockAppStateService;
    let router: jasmine.SpyObj<Router>;
    let permissions: jasmine.SpyObj<PermissionsService>;

    beforeEach(async () => {
        appState = new MockAppStateService();
        router = jasmine.createSpyObj('Router', ['navigate']);
        router.navigate.and.returnValue(Promise.resolve(true));
        permissions = jasmine.createSpyObj('PermissionsService', ['checkPermissions', 'getUserActionPermsForId']);
        permissions.checkPermissions.and.returnValue(of(true));

        const route: any = {
            pathFromRoot: [
                { snapshot: { outlet: 'primary', url: [{ path: 'groups' }] } },
            ],
        };

        await TestBed.configureTestingModule({
            declarations: [GroupMasterComponent],
            providers: [
                { provide: Router, useValue: router },
                { provide: ActivatedRoute, useValue: route },
                { provide: AppStateService, useValue: appState },
                { provide: PermissionsService, useValue: permissions },
                { provide: ChangeDetectorRef, useValue: jasmine.createSpyObj('ChangeDetectorRef', ['markForCheck', 'detectChanges']) },
            ],
        })
            // The real template pulls in gtx-group-table / gtx-group-management-trable and the i18n
            // pipe; none of that is under test here.
            .overrideComponent(GroupMasterComponent, { set: { template: '' } })
            .compileComponents();

        fixture = TestBed.createComponent(GroupMasterComponent);
        component = fixture.componentInstance;
    });

    describe('groupViewMode validation (fix round F6, security T8)', () => {

        function viewModeFor(persisted: any): string {
            appState.mock(createState(persisted));
            fixture.detectChanges();
            return component.viewMode;
        }

        it('restores the tree mode for the exact stored value "tree"', () => {
            expect(viewModeFor('tree')).toBe(GROUP_VIEW_MODE_TREE);
        });

        it('defaults to the list mode when nothing is stored', () => {
            expect(viewModeFor(undefined)).toBe(GROUP_VIEW_MODE_LIST);
        });

        it('falls back to the list mode for any value which is not exactly "tree"', () => {
            const bogusValues: any[] = [
                'treeX',
                'TREE',
                ' tree',
                'list',
                '',
                null,
                0,
                42,
                {},
                { a: 1 },
                [],
                ['tree'],
                true,
                '<script>alert(1)</script>',
            ];

            for (const bogus of bogusValues) {
                // A fresh component per value - the subscription uses distinctUntilChanged.
                const localFixture = TestBed.createComponent(GroupMasterComponent);
                appState.mock(createState(bogus));
                localFixture.detectChanges();

                expect(localFixture.componentInstance.viewMode)
                    .withContext(`persisted value ${JSON.stringify(bogus)}`)
                    .toBe(GROUP_VIEW_MODE_LIST);
                localFixture.destroy();
            }
        });

        it('switches back to the list mode when the stored value turns bogus at runtime', () => {
            expect(viewModeFor('tree')).toBe(GROUP_VIEW_MODE_TREE);

            appState.mock(createState('treeX'));
            fixture.detectChanges();

            expect(component.viewMode).toBe(GROUP_VIEW_MODE_LIST);
        });
    });

    describe('view mode toggle (developer AC 1)', () => {

        it('dispatches SetUserSettingAction("groupViewMode", "tree") when toggling away from the list', () => {
            appState.mock(createState(undefined));
            fixture.detectChanges();
            appState.dispatch.calls.reset();

            component.toggleViewMode();

            expect(appState.dispatch).toHaveBeenCalledTimes(1);
            const action = appState.dispatch.calls.mostRecent().args[0] as SetUserSettingAction<any>;
            expect(action instanceof SetUserSettingAction).toBe(true);
            expect(action.setting).toBe('groupViewMode');
            expect(action.value).toBe(GROUP_VIEW_MODE_TREE);
        });

        it('dispatches the list mode when toggling away from the tree', () => {
            appState.mock(createState('tree'));
            fixture.detectChanges();
            appState.dispatch.calls.reset();

            component.toggleViewMode();

            const action = appState.dispatch.calls.mostRecent().args[0] as SetUserSettingAction<any>;
            expect(action.setting).toBe('groupViewMode');
            expect(action.value).toBe(GROUP_VIEW_MODE_LIST);
        });

        it('toggles back to the tree after a bogus value was persisted (no dead toggle)', () => {
            appState.mock(createState('treeX'));
            fixture.detectChanges();
            appState.dispatch.calls.reset();

            component.toggleViewMode();

            expect((appState.dispatch.calls.mostRecent().args[0] as SetUserSettingAction<any>).value)
                .toBe(GROUP_VIEW_MODE_TREE);
        });
    });

    it('keeps the active entity when the view mode changes (developer AC 12)', fakeAsync(() => {
        appState.mock(createState(undefined, GROUP_ID));
        fixture.detectChanges();
        tick();

        expect(component.viewMode).toBe(GROUP_VIEW_MODE_LIST);
        expect(component.activeEntity).toBe(String(GROUP_ID));

        appState.mock(createState(GROUP_VIEW_MODE_TREE, GROUP_ID));
        fixture.detectChanges();
        tick();

        expect(component.viewMode).toBe(GROUP_VIEW_MODE_TREE);
        expect(component.activeEntity).withContext('selection survives the mode switch').toBe(String(GROUP_ID));
    }));

    describe('landing tab (fix round F2)', () => {

        const row: any = { id: String(GROUP_ID), item: { id: GROUP_ID, name: 'Marketing' } };

        it('lands on the group users tab when USER_ADMIN read is granted', async () => {
            permissions.checkPermissions.and.returnValue(of(true));
            fixture.detectChanges();

            await component.handleTreeRowClick(row);

            expect(permissions.checkPermissions).toHaveBeenCalledWith({
                type: AccessControlledType.USER_ADMIN,
                permissions: GcmsPermission.READ,
            });
            expect(router.navigate).toHaveBeenCalledTimes(1);
            const [commands, extras] = router.navigate.calls.mostRecent().args as any[];
            expect(commands[0]).toBe('/groups');
            expect(commands[1]).toEqual({ outlets: { detail: ['group', String(GROUP_ID), GroupDetailTabs.GROUP_USERS] } });
            expect(extras.state[ROUTE_ENTITY_LOADED]).toBe(true);
            expect(extras.state[ROUTE_ENTITY_RESOLVER_KEY]).toBe(row.item);
        });

        it('lands on the properties tab when USER_ADMIN read is denied', async () => {
            permissions.checkPermissions.and.returnValue(of(false));
            fixture.detectChanges();

            await component.handleTreeRowClick(row);

            const [commands] = router.navigate.calls.mostRecent().args as any[];
            expect(commands[1]).toEqual({ outlets: { detail: ['group', String(GROUP_ID), GroupDetailTabs.PROPERTIES] } });
        });

        it('never navigates to the users tab when the permission observable never grants', async () => {
            permissions.checkPermissions.and.returnValue(of(false, true));
            fixture.detectChanges();

            await component.handleTreeRowClick(row);

            // `take(1)` must win - the first emission decides, no double navigation.
            expect(router.navigate).toHaveBeenCalledTimes(1);
            const [commands] = router.navigate.calls.mostRecent().args as any[];
            expect(commands[1]).toEqual({ outlets: { detail: ['group', String(GROUP_ID), GroupDetailTabs.PROPERTIES] } });
        });

        it('list mode navigation stays tab free (refactoring C2)', fakeAsync(() => {
            fixture.detectChanges();

            void component.handleRowClick({ id: String(GROUP_ID), item: { id: GROUP_ID } } as any);
            tick();

            const [commands] = router.navigate.calls.mostRecent().args as any[];
            expect(commands[1]).toEqual({ outlets: { detail: ['group', String(GROUP_ID)] } });
        }));
    });
});
