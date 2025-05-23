import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { Observable, of } from 'rxjs';
import { filter, map, take, distinctUntilChanged, tap, switchMap } from 'rxjs/operators';
import { ApplicationStateService, FolderActionsService } from '../../../state';

/**
 * A guard to prevent users navigating to protected routes when not logged in,
 * and preventing navigating to login when already logged in.
 */
@Injectable()
export class AuthGuard  {

    constructor(
        private appState: ApplicationStateService,
        private router: Router,
        private folderActions: FolderActionsService,
    ) {}

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean | Promise<boolean> | Observable<boolean> {
        const authState = this.appState.now.auth;

        if (route.url[0]?.path === 'login') {
            // If the user is currently tring to log in, then we always have to allow it
            if (!authState.isLoggedIn) {
                // If we're already logging in in the background (validating existing login for example),
                // then we don't want to display the login screen yet.
                if (!authState.loggingIn) {
                    return true;
                }

                // Wait till we got response of the background check
                return this.waitForLogin().pipe(
                    // If we're already logged in, redirect to the list, otherwise allow the login screen
                    switchMap(loggedIn => loggedIn ? this.redirectToList() : of(true)),
                );
            }

            // if logged in an attempting to access the /login route, redirect to default node.
            // also it should wait until nodes are loaded because it will stuck on login page
            return this.redirectToList();
        }

        if (authState.isLoggedIn) {
            return true;
        }

        if (!authState.loggingIn) {
            this.router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
            return false;
        }

        return this.waitForLogin().pipe(
            tap(loginValidated => {
                if (!loginValidated) {
                    this.router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
                }
            }),
        );
    }

    private waitForLogin(): Observable<boolean> {
        return this.appState.select(state => state.auth.loggingIn).pipe(
            distinctUntilChanged(),
            filter(loggingIn => !loggingIn),
            take(1),
            map(() => this.appState.now.auth.isLoggedIn),
        );
    }

    private redirectToList(): Observable<boolean> {
        return this.appState.select(state => state.folder.nodesLoaded).pipe(
            filter(loaded => loaded),
            switchMap(() => this.appState.select(state => state.folder.nodes.list)),
            take(1),
            map(() => {
                // Only navigate to the default node if no other node has been set through the user settings.
                // This can occur if a logged in user navigates to <cms-domain/.Node/ui/
                // He is first redirected to /login and without this check he would always
                // be redirected to the default node in that case.
                if (!this.appState.now.folder.activeNode) {
                    this.folderActions.navigateToDefaultNode();
                }
                return false;
            }),
        );
    }
}
