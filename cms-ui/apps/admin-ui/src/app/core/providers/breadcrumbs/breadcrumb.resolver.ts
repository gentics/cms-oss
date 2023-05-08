import { Injectable, Injector } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';
import { Observable } from 'rxjs';

import { BreadcrumbInfo } from './breadcrumb-info';

/** The name of the static method used to resolve the `BreadcrumbInfo` in a component' class. */
export const BREADCRUMB_RESOLVER = 'resolveBreadcrumb';

/**
 * Helper function type for resolving the `BreadcrumbInfo` inside a static method of a component.
 */
export type ResolveBreadcrumbFn = (route: ActivatedRouteSnapshot, injector: Injector) => Observable<BreadcrumbInfo>;

// tslint:disable: jsdoc-format
/**
 * Generic resolver for a `BreadcrumbInfo`, which can be used in a `GcmsAdminUiRoute`
 * instead of a custom `Resolve<BreadcrumbInfo>` implementation.
 *
 * The component that is referenced by the route segment needs to provide a static method
 * with the name `[BREADCRUMB_RESOLVER]` of type `ResolveBreadcrumbFn`.
 *
 * Example:
 * ```
  @Component{...}
  export class MyComponent {

       static [BREADCRUMB_RESOLVER]: ResolveBreadcrumbFn = (route, injector) => {
            const appState = injector.get(AppStateService);
            // Read some data from appState.
            return of(dataFromAppState);
        }

  }
  ```
 */
@Injectable()
export class BreadcrumbResolver implements Resolve<BreadcrumbInfo> {

    constructor(private injector: Injector) {}

    resolve(route: ActivatedRouteSnapshot): Observable<BreadcrumbInfo> | Promise<BreadcrumbInfo> | BreadcrumbInfo {
        const routeComponent = (route.component as any);
        if (routeComponent[BREADCRUMB_RESOLVER]) {
            const breadcrumbResolverFn: ResolveBreadcrumbFn = routeComponent[BREADCRUMB_RESOLVER];
            return breadcrumbResolverFn(route, this.injector);
        } else {
            return null;
        }
    }

}
