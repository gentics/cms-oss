import { Observable } from 'rxjs';
import {
    filter,
    map,
    pairwise,
    startWith,
} from 'rxjs/operators';
import { AppStateService } from '../../providers/app-state/app-state.service';

// This file contains commonly used state selectors.

/**
 * @returns an observable, which emits whenever a user logs in or if the user
 * is already logged in.
 */
export function selectLoginEventOrIsLoggedIn(appState: AppStateService): Observable<void> {
    return appState.select(state => state.auth.isLoggedIn).pipe(
        startWith(false),
        pairwise(),
        filter(([wasLoggedIn, isLoggedIn]) => !wasLoggedIn && isLoggedIn),
        map(() => undefined),
    );
}

/**
 * @returns an observable, which emits whenever a user logs out.
 */
export function selectLogoutEvent(appState: AppStateService): Observable<void> {
    return appState.select(state => state.auth.isLoggedIn).pipe(
        pairwise(),
        filter(([wasLoggedIn, isLoggedIn]) => wasLoggedIn && !isLoggedIn),
        map(() => undefined),
    );
}
