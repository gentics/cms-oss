import { GroupBO } from '@admin-ui/common';
import {
    EntityManagerService,
    ErrorHandler,
    GroupManagementTrableLoaderService,
    GroupOperations,
    PermissionsService,
} from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '@gentics/cms-components';
import { MockI18nPipe } from '@gentics/cms-components/testing';
import { Group, Raw } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { ModalService, TrableRow } from '@gentics/ui-core';
import { of } from 'rxjs';
import { GroupManagementTrableComponent } from './group-management-trable.component';

/**
 * Generous wait for the two 50 ms debounces in play: `BaseEntityTrableComponent.setupActionLoading()`
 * for the action rebuild, and `SEARCH_DEBOUNCE_MS` for the search.
 */
const ACTION_DEBOUNCE_MS = 100;
const DEBOUNCE_SETTLE_MS = 100;

/** Waits for the debounced pipelines to settle. */
function settle(): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, DEBOUNCE_SETTLE_MS));
}

/** Types a query and waits for the debounced filter to run. */
async function search(component: GroupManagementTrableComponent, query: string): Promise<void> {
    component.updateSearchQuery(query);
    await settle();
}

/** Marker object used to tell the granted from the denied type permissions in the mock. */
const GRANTED_PERMISSION = Object.freeze({ granted: true });

const GROUP_ID_ROOT = 1;
const GROUP_ID_MARKETING = 2;
const GROUP_ID_SALES = 3;
const GROUP_ID_SEO = 4;

function createTree(): Group<Raw>[] {
    return [{
        id: GROUP_ID_ROOT,
        name: 'Node Super Admin',
        description: 'The root group',
        children: [
            {
                id: GROUP_ID_MARKETING,
                name: 'Marketing',
                description: 'Marketing department',
                children: [
                    { id: GROUP_ID_SEO, name: 'SEO', description: 'Search engine optimization' },
                ],
            },
            { id: GROUP_ID_SALES, name: 'Sales', description: 'Sales department' },
        ],
    }];
}

/** Flattens the visible tree (regardless of expansion) into a list of row ids. */
function collectIds(rows: TrableRow<GroupBO>[]): string[] {
    return rows.reduce<string[]>((acc, row) => [...acc, row.id, ...collectIds(row.children || [])], []);
}

/** Rows a trable would actually render: every root, plus the children of expanded rows. */
function countRenderedRows(rows: TrableRow<GroupBO>[]): number {
    return rows.reduce((total, row) => total + 1 + (row.expanded ? countRenderedRows(row.children || []) : 0), 0);
}

/**
 * A wide, deep tree in which *every* group matches `"group"`, so the reveal budget has to kick in.
 * 5 roots x 4 children x 4 levels = 425 groups.
 */
function createWideTree(): Group<Raw>[] {
    let nextId = 1000;
    const build = (level: number): Group<Raw> => {
        const id = nextId++;
        const group: Group<Raw> = { id, name: `Group ${id}`, description: `Group ${id} description` };
        if (level < 4) {
            group.children = [build(level + 1), build(level + 1), build(level + 1), build(level + 1)];
        }
        return group;
    };
    return [build(1), build(1), build(1), build(1), build(1)];
}

/** First retained row that has children but was not opened by the reveal budget. */
function findCollapsedParent(rows: TrableRow<GroupBO>[]): TrableRow<GroupBO> | undefined {
    for (const row of rows) {
        if (row.hasChildren && !row.expanded) {
            return row;
        }
        const found = row.expanded ? findCollapsedParent(row.children || []) : undefined;
        if (found) {
            return found;
        }
    }
    return undefined;
}

function findRow(rows: TrableRow<GroupBO>[], id: string): TrableRow<GroupBO> | undefined {
    for (const row of rows) {
        if (row.id === id) {
            return row;
        }
        const found = findRow(row.children || [], id);
        if (found) {
            return found;
        }
    }
    return undefined;
}

describe('GroupManagementTrableComponent', () => {

    let component: GroupManagementTrableComponent;
    let fixture: ComponentFixture<GroupManagementTrableComponent>;
    let groupApi: jasmine.SpyObj<GcmsApi['group']>;
    let entityManager: jasmine.SpyObj<EntityManagerService>;

    beforeEach(async () => {
        groupApi = jasmine.createSpyObj('GroupApi', ['getGroupsTree', 'getGroup']);
        groupApi.getGroupsTree.and.callFake(() => of({ groups: createTree() } as any));

        entityManager = jasmine.createSpyObj('EntityManagerService', ['addEntities']);
        entityManager.addEntities.and.returnValue(Promise.resolve());

        const i18nSpy = jasmine.createSpyObj('I18nService', ['instant']);
        i18nSpy.instant.and.callFake((key: string) => key);

        const permissionsSpy = jasmine.createSpyObj('PermissionsService', ['checkPermissions', 'getUserActionPermsForId']);
        permissionsSpy.getUserActionPermsForId.and.returnValue({ typePermissions: [] });
        permissionsSpy.checkPermissions.and.returnValue(of(true));

        await TestBed.configureTestingModule({
            declarations: [
                GroupManagementTrableComponent,
                MockI18nPipe,
            ],
            providers: [
                GroupManagementTrableLoaderService,
                { provide: GcmsApi, useValue: { group: groupApi } },
                { provide: EntityManagerService, useValue: entityManager },
                { provide: I18nService, useValue: i18nSpy },
                { provide: ModalService, useValue: jasmine.createSpyObj('ModalService', ['dialog', 'fromComponent']) },
                { provide: PermissionsService, useValue: permissionsSpy },
                { provide: ErrorHandler, useValue: jasmine.createSpyObj('ErrorHandler', ['catch']) },
                { provide: GroupOperations, useValue: jasmine.createSpyObj('GroupOperations', ['delete']) },
                { provide: AppStateService, useValue: jasmine.createSpyObj('AppStateService', ['dispatch']) },
            ],
            schemas: [NO_ERRORS_SCHEMA],
        }).compileComponents();

        fixture = TestBed.createComponent(GroupManagementTrableComponent);
        component = fixture.componentInstance;
    });

    it('should create', () => {
        fixture.detectChanges();
        expect(component).toBeTruthy();
    });

    describe('eager loading', () => {

        it('should load the whole tree with exactly one request', () => {
            fixture.detectChanges();

            expect(groupApi.getGroupsTree).toHaveBeenCalledTimes(1);
            expect(collectIds(component.rows)).toEqual([
                String(GROUP_ID_ROOT),
                String(GROUP_ID_MARKETING),
                String(GROUP_ID_SEO),
                String(GROUP_ID_SALES),
            ]);
        });

        it('should mark every row as loaded, so no further request is issued on expansion', () => {
            fixture.detectChanges();

            const allLoaded = (rows: TrableRow<GroupBO>[]): boolean =>
                rows.every((row) => row.loaded && allLoaded(row.children || []));

            expect(allLoaded(component.rows)).toBe(true);
        });

        it('should put every group of the tree into the entity state, flat and without children', () => {
            fixture.detectChanges();

            expect(entityManager.addEntities).toHaveBeenCalledTimes(1);
            const [type, entities] = entityManager.addEntities.calls.mostRecent().args;
            expect(type).toBe('group');
            expect((entities as Group<Raw>[]).map((group) => group.id))
                .toEqual([GROUP_ID_ROOT, GROUP_ID_MARKETING, GROUP_ID_SEO, GROUP_ID_SALES]);
            expect((entities as Group<Raw>[]).every((group) => group.children === undefined)).toBe(true);
        });

        it('should not report children for leaf groups', () => {
            fixture.detectChanges();

            expect(findRow(component.rows, String(GROUP_ID_ROOT)).hasChildren).toBe(true);
            expect(findRow(component.rows, String(GROUP_ID_MARKETING)).hasChildren).toBe(true);
            expect(findRow(component.rows, String(GROUP_ID_SEO)).hasChildren).toBe(false);
            expect(findRow(component.rows, String(GROUP_ID_SALES)).hasChildren).toBe(false);
        });
    });

    describe('row actions', () => {

        it('should offer create, move and delete when all permissions are granted', async () => {
            fixture.detectChanges();
            await new Promise((resolve) => setTimeout(resolve, ACTION_DEBOUNCE_MS));

            expect(component.actions.map((action) => action.id)).toEqual(['createSubGroup', 'move', 'delete']);
            expect(component.actions.every((action) => action.single)).toBe(true);
        });

        it('should drop actions the user is not permitted to perform', async () => {
            const permissions = TestBed.inject(PermissionsService) as jasmine.SpyObj<PermissionsService>;
            permissions.checkPermissions.and.callFake(
                // Only `group.moveGroup` is granted; the argument is the type-permission marker below.
                (required: any) => of(required === GRANTED_PERMISSION),
            );
            permissions.getUserActionPermsForId.and.callFake((actionId: string) => ({
                typePermissions: actionId === 'group.moveGroup' ? GRANTED_PERMISSION : [],
            }) as any);

            fixture.detectChanges();
            await new Promise((resolve) => setTimeout(resolve, ACTION_DEBOUNCE_MS));

            expect(component.actions.map((action) => action.id)).toEqual(['move']);
        });
    });

    describe('deeplink', () => {

        it('should expand all ancestors of the active entity when the tree is loaded', () => {
            component.activeEntity = String(GROUP_ID_SEO);
            fixture.detectChanges();

            expect(findRow(component.rows, String(GROUP_ID_ROOT)).expanded).toBe(true);
            expect(findRow(component.rows, String(GROUP_ID_MARKETING)).expanded).toBe(true);
            expect(findRow(component.rows, String(GROUP_ID_SEO)).expanded).toBe(false);
        });

        it('should expand all ancestors when the active entity arrives after the tree', () => {
            fixture.detectChanges();
            expect(findRow(component.rows, String(GROUP_ID_MARKETING)).expanded).toBe(false);

            component.activeEntity = String(GROUP_ID_SEO);
            component.ngOnChanges({
                activeEntity: {
                    currentValue: String(GROUP_ID_SEO),
                    previousValue: undefined,
                    firstChange: false,
                    isFirstChange: () => false,
                },
            });

            expect(findRow(component.rows, String(GROUP_ID_ROOT)).expanded).toBe(true);
            expect(findRow(component.rows, String(GROUP_ID_MARKETING)).expanded).toBe(true);
        });
    });

    describe('client-side search', () => {

        beforeEach(() => {
            fixture.detectChanges();
            groupApi.getGroupsTree.calls.reset();
        });

        it('should keep the ancestors of a match and drop everything else', async () => {
            await search(component, 'seo');

            expect(collectIds(component.rows)).toEqual([
                String(GROUP_ID_ROOT),
                String(GROUP_ID_MARKETING),
                String(GROUP_ID_SEO),
            ]);
        });

        it('should auto-expand the path to a match', async () => {
            await search(component, 'seo');

            expect(findRow(component.rows, String(GROUP_ID_ROOT)).expanded).toBe(true);
            expect(findRow(component.rows, String(GROUP_ID_MARKETING)).expanded).toBe(true);
        });

        it('should not report a truncation for a result set that fits', async () => {
            await search(component, 'seo');

            expect(component.searchMatchCount).toBe(1);
            expect(component.searchVisibleMatchCount).toBe(1);
            expect(component.searchTruncated).toBe(false);
        });

        it('should match the description as well', async () => {
            await search(component, 'sales department');

            expect(collectIds(component.rows)).toEqual([
                String(GROUP_ID_ROOT),
                String(GROUP_ID_SALES),
            ]);
        });

        it('should match case-insensitively', async () => {
            await search(component, 'MARKETING');

            expect(collectIds(component.rows)).toContain(String(GROUP_ID_MARKETING));
        });

        it('should not issue any additional request', async () => {
            await search(component, 'seo');
            await search(component, 'sales');
            await search(component, '');

            expect(groupApi.getGroupsTree).not.toHaveBeenCalled();
        });

        it('should restore the previous expansion state when the query is cleared', async () => {
            findRow(component.rows, String(GROUP_ID_ROOT)).expanded = true;
            findRow(component.rows, String(GROUP_ID_MARKETING)).expanded = false;

            await search(component, 'seo');
            await search(component, '');

            expect(findRow(component.rows, String(GROUP_ID_ROOT)).expanded).toBe(true);
            expect(findRow(component.rows, String(GROUP_ID_MARKETING)).expanded).toBe(false);
            expect(collectIds(component.rows).length).toBe(4);
        });

        it('should not filter for a query shorter than two characters', async () => {
            await search(component, 's');

            expect(collectIds(component.rows).length).toBe(4);
        });

        it('should restore the full tree when the query falls back below two characters', async () => {
            findRow(component.rows, String(GROUP_ID_MARKETING)).expanded = false;

            await search(component, 'seo');
            expect(collectIds(component.rows).length).toBe(3);

            await search(component, 's');

            expect(collectIds(component.rows).length).toBe(4);
            expect(findRow(component.rows, String(GROUP_ID_MARKETING)).expanded).toBe(false);
        });

        it('should filter only once for a burst of keystrokes', async () => {
            const applyQuery = spyOn(component as any, 'applyQuery').and.callThrough();

            component.updateSearchQuery('s');
            component.updateSearchQuery('se');
            component.updateSearchQuery('seo');
            await settle();

            expect(applyQuery).toHaveBeenCalledTimes(1);
            expect(applyQuery).toHaveBeenCalledWith('seo');
        });

        it('should keep the row object identity across filter runs', async () => {
            await search(component, 'se');
            const first = findRow(component.rows, String(GROUP_ID_SEO));

            await search(component, 'seo');
            const second = findRow(component.rows, String(GROUP_ID_SEO));

            expect(second).toBe(first);
        });
    });

    describe('reveal budget on a large result set (QA blocker, fix round 2)', () => {

        /** Mirrors `MAX_VISIBLE_SEARCH_ROWS` in the component. */
        const MAX_VISIBLE_SEARCH_ROWS = 100;

        beforeEach(() => {
            groupApi.getGroupsTree.and.callFake(() => of({ groups: createWideTree() } as any));
            fixture.detectChanges();
        });

        it('keeps the rendered rows within the budget even when everything matches', async () => {
            await search(component, 'group');

            expect(collectIds(component.rows).length).toBe(425);
            expect(countRenderedRows(component.rows)).toBeLessThanOrEqual(MAX_VISIBLE_SEARCH_ROWS);
        });

        it('reports the total match count and how much of it is shown', async () => {
            await search(component, 'group');

            expect(component.searchMatchCount).toBe(425);
            expect(component.searchVisibleMatchCount).toBe(countRenderedRows(component.rows));
            expect(component.searchVisibleMatchCount).toBeLessThan(component.searchMatchCount);
            expect(component.searchTruncated).toBe(true);
        });

        it('leaves the matches beyond the budget collapsed but manually reachable', async () => {
            await search(component, 'group');

            const collapsedParent = findCollapsedParent(component.rows);
            expect(collapsedParent).withContext('a retained but unexpanded parent exists').toBeTruthy();
            // `hasChildren` is what makes gtx-trable draw the expander.
            expect(collapsedParent.hasChildren).toBe(true);
            expect(collapsedParent.children.length).toBeGreaterThan(0);
        });

        it('reveals the first matches in document order', async () => {
            await search(component, 'group');

            // The very first root is the first match in document order, so it must be open.
            expect(component.rows[0].expanded).toBe(true);
            // The last root cannot fit any more and stays closed.
            expect(component.rows[component.rows.length - 1].expanded).toBe(false);
        });

        it('clears the truncation info when the query drops below the minimum length', async () => {
            await search(component, 'group');
            expect(component.searchTruncated).toBe(true);

            await search(component, 'g');

            expect(component.searchTruncated).toBe(false);
            expect(component.searchMatchCount).toBe(0);
            expect(component.searchVisibleMatchCount).toBe(0);
        });
    });

    describe('search scope (design decision D5)', () => {

        /** Ids in the shared fixture are single digits, which is below the search threshold. */
        const GROUP_ID_TWO_DIGIT = 42;

        it('should not match the group id', async () => {
            groupApi.getGroupsTree.and.callFake(() => of({
                groups: [{ id: GROUP_ID_TWO_DIGIT, name: 'Marketing', description: 'Marketing department' }],
            } as any));
            fixture.detectChanges();

            await search(component, String(GROUP_ID_TWO_DIGIT));

            expect(component.rows).toEqual([]);
        });
    });

    describe('reload', () => {

        beforeEach(() => {
            fixture.detectChanges();
            groupApi.getGroupsTree.calls.reset();
        });

        it('should re-run exactly one tree request and never touch the single-group endpoint', () => {
            component.reload();

            expect(groupApi.getGroupsTree).toHaveBeenCalledTimes(1);
            expect(groupApi.getGroup).not.toHaveBeenCalled();
        });

        it('should keep the expanded nodes expanded and not collapse any subtree into a leaf', () => {
            findRow(component.rows, String(GROUP_ID_ROOT)).expanded = true;
            findRow(component.rows, String(GROUP_ID_MARKETING)).expanded = true;

            component.reload();

            expect(findRow(component.rows, String(GROUP_ID_ROOT)).expanded).toBe(true);
            expect(findRow(component.rows, String(GROUP_ID_MARKETING)).expanded).toBe(true);
            expect(findRow(component.rows, String(GROUP_ID_MARKETING)).hasChildren).toBe(true);
            expect(collectIds(component.rows).length).toBe(4);
        });

        it('should not re-expand nodes which were collapsed before the reload', () => {
            findRow(component.rows, String(GROUP_ID_ROOT)).expanded = true;

            component.reload();

            expect(findRow(component.rows, String(GROUP_ID_MARKETING)).expanded).toBe(false);
        });
    });
});
