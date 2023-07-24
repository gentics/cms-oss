import { AdminUIModuleRoutes } from '@admin-ui/common';
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { distinctUntilChanged, filter, map, take } from 'rxjs/operators';
import { AppStateService } from '../../../../state';

/**
 * A guard to prevent users navigating to protected routes when not logged in,
 * and preventing navigating to login when already logged in.
 */
@Injectable()
export class AuthGuard  {

    constructor(
        private appState: AppStateService,
        private router: Router,
    ) {}

    canActivate(route: ActivatedRouteSnapshot, routerState: RouterStateSnapshot): boolean | Promise<boolean> | Observable<boolean> {
        const authState = this.appState.now.auth;

        if (route.url.length > 0 && route.url[0].path === AdminUIModuleRoutes.LOGIN) {
            if (authState.isLoggedIn) {
                // if logged in and attempting to access the /login route, redirect to the root route.
                this.router.navigateByUrl('/');
                return false;
            } else {
                return true;
            }
        }

        if (authState.isLoggedIn) {
            return true;
        }

        if (authState.loggingIn) {
            return this.appState.select(state => state.auth).pipe(
                distinctUntilChanged((a, b) => a.loggingIn === b.loggingIn),
                filter(auth => !auth.loggingIn),
                take(1),
                map(auth => auth.isLoggedIn),
            ).toPromise()
                .then(loginValidated => {
                    if (!loginValidated) {
                        this.router.navigate([`/${AdminUIModuleRoutes.LOGIN}`], { queryParams: { returnUrl: routerState.url } });
                    }
                    return loginValidated;
                });
        }

        this.router.navigate([`/${AdminUIModuleRoutes.LOGIN}`], { queryParams: { returnUrl: routerState.url } });
        return false;
    }
}
