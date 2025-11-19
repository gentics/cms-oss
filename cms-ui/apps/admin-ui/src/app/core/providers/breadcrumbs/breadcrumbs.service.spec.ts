import { Component, Injectable } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { IndexByKey } from '@gentics/cms-models';
import { IBreadcrumbRouterLink } from '@gentics/ui-core';
import { Observable, of as observableOf } from 'rxjs';
import { delay, takeUntil } from 'rxjs/operators';
import { GcmsAdminUiRoute } from '../../../common/models/routing';
import { ObservableStopper } from '../../../common/utils/observable-stopper/observable-stopper';
import { GenericRouterOutletComponent } from '../../../shared/components/generic-router-outlet/generic-router-outlet.component';
import { AppStateService } from '../../../state';
import { TestAppState, assembleTestAppStateImports } from '../../../state/utils/test-app-state';
import { RouteEntityResolverService } from '../route-entity-resolver/route-entity-resolver.service';
import { BreadcrumbInfo } from './breadcrumb-info';
import { BreadcrumbsService } from './breadcrumbs.service';
import { I18nService } from '@gentics/cms-components';
import { MockI18nService } from '@gentics/cms-components/testing';

const DASHBOARD = '';
const MODULE_A = 'module-a';
const SUBMODULE_AA = 'sub-module-aa';
const SUBMODULE_AAA = 'sub-module-aaa';
const SUBMODULE_AB = 'sub-module-ab';
const MODULE_B = 'module-b';
const SUBMODULE_BA = 'modules-ba';
const SUBMODULE_BAA = 'modules-baa';
const ROOT_SIBLING = 'root-sibling';

const RESOLVE_DELAY = 100;

@Component({
    template: '<router-outlet></router-outlet>',
    standalone: false,
})
class RootComponent { }

@Component({
    template: '<router-outlet></router-outlet>',
    standalone: false,
})
class TestComponent { }

@Injectable()
class DelayedResolver {
    resolve(): Observable<BreadcrumbInfo> {
        return observableOf({
            title: 'main.resolvedTitle',
        }).pipe(delay(RESOLVE_DELAY));
    }
}

const ROUTES: GcmsAdminUiRoute[] = [
    {
        path: DASHBOARD,
        component: GenericRouterOutletComponent,
        data: {
            breadcrumb: {
                title: 'main.dashboard',
            },
        },
        children: [
            {
                path: '',
                pathMatch: 'full',
                component: TestComponent,
            },
            {
                path: MODULE_A,
                component: TestComponent,
                data: {
                    breadcrumb: {
                        title: 'main.moduleA',
                        titleParams: { paramA: 1, paramB: 2 },
                    },
                },
                children: [
                    {
                        path: SUBMODULE_AA,
                        component: TestComponent,
                        data: {
                            breadcrumb: {
                                title: 'SubModule AA',
                                doNotTranslate: true,
                            },
                        },
                        children: [
                            {
                                path: SUBMODULE_AAA,
                                component: TestComponent,
                                data: {
                                    breadcrumb: {
                                        title: 'main.subModuleAAA',
                                        titleParams: { paramA: 1 },
                                        tooltip: 'main.subModuleAAATooltip',
                                        tooltipParams: { paramB: 2 },
                                    },
                                },
                            },
                        ],
                    },
                    {
                        path: SUBMODULE_AB,
                        component: TestComponent,
                        resolve: {
                            breadcrumb: DelayedResolver,
                        },
                    },
                ],
            },
            {
                path: MODULE_B,
                component: TestComponent,
                data: {
                    breadcrumb: {
                        title: 'main.moduleB',
                    },
                },
                children: [
                    {
                        path: SUBMODULE_BA,
                        component: TestComponent,
                        // Intentionally no breadcrumbs for MODULE_BA.
                        children: [
                            {
                                path: SUBMODULE_BAA,
                                component: TestComponent,
                                data: {
                                    breadcrumb: {
                                        title: 'main.moduleBAA',
                                    },
                                },
                            },
                        ],
                    },
                ],
            },
        ],
    },
    {
        path: ROOT_SIBLING,
        component: TestComponent,
        data: {
            breadcrumb: {
                title: 'main.root-sibling',
            },
        },
    },
];

const EXPECTED_BREADCRUMBS: IndexByKey<IBreadcrumbRouterLink> = {
    [DASHBOARD]: {
        route: ['/'],
        text: 'translated-en-main.dashboard',
    },
    [MODULE_A]: {
        route: [MODULE_A],
        text: 'translated-en-main.moduleA {"paramA":1,"paramB":2}',
    },
    [SUBMODULE_AA]: {
        route: [MODULE_A, SUBMODULE_AA],
        text: 'SubModule AA',
    },
    [SUBMODULE_AAA]: {
        route: [MODULE_A, SUBMODULE_AA, SUBMODULE_AAA],
        text: 'translated-en-main.subModuleAAA {"paramA":1}',
        tooltip: 'translated-en-main.subModuleAAATooltip {"paramB":2}',
    },
    [SUBMODULE_AB]: {
        route: [MODULE_A, SUBMODULE_AB],
        text: 'translated-en-main.resolvedTitle',
    },
    [MODULE_B]: {
        route: [MODULE_B],
        text: 'translated-en-main.moduleB',
    },
    [SUBMODULE_BAA]: {
        route: [MODULE_B, SUBMODULE_BA, SUBMODULE_BAA],
        text: 'translated-en-main.moduleBAA',
    },
    [ROOT_SIBLING]: {
        route: [ROOT_SIBLING],
        text: 'translated-en-main.root-sibling',
    },
};
Object.freeze(EXPECTED_BREADCRUMBS);

class MockRouteEntityResolverService {}

describe('BreadcrumbsService', () => {

    let breadcrumbs: BreadcrumbsService;
    let state: TestAppState;
    let i18n: I18nService;
    let router: Router;
    let stopper: ObservableStopper;
    let fixture: ComponentFixture<RootComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                assembleTestAppStateImports(),
                RouterTestingModule.withRoutes(ROUTES),
            ],
            declarations: [
                GenericRouterOutletComponent,
                RootComponent,
                TestComponent,
            ],
            providers: [
                BreadcrumbsService,
                DelayedResolver,
                TestAppState,
                { provide: RouteEntityResolverService, useClass: MockRouteEntityResolverService },
                { provide: AppStateService, useExisting: TestAppState },
                { provide: I18nService, useClass: MockI18nService },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(RootComponent);
        breadcrumbs = TestBed.inject(BreadcrumbsService);
        state = TestBed.inject(TestAppState);
        i18n = TestBed.inject(I18nService);
        router = TestBed.inject(Router);
        stopper = new ObservableStopper();

        spyOn(i18n, 'instant').and.callFake((key, params) => {
            const translated = `translated-${state.now.ui.language}-${key}`;
            return params ? `${translated} ${JSON.stringify(params)}` : translated;
        });

        breadcrumbs.init();
    });

    afterEach(() => {
        stopper.stop();
        fixture.destroy();
    });

    function navigateByUrl(url: string): Promise<boolean> {
        // We need to execute the navigation inside an NgZone, otherwise we get a warning on the console.
        return fixture.ngZone.run(() => router.navigateByUrl(url));
    }

    it('emits an empty array after initialization', fakeAsync(() => {
        let breadcrumbLinks: IBreadcrumbRouterLink[];
        breadcrumbs.breadcrumbs$
            .pipe(takeUntil(stopper.stopper$))
            .subscribe((links) => breadcrumbLinks = links);

        expect(breadcrumbLinks).toEqual([]);
    }));

    it('works for the root route and does not produce double breadcrumbs for two consecutive empty paths', fakeAsync(() => {
        let breadcrumbLinks: IBreadcrumbRouterLink[];
        breadcrumbs.breadcrumbs$
            .pipe(takeUntil(stopper.stopper$))
            .subscribe((links) => breadcrumbLinks = links);

        navigateByUrl('/');
        tick();
        expect(breadcrumbLinks).toEqual([EXPECTED_BREADCRUMBS[DASHBOARD]]);
    }));

    it('works for a first level child route', fakeAsync(() => {
        let breadcrumbLinks: IBreadcrumbRouterLink[];
        breadcrumbs.breadcrumbs$
            .pipe(takeUntil(stopper.stopper$))
            .subscribe((links) => breadcrumbLinks = links);

        navigateByUrl(`/${MODULE_B}`);
        tick();
        expect(breadcrumbLinks).toEqual([
            EXPECTED_BREADCRUMBS[DASHBOARD],
            EXPECTED_BREADCRUMBS[MODULE_B],
        ]);
    }));

    it('works for a route with titleParams', fakeAsync(() => {
        let breadcrumbLinks: IBreadcrumbRouterLink[];
        breadcrumbs.breadcrumbs$
            .pipe(takeUntil(stopper.stopper$))
            .subscribe((links) => breadcrumbLinks = links);

        navigateByUrl(`/${MODULE_A}`);
        tick();
        expect(breadcrumbLinks).toEqual([
            EXPECTED_BREADCRUMBS[DASHBOARD],
            EXPECTED_BREADCRUMBS[MODULE_A],
        ]);
    }));

    it('works for a route with doNotTranslate', fakeAsync(() => {
        let breadcrumbLinks: IBreadcrumbRouterLink[];
        breadcrumbs.breadcrumbs$
            .pipe(takeUntil(stopper.stopper$))
            .subscribe((links) => breadcrumbLinks = links);

        navigateByUrl(`/${MODULE_A}/${SUBMODULE_AA}`);
        tick();
        expect(breadcrumbLinks).toEqual([
            EXPECTED_BREADCRUMBS[DASHBOARD],
            EXPECTED_BREADCRUMBS[MODULE_A],
            EXPECTED_BREADCRUMBS[SUBMODULE_AA],
        ]);
    }));

    it('works for a nested route with title and tooltip', fakeAsync(() => {
        let breadcrumbLinks: IBreadcrumbRouterLink[];
        breadcrumbs.breadcrumbs$
            .pipe(takeUntil(stopper.stopper$))
            .subscribe((links) => breadcrumbLinks = links);

        navigateByUrl(`/${MODULE_A}/${SUBMODULE_AA}/${SUBMODULE_AAA}`);
        tick();
        expect(breadcrumbLinks).toEqual([
            EXPECTED_BREADCRUMBS[DASHBOARD],
            EXPECTED_BREADCRUMBS[MODULE_A],
            EXPECTED_BREADCRUMBS[SUBMODULE_AA],
            EXPECTED_BREADCRUMBS[SUBMODULE_AAA],
        ]);
    }));

    it('works for a route with a breadcrumb resolver', fakeAsync(() => {
        let breadcrumbLinks: IBreadcrumbRouterLink[];
        breadcrumbs.breadcrumbs$
            .pipe(takeUntil(stopper.stopper$))
            .subscribe((links) => breadcrumbLinks = links);

        navigateByUrl(`/${MODULE_A}/${SUBMODULE_AB}`);
        tick(RESOLVE_DELAY);
        expect(breadcrumbLinks).toEqual([
            EXPECTED_BREADCRUMBS[DASHBOARD],
            EXPECTED_BREADCRUMBS[MODULE_A],
            EXPECTED_BREADCRUMBS[SUBMODULE_AB],
        ]);
    }));

    it('produces working router links and updates the breadcrumbs on a subsequent navigation', fakeAsync(() => {
        let breadcrumbLinks: IBreadcrumbRouterLink[];
        breadcrumbs.breadcrumbs$
            .pipe(takeUntil(stopper.stopper$))
            .subscribe((links) => breadcrumbLinks = links);

        navigateByUrl(`/${MODULE_A}/${SUBMODULE_AA}`);
        tick();
        expect(breadcrumbLinks).toEqual([
            EXPECTED_BREADCRUMBS[DASHBOARD],
            EXPECTED_BREADCRUMBS[MODULE_A],
            EXPECTED_BREADCRUMBS[SUBMODULE_AA],
        ]);

        fixture.ngZone.run(() => router.navigate(breadcrumbLinks[1].route));
        tick();
        expect(breadcrumbLinks).toEqual([
            EXPECTED_BREADCRUMBS[DASHBOARD],
            EXPECTED_BREADCRUMBS[MODULE_A],
        ]);
    }));

    it('works for a route where one part has no breadcrumbInfo', fakeAsync(() => {
        let breadcrumbLinks: IBreadcrumbRouterLink[];
        breadcrumbs.breadcrumbs$
            .pipe(takeUntil(stopper.stopper$))
            .subscribe((links) => breadcrumbLinks = links);

        navigateByUrl(`/${MODULE_B}/${SUBMODULE_BA}/${SUBMODULE_BAA}`);
        tick();
        expect(breadcrumbLinks).toEqual([
            EXPECTED_BREADCRUMBS[DASHBOARD],
            EXPECTED_BREADCRUMBS[MODULE_B],
            EXPECTED_BREADCRUMBS[SUBMODULE_BAA],
        ]);
    }));

    it('works for a route where the end has no breadcrumbInfo', fakeAsync(() => {
        let breadcrumbLinks: IBreadcrumbRouterLink[];
        breadcrumbs.breadcrumbs$
            .pipe(takeUntil(stopper.stopper$))
            .subscribe((links) => breadcrumbLinks = links);

        navigateByUrl(`/${MODULE_B}/${SUBMODULE_BA}`);
        tick();
        expect(breadcrumbLinks).toEqual([
            EXPECTED_BREADCRUMBS[DASHBOARD],
            EXPECTED_BREADCRUMBS[MODULE_B],
        ]);
    }));

    it('works for a sibling to the empty root route', fakeAsync(() => {
        let breadcrumbLinks: IBreadcrumbRouterLink[];
        breadcrumbs.breadcrumbs$
            .pipe(takeUntil(stopper.stopper$))
            .subscribe((links) => breadcrumbLinks = links);

        navigateByUrl(`/${ROOT_SIBLING}`);
        tick();
        expect(breadcrumbLinks).toEqual([
            EXPECTED_BREADCRUMBS[ROOT_SIBLING],
        ]);
    }));

    it('updates breadcrumbs when the UI language changes', fakeAsync(() => {
        let breadcrumbLinks: IBreadcrumbRouterLink[];
        breadcrumbs.breadcrumbs$
            .pipe(takeUntil(stopper.stopper$))
            .subscribe((links) => breadcrumbLinks = links);

        navigateByUrl(`/${MODULE_A}`);
        tick();
        expect(breadcrumbLinks).toEqual([
            EXPECTED_BREADCRUMBS[DASHBOARD],
            EXPECTED_BREADCRUMBS[MODULE_A],
        ]);

        state.mockState({
            ui: {
                language: 'de',
            },
        });
        tick();
        expect(breadcrumbLinks).toEqual([
            {
                ...EXPECTED_BREADCRUMBS[DASHBOARD],
                text: 'translated-de-main.dashboard',
            },
            {
                ...EXPECTED_BREADCRUMBS[MODULE_A],
                text: 'translated-de-main.moduleA {"paramA":1,"paramB":2}',
            },
        ]);
    }));

});
