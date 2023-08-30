import { ROUTE_BREADCRUMB_KEY, ROUTE_CHILD_BREADCRUMB_OUTLET_KEY, ROUTE_ENTITY_RESOLVER_KEY, ROUTE_ENTITY_TYPE_KEY, RouteData } from '@admin-ui/common';
import { InitializableServiceBase } from '@admin-ui/shared/providers/initializable-service-base';
import { SelectState } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ActivatedRoute, NavigationEnd, PRIMARY_OUTLET, Router, UrlSegment } from '@angular/router';
import { GcmsUiLanguage } from '@gentics/cms-models';
import { IBreadcrumbRouterLink } from '@gentics/ui-core';
import { has as _has, isEqual as _isEqual } from'lodash-es'
import { BehaviorSubject, Observable, combineLatest, of as observableOf } from 'rxjs';
import { filter, map, switchMap, takeUntil } from 'rxjs/operators';
import { I18nService } from '../i18n/i18n.service';
import { RouteEntityResolverService } from '../route-entity-resolver/route-entity-resolver.service';
import { BreadcrumbInfo } from './breadcrumb-info';

interface RouteSegment {
    breadcrumb: BreadcrumbInfo;
    routerCommands: string[];
}

/**
 * Service for providing the breadcrumbs that correspond to the current router state in the current UI language.
 * Use the `breadcrumbs$` property to observe the breadcrumbs.
 */
@Injectable()
export class BreadcrumbsService extends InitializableServiceBase {

    @SelectState(state => state.ui.language)
    private uiLanguage$: Observable<GcmsUiLanguage>;

    private currBreadcrumbs$ = new BehaviorSubject<IBreadcrumbRouterLink[]>([]);

    private reloadTrigger$ = new BehaviorSubject<void>(null);

    /**
     * Gets an observable for the array of `IBreadcrumbRouterLink`s that should be
     * displayed as breadcrumbs. When the route or the UI language changes,
     * the observable will emit the updated breadcrumbs.
     */
    get breadcrumbs$(): Observable<IBreadcrumbRouterLink[]> {
        return this.currBreadcrumbs$;
    }

    constructor(
        private activatedRoute: ActivatedRoute,
        private i18n: I18nService,
        private router: Router,
        private routeEntityResolver: RouteEntityResolverService,
    ) {
        super();
    }

    public reload(): void {
        this.reloadTrigger$.next();
    }

    protected onServiceInit(): void {
        const breadcrumbChanges$ = this.router.events.pipe(
            filter(event => event instanceof NavigationEnd),
            switchMap(() => combineLatest([
                this.collectBreadcrumbsFromRoute(this.activatedRoute),
                this.uiLanguage$,
                this.reloadTrigger$,
            ])),
            map(([segments, uiLang]) => this.assembleRouterLinks(segments)),
            map(routerLinks => routerLinks.filter(
                // Filter out duplicate breadcrumbs - this can happen if two routes with empty paths are nested.
                // Since the breadcrumbs are created asynchronously, filtering here is easier than while assembling the breadcrumbs.
                (currLink, index) => index === 0 || !_isEqual(currLink.route, routerLinks[index - 1].route),
            )),
            takeUntil(this.stopper.stopper$),
        );

        breadcrumbChanges$.subscribe(breadcrumbs => this.currBreadcrumbs$.next(breadcrumbs));
    }

    private collectBreadcrumbsFromRoute(activatedRoute: ActivatedRoute): Observable<RouteSegment[]> {
        const routeSegments: Observable<RouteSegment>[] = [];
        const parentUrl: UrlSegment[] = [];
        let currRoute = activatedRoute.root;

        do {
            const currSnapshot = currRoute.snapshot;
            const childOutlets = [].concat(
                _has(currSnapshot, `data.${ROUTE_CHILD_BREADCRUMB_OUTLET_KEY}`)
                    ? currSnapshot.data[ROUTE_CHILD_BREADCRUMB_OUTLET_KEY]
                    : PRIMARY_OUTLET,
            );
            const childRoutes = currRoute.children;
            currRoute = null;
            for (const route of childRoutes) {
                // First matched breadcrumb will be used if there are multiple
                if (childOutlets.includes(route.outlet)) {
                    currRoute = route;
                    routeSegments.push(this.assembleRouteSegment(parentUrl, route));
                    parentUrl.push(...route.snapshot.url);
                    break;
                }
            }
        } while (currRoute);

        return combineLatest(routeSegments).pipe(
            // Filter out segments that have no breadcrumbs.
            map(segments => segments.filter(segment => !!segment)),
        );
    }

    private convertUrlToRouterCommands(parentUrl: UrlSegment[], url: UrlSegment[], outlet: string): string[] {
        const route: any[] = [];
        parentUrl.forEach(segment => route.push(segment.path));

        if (outlet !== PRIMARY_OUTLET) {
            route.push({ outlets: { [outlet]: [].concat(url.map(segment => segment.path)) } });
        } else {
            url.forEach(segment => route.push(segment.path));
        }

        if (route.length === 0) {
            route.push('/');
        }

        return route;
    }

    /**
     * @returns an observable, which will emit a RouteSegment if the following conditions are true:
     * - it is the first route or the length of the route's url is greater than 0 (otherwise it would be the same as its parent route) and
     * - the route's data has breadcrumb infomation associated with it.
     * Otherwise the observable will return `undefined`.
     */
    private assembleRouteSegment(parentUrl: UrlSegment[], route: ActivatedRoute): Observable<RouteSegment | undefined> {
        const snapshot = route.snapshot;
        if (snapshot.url.length === 0 && parentUrl.length > 0) {
            return observableOf(undefined);
        }
        const routerLink = this.convertUrlToRouterCommands(parentUrl, snapshot.url, snapshot.outlet);

        return route.data.pipe(
            map((data: RouteData) => {
                if (!data) {
                    return;
                }

                let ret: RouteSegment;

                if (data[ROUTE_BREADCRUMB_KEY]) {
                    ret = {
                        routerCommands: routerLink,
                        breadcrumb: data[ROUTE_BREADCRUMB_KEY],
                    };
                } else if (data[ROUTE_ENTITY_RESOLVER_KEY] && data[ROUTE_ENTITY_TYPE_KEY]) {
                    const handler = this.routeEntityResolver.getHandler(data[ROUTE_ENTITY_TYPE_KEY]);
                    if (handler) {
                        ret = {
                            routerCommands: routerLink,
                            breadcrumb: {
                                title: handler.displayName(data[ROUTE_ENTITY_RESOLVER_KEY]),
                                doNotTranslate: true,
                            },
                        }
                    }
                }
                return ret;
            }),
        );
    }

    private assembleRouterLinks(segments: RouteSegment[]): IBreadcrumbRouterLink[] {
        return segments.map(segment => {
            const breadcrumb = segment.breadcrumb;
            const translate = !breadcrumb.doNotTranslate;

            const title = translate ? this.i18n.instant(breadcrumb.title, breadcrumb.titleParams) : breadcrumb.title;
            let tooltip: string;
            if (breadcrumb.tooltip) {
                tooltip = translate ? this.i18n.instant(breadcrumb.tooltip, breadcrumb.tooltipParams) : breadcrumb.tooltip;
            }

            const ret: IBreadcrumbRouterLink = {
                route: segment.routerCommands,
                text: title,
            };
            if (tooltip) {
                ret.tooltip = tooltip;
            }
            return ret;
        });
    }
}
