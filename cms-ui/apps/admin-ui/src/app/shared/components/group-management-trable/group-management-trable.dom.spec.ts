/*
 * QA spec for `GroupManagementTrableComponent` which renders the **real** `gtx-trable`.
 *
 * The developer's own spec uses `NO_ERRORS_SCHEMA`, so `gtx-trable` is never instantiated there
 * and nothing about the actual DOM is covered. This spec fills that gap and settles the open
 * disagreement between the performance review ("the identity cache stops the DOM churn") and the
 * fix round ("it only helps while the result set is stable, because ui-core's `createRowId`
 * folds all expanded descendants into the root's track key").
 *
 * Brief mapping:
 * - fix round F1 (c)          -> describe('DOM churn while filtering (fix round F1c)')
 * - performance criteria 4/6  -> describe('search cost on a large tree (performance brief)')
 * - developer AC 9            -> 'leaf rows render no expander'
 * - security T1/T2, D-e       -> describe('row action buttons in the real DOM')
 * - fix round 2 (row budget)  -> describe('reveal budget in the real DOM (fix round 2)')
 */
import { GroupBO } from '@admin-ui/common';
import {
    EntityManagerService,
    ErrorHandler,
    GroupManagementTrableLoaderService,
    GroupOperations,
    PermissionsService,
} from '@admin-ui/core';
import { AppStateService } from '../../../state/providers/app-state/app-state.service';
import { Component, NO_ERRORS_SCHEMA, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '@gentics/cms-components';
import { MockI18nPipe } from '@gentics/cms-components/testing';
import { Group, Raw } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { GenticsUICoreModule, ModalService } from '@gentics/ui-core';
import { NEVER, config as rxjsConfig, of, throwError } from 'rxjs';
import { GroupManagementTrableComponent } from './group-management-trable.component';

/** Both the search and the action rebuild are debounced by 50 ms in the component. */
const DEBOUNCE_SETTLE_MS = 120;

const GROUP_ID_ROOT = 1;
const GROUP_ID_MARKETING = 2;
const GROUP_ID_SALES = 3;
const GROUP_ID_SEO = 4;

function settle(): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, DEBOUNCE_SETTLE_MS));
}

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

/**
 * Builds a synthetic tree of `roots * (branching ^ 0 + ... )` groups, shaped like the one the
 * performance brief asks for (10 roots x 4 children x 4 levels = 3410 groups).
 */
function createLargeTree(roots: number, branching: number, depth: number): { tree: Group<Raw>[]; count: number } {
    let nextId = 1;

    const build = (level: number): Group<Raw> => {
        const id = nextId++;
        const group: Group<Raw> = {
            id,
            name: `Group ${id} level ${level}`,
            description: `Description of group ${id}`,
        };
        if (level < depth) {
            group.children = [];
            for (let i = 0; i < branching; i++) {
                group.children.push(build(level + 1));
            }
        }
        return group;
    };

    const tree: Group<Raw>[] = [];
    for (let i = 0; i < roots; i++) {
        tree.push(build(1));
    }

    return { tree, count: nextId - 1 };
}

@Component({
    template: `<gtx-group-management-trable
        [inlineExpansion]="true"
        [activeEntity]="activeEntity"
    ></gtx-group-management-trable>`,
    standalone: false,
})
class TestHostComponent {
    public activeEntity: string = null;

    @ViewChild(GroupManagementTrableComponent, { static: true })
    public trable: GroupManagementTrableComponent;
}

describe('GroupManagementTrableComponent (real DOM)', () => {

    let fixture: ComponentFixture<TestHostComponent>;
    let host: TestHostComponent;
    let component: GroupManagementTrableComponent;
    let groupApi: jasmine.SpyObj<GcmsApi['group']>;
    let permissions: jasmine.SpyObj<PermissionsService>;

    function rowElement(id: number | string): HTMLElement {
        return fixture.nativeElement.querySelector(`gtx-trable .data-row[data-id="${id}"]`);
    }

    function visibleRowIds(): string[] {
        return Array.from<HTMLElement>(fixture.nativeElement.querySelectorAll('gtx-trable .data-row'))
            .map((el) => el.getAttribute('data-id'));
    }

    async function search(query: string): Promise<void> {
        component.updateSearchQuery(query);
        await settle();
        fixture.detectChanges();
    }

    beforeEach(async () => {
        groupApi = jasmine.createSpyObj('GroupApi', ['getGroupsTree', 'getGroup']);
        groupApi.getGroupsTree.and.callFake(() => of({ groups: createTree() } as any));

        const entityManager = jasmine.createSpyObj('EntityManagerService', ['addEntities']);
        entityManager.addEntities.and.returnValue(Promise.resolve());

        const i18nSpy = jasmine.createSpyObj('I18nService', ['instant']);
        i18nSpy.instant.and.callFake((key: string) => key);

        permissions = jasmine.createSpyObj('PermissionsService', ['checkPermissions', 'getUserActionPermsForId']);
        permissions.getUserActionPermsForId.and.returnValue({ typePermissions: [] } as any);
        permissions.checkPermissions.and.returnValue(of(true));

        await TestBed.configureTestingModule({
            imports: [
                GenticsUICoreModule.forRoot(),
            ],
            declarations: [
                GroupManagementTrableComponent,
                MockI18nPipe,
                TestHostComponent,
            ],
            providers: [
                GroupManagementTrableLoaderService,
                { provide: GcmsApi, useValue: { group: groupApi } },
                { provide: EntityManagerService, useValue: entityManager },
                { provide: I18nService, useValue: i18nSpy },
                { provide: ModalService, useValue: jasmine.createSpyObj('ModalService', ['dialog', 'fromComponent']) },
                { provide: PermissionsService, useValue: permissions },
                { provide: ErrorHandler, useValue: jasmine.createSpyObj('ErrorHandler', ['catch']) },
                { provide: GroupOperations, useValue: jasmine.createSpyObj('GroupOperations', ['delete']) },
                { provide: AppStateService, useValue: jasmine.createSpyObj('AppStateService', ['dispatch']) },
            ],
            // `gtx-icon` is an admin-ui component and irrelevant here.
            schemas: [NO_ERRORS_SCHEMA],
        }).compileComponents();

        fixture = TestBed.createComponent(TestHostComponent);
        host = fixture.componentInstance;
        component = host.trable;
    });

    it('renders the whole loaded tree through the real gtx-trable', () => {
        fixture.detectChanges();

        expect(rowElement(GROUP_ID_ROOT)).withContext('root row is rendered').toBeTruthy();
        // Children are only in the DOM once their parent is expanded.
        component.updateRowExpansion({ row: component.rows[0], expanded: true } as any);
        fixture.detectChanges();

        expect(visibleRowIds()).toEqual([String(GROUP_ID_ROOT), String(GROUP_ID_MARKETING), String(GROUP_ID_SALES)]);
    });

    it('leaf rows render no expander, inner rows do (developer AC 9)', () => {
        fixture.detectChanges();
        component.updateRowExpansion({ row: component.rows[0], expanded: true } as any);
        fixture.detectChanges();

        const marketing = rowElement(GROUP_ID_MARKETING);
        const sales = rowElement(GROUP_ID_SALES);

        expect(marketing.querySelector('.row-expansion icon')).withContext('Marketing has children').toBeTruthy();
        expect(sales.querySelector('.row-expansion icon')).withContext('Sales is a leaf').toBeNull();
    });

    it('renders the expander inline in the name column and marks the nesting level', () => {
        fixture.detectChanges();
        component.updateRowExpansion({ row: component.rows[0], expanded: true } as any);
        fixture.detectChanges();

        const root = rowElement(GROUP_ID_ROOT);
        const marketing = rowElement(GROUP_ID_MARKETING);

        // `[inlineExpansion]="true"` - no separate expand column, the toggle sits in the first
        // (name) cell, which is what produces the visible indentation.
        expect(root.querySelector('.expand-column')).withContext('no separate expand column').toBeNull();
        expect(root.querySelector('.data-column[data-id="name"] .row-expansion')).toBeTruthy();
        expect(root.classList).toContain('row-level-0');
        expect(marketing.classList).toContain('row-level-1');
        expect(root.querySelector('.data-column[data-id="name"] .name').textContent.trim()).toBe('Node Super Admin');
    });

    it('expands the ancestors of a deeplinked group and highlights it (developer AC 3 and AC 11)', () => {
        host.activeEntity = String(GROUP_ID_SEO);
        fixture.detectChanges();

        expect(visibleRowIds()).toEqual([
            String(GROUP_ID_ROOT),
            String(GROUP_ID_MARKETING),
            String(GROUP_ID_SEO),
            String(GROUP_ID_SALES),
        ]);
        expect(rowElement(GROUP_ID_SEO).classList).withContext('deeplinked row is highlighted').toContain('active');
        expect(rowElement(GROUP_ID_MARKETING).classList).not.toContain('active');
    });

    describe('untrusted group data in the DOM (security T6, T11)', () => {

        const XSS_PAYLOAD = '<img src=x onerror="window.xssExecuted = 1">';
        const PAYLOAD_GROUP_ID = 99;

        beforeEach(() => {
            delete (window as any).xssExecuted;
            groupApi.getGroupsTree.and.callFake(() => of({
                groups: [{
                    id: PAYLOAD_GROUP_ID,
                    name: XSS_PAYLOAD,
                    description: XSS_PAYLOAD,
                }],
            } as any));
        });

        afterEach(() => {
            delete (window as any).xssExecuted;
        });

        it('renders a group name and description containing HTML as literal text', () => {
            fixture.detectChanges();

            const row = rowElement(PAYLOAD_GROUP_ID);
            expect(row).toBeTruthy();
            expect(row.querySelector('img')).withContext('no img element was created').toBeNull();
            expect(row.textContent).toContain(XSS_PAYLOAD);
            expect((window as any).xssExecuted).toBeUndefined();
        });

        it('puts the group name into the dialog title and keeps the dialog body free of it (deviation D-f)', async () => {
            const modalService = TestBed.inject(ModalService) as jasmine.SpyObj<ModalService>;
            modalService.dialog.and.returnValue(Promise.resolve({ open: () => Promise.resolve(false) } as any));
            const i18n = TestBed.inject(I18nService) as jasmine.SpyObj<I18nService>;

            fixture.detectChanges();
            await settle();

            component.handleActionClick({
                actionId: 'delete',
                item: component.rows[0].item,
                selection: false,
            } as any);
            await settle();

            expect(modalService.dialog).toHaveBeenCalledTimes(1);
            const [config] = modalService.dialog.calls.mostRecent().args as any[];
            expect(config.body).withContext('the body is rendered with bypassSecurityTrustHtml').not.toContain('<img');
            expect(config.body).toBe('modal.confirm_delete_group_message');
            // The payload only ever travels as an i18n parameter of the safely interpolated title.
            expect(i18n.instant).toHaveBeenCalledWith('modal.confirm_delete_group_title', { groupName: XSS_PAYLOAD });
            expect((window as any).xssExecuted).toBeUndefined();
        });
    });

    describe('search input fuzzing (security T11)', () => {

        // Every payload is at least two characters long - shorter queries deliberately do not
        // filter at all (MIN_SEARCH_QUERY_LENGTH), which is covered by the developer's own spec.
        const PAYLOADS = [
            '<script>alert(1)</script>',
            '%00',
            '../../etc/passwd',
            '\' OR 1=1 --',
            '(((((((((((a+)+)+)+',
            '.*',
            '[a-z]+',
            '\\\\',
            'a'.repeat(10000),
        ];

        it('filters purely client side, never throws and issues no request', async () => {
            fixture.detectChanges();
            await settle();
            groupApi.getGroupsTree.calls.reset();
            groupApi.getGroup.calls.reset();

            for (const payload of PAYLOADS) {
                const start = performance.now();
                await search(payload);
                const duration = performance.now() - start - DEBOUNCE_SETTLE_MS;

                expect(component.rows).withContext(`payload ${payload.slice(0, 20)} yields no match`).toEqual([]);
                expect(duration).withContext(`payload ${payload.slice(0, 20)} does not freeze the input`).toBeLessThan(1000);
            }

            expect(groupApi.getGroupsTree).not.toHaveBeenCalled();
            expect(groupApi.getGroup).not.toHaveBeenCalled();

            // And the tree comes back unharmed once the query is cleared.
            await search('');
            expect(visibleRowIds()).toEqual([String(GROUP_ID_ROOT)]);
        }, 30000);

        it('keeps a trailing space in the query but trims it for matching', async () => {
            fixture.detectChanges();
            await settle();

            await search('marketing ');

            expect(component.query).toBe('marketing ');
            expect(visibleRowIds()).toEqual([String(GROUP_ID_ROOT), String(GROUP_ID_MARKETING)]);
        });
    });

    describe('DOM churn while filtering (fix round F1c vs. performance review)', () => {

        it('keeps the DOM of every row alive when the filter result does not change shape', async () => {
            fixture.detectChanges();
            await settle();

            // "se" and "seo" both yield root > Marketing > SEO.
            await search('se');
            const idsBefore = visibleRowIds();
            const rootBefore = rowElement(GROUP_ID_ROOT);
            const marketingBefore = rowElement(GROUP_ID_MARKETING);
            const seoBefore = rowElement(GROUP_ID_SEO);

            await search('seo');

            expect(visibleRowIds()).withContext('same result set').toEqual(idsBefore);
            expect(rowElement(GROUP_ID_ROOT)).withContext('root DOM element reused').toBe(rootBefore);
            expect(rowElement(GROUP_ID_MARKETING)).withContext('child DOM element reused').toBe(marketingBefore);
            expect(rowElement(GROUP_ID_SEO)).withContext('grandchild DOM element reused').toBe(seoBefore);
        });

        it('re-creates the DOM of the surviving root when the filter result changes shape (ui-core createRowId)', async () => {
            fixture.detectChanges();
            await settle();

            await search('seo');
            const rootBefore = rowElement(GROUP_ID_ROOT);
            expect(visibleRowIds()).toEqual([String(GROUP_ID_ROOT), String(GROUP_ID_MARKETING), String(GROUP_ID_SEO)]);

            await search('sales');

            expect(visibleRowIds()).toEqual([String(GROUP_ID_ROOT), String(GROUP_ID_SALES)]);
            // The root survives the filter change, but its track id changed with its descendants,
            // so Angular tore the embedded view down and rebuilt it. This is the behaviour the fix
            // round predicted and the performance review's wording did not.
            expect(rowElement(GROUP_ID_ROOT)).not.toBe(rootBefore);
        });
    });

    describe('row action buttons in the real DOM (security T1, deviation D-e)', () => {

        it('renders all three action buttons when every permission is granted', async () => {
            fixture.detectChanges();
            await settle();
            fixture.detectChanges();

            const buttons = rowElement(GROUP_ID_ROOT).querySelectorAll('.action-column gtx-button[data-id]');
            expect(Array.from<Element>(buttons).map((el) => el.getAttribute('data-id')))
                .toEqual(['createSubGroup', 'move', 'delete']);
        });

        it('renders no action button at all while the permissions have not answered yet (no optimistic render)', () => {
            // `checkPermissions` never emits.
            permissions.checkPermissions.and.returnValue(NEVER);
            fixture.detectChanges();

            expect(component.actions).toEqual([]);
            expect(rowElement(GROUP_ID_ROOT).querySelectorAll('.action-column gtx-button[data-id]').length).toBe(0);
        });

        it('renders no action button when the permission check errors (fail closed, security T2)', async () => {
            // `BaseEntityTrableComponent.setupActionLoading()` subscribes without an error
            // callback (`base-entity-trable.component.ts:120-132`), so the error escapes as an
            // unhandled rxjs error. Capture it instead of letting it fail an unrelated spec, and
            // assert both facts: nothing is rendered (fail closed) AND the error is unhandled.
            const unhandled: any[] = [];
            const previousHandler = rxjsConfig.onUnhandledError;
            rxjsConfig.onUnhandledError = (err) => unhandled.push(err);

            try {
                permissions.checkPermissions.and.returnValue(throwError(() => new Error('boom')));
                fixture.detectChanges();
                await settle();
                fixture.detectChanges();

                expect(component.actions || []).toEqual([]);
                expect(rowElement(GROUP_ID_ROOT).querySelectorAll('.action-column gtx-button[data-id]').length).toBe(0);
                expect(unhandled.length)
                    .withContext('the permission error is not handled by the base class - QA finding, not a wanted behaviour')
                    .toBeGreaterThan(0);
            } finally {
                rxjsConfig.onUnhandledError = previousHandler;
            }
        });

        it('omits the delete button entirely when delete is denied - it is not merely disabled', async () => {
            const granted = Object.freeze({ granted: true });
            permissions.getUserActionPermsForId.and.callFake((actionId: string) => ({
                typePermissions: actionId === 'group.deleteGroup' ? [] : granted,
            }) as any);
            permissions.checkPermissions.and.callFake((required: any) => of(required === granted));

            fixture.detectChanges();
            await settle();
            fixture.detectChanges();

            const ids = Array.from<Element>(rowElement(GROUP_ID_ROOT).querySelectorAll('.action-column gtx-button[data-id]'))
                .map((el) => el.getAttribute('data-id'));
            expect(ids).toEqual(['createSubGroup', 'move']);
            expect(ids).not.toContain('delete');
        });
    });

    describe('search cost on a large tree (performance brief criteria 4 and 6)', () => {

        const LARGE = createLargeTree(10, 4, 5);

        beforeEach(() => {
            groupApi.getGroupsTree.and.callFake(() => of({ groups: LARGE.tree } as any));
        });

        it('loads the large fixture with exactly one request', () => {
            const start = performance.now();
            fixture.detectChanges();
            const duration = performance.now() - start;

            expect(LARGE.count).toBeGreaterThan(3000);
            expect(groupApi.getGroupsTree).toHaveBeenCalledTimes(1);
            console.log(`[QA-PERF] groups=${LARGE.count} initial load+render=${duration.toFixed(1)} ms rootRows=${component.rows.length}`);
        });

        /**
         * Measures the two blocking parts of one keystroke separately:
         * `filterMs` is the debounced `applyQuery()` (pure JS), `renderMs` is the change
         * detection run that materialises the result in the DOM. The debounce only decides *how
         * often* this pair runs, never how long it takes, so it is bypassed on purpose here.
         */
        let lastFilterMs = 0;

        /** Times the debounced `applyQuery()` from the inside. Install once per spec. */
        function instrumentFilter(): void {
            const original = (component as any).applyQuery.bind(component);
            spyOn(component as any, 'applyQuery').and.callFake((typed: string) => {
                const start = performance.now();
                original(typed);
                lastFilterMs = performance.now() - start;
            });
        }

        async function measureKeystroke(query: string): Promise<{ query: string; filterMs: number; renderMs: number; rows: number }> {
            lastFilterMs = 0;
            component.updateSearchQuery(query);
            await settle();

            const renderStart = performance.now();
            fixture.detectChanges();
            const renderMs = performance.now() - renderStart;

            return { query, filterMs: lastFilterMs, renderMs, rows: visibleRowIds().length };
        }

        it('issues no request for any keystroke (performance criterion 4)', async () => {
            fixture.detectChanges();
            await settle();
            groupApi.getGroupsTree.calls.reset();
            groupApi.getGroup.calls.reset();

            for (const query of ['le', 'lev', 'leve']) {
                component.updateSearchQuery(query);
                await settle();
                fixture.detectChanges();
            }

            expect(groupApi.getGroupsTree).not.toHaveBeenCalled();
            expect(groupApi.getGroup).not.toHaveBeenCalled();
        }, 60000);

        it('stays below the 200 ms budget for every keystroke (performance criterion 6)', async () => {
            fixture.detectChanges();
            await settle();
            instrumentFilter();

            const measurements: { query: string; filterMs: number; renderMs: number; rows: number }[] = [];
            for (const query of ['le', 'lev', 'leve', 'level', 'level 5']) {
                measurements.push(await measureKeystroke(query));
            }

            console.log(`[QA-PERF] groups=${LARGE.count} ${measurements.map((m) =>
                `"${m.query}": filter=${m.filterMs.toFixed(1)}ms render=${m.renderMs.toFixed(1)}ms rowsInDom=${m.rows}`).join(' | ')}`);

            for (const measurement of measurements) {
                expect(measurement.filterMs + measurement.renderMs)
                    .withContext(`keystroke "${measurement.query}" (filter ${measurement.filterMs.toFixed(1)} ms + render ${measurement.renderMs.toFixed(1)} ms, ${measurement.rows} rows in the DOM)`)
                    .toBeLessThan(200);
            }
        }, 120000);

        /* ---- fix round 2: the row budget, its hint, and what it does *not* bound ---- */

        it('shows the truncation hint only when the result is truncated, with honest counts', async () => {
            const i18nSpy = spyOn(MockI18nPipe.prototype, 'transform').and.callThrough();
            fixture.detectChanges();
            await settle();

            // Everything matches "le" -> far more matches than the budget.
            await search('le');

            const hint = fixture.nativeElement.querySelector('[data-id="search-truncated"]');
            expect(hint).withContext('hint is rendered').toBeTruthy();
            expect(component.searchTruncated).toBe(true);
            expect(component.searchMatchCount).toBe(LARGE.count);
            expect(component.searchVisibleMatchCount).toBeLessThan(component.searchMatchCount);

            // The counts the user sees must match what is actually on screen. Every group in this
            // fixture matches, so every rendered row is a visible match.
            expect(component.searchVisibleMatchCount).toBe(visibleRowIds().length);
            expect(i18nSpy).toHaveBeenCalledWith('shared.search_results_truncated', {
                visible: component.searchVisibleMatchCount,
                total: component.searchMatchCount,
            });
        });

        it('hides the hint again when the query drops below the minimum length and when it is cleared', async () => {
            fixture.detectChanges();
            await settle();

            await search('le');
            expect(fixture.nativeElement.querySelector('[data-id="search-truncated"]')).toBeTruthy();

            await search('l');
            expect(fixture.nativeElement.querySelector('[data-id="search-truncated"]'))
                .withContext('below the minimum length').toBeNull();
            expect(component.searchTruncated).toBe(false);

            await search('le');
            expect(fixture.nativeElement.querySelector('[data-id="search-truncated"]')).toBeTruthy();

            await search('');
            expect(fixture.nativeElement.querySelector('[data-id="search-truncated"]'))
                .withContext('query cleared').toBeNull();
        });

        it('is a row budget, not a match limit: a single deep match is still revealed without a hint', async () => {
            fixture.detectChanges();
            await settle();

            // Unique name deep inside the tree (level 5 leaf).
            const deepId = LARGE.count;
            await search(`group ${deepId} level`);

            expect(component.searchMatchCount).withContext('exactly one match').toBe(1);
            expect(component.searchTruncated).toBe(false);
            expect(fixture.nativeElement.querySelector('[data-id="search-truncated"]')).toBeNull();
            expect(rowElement(deepId)).withContext('the deep match is rendered').toBeTruthy();
            expect(visibleRowIds().length)
                .withContext('only the path plus its siblings, nowhere near the whole tree')
                .toBeLessThan(100);
        });

        it('leaves rows beyond the budget collapsed but expandable by hand', async () => {
            fixture.detectChanges();
            await settle();
            await search('le');

            const rowsAfterSearch = visibleRowIds().length;
            // The last root cannot fit into the budget, so it must still offer its expander.
            const lastRootId = component.rows[component.rows.length - 1].id;
            const lastRoot = rowElement(lastRootId);
            expect(lastRoot.querySelector('.row-expansion icon')).withContext('expander is drawn').toBeTruthy();

            component.updateRowExpansion({ row: component.rows[component.rows.length - 1], expanded: true } as any);
            fixture.detectChanges();

            expect(visibleRowIds().length)
                .withContext('manual expansion works and is not re-collapsed by the budget')
                .toBeGreaterThan(rowsAfterSearch);
        });

        it('MEASUREMENT: the budget does not bound manual expansion or a restored expansion state', async () => {
            fixture.detectChanges();
            await settle();

            // The scenario the fix round names as its own limit: a reload restores a large
            // expansion set (here: everything), no search involved.
            const allIds = new Set<string>();
            const collect = (rows: any[]): void => rows.forEach((row) => {
                allIds.add(row.id);
                collect(row.children || []);
            });
            collect(component.rows);
            (component as any).applyExpansion((component as any).allRows, allIds);
            (component as any).rows = [...(component as any).allRows];

            const start = performance.now();
            fixture.detectChanges();
            const renderMs = performance.now() - start;
            const rows = visibleRowIds().length;

            console.log(`[QA-PERF-2] manual/restored expansion, no search: rows=${rows} render=${renderMs.toFixed(1)} ms`);

            // No assertion on the duration - this documents the limitation, it does not gate it.
            expect(rows).withContext('the search budget does not apply here').toBeGreaterThan(100);
        }, 120000);

        it('does not filter at all below the minimum query length, even on the large tree', async () => {
            fixture.detectChanges();
            await settle();
            const rowsBefore = visibleRowIds().length;

            component.updateSearchQuery('l');
            await settle();
            fixture.detectChanges();

            expect(component.rows.length).toBe(10);
            expect(visibleRowIds().length).toBe(rowsBefore);
        });
    });
});
