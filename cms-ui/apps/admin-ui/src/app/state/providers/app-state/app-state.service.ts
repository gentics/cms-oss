import { Injectable } from '@angular/core';
import { Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { ServiceBase } from '../../../shared/providers/service-base/service.base';
import { AppState } from '../../app-state';

/**
 * Wraps ngxs `Store` and provides typed methods specific to `AppState`.
 *
 * Always inject this service into your components and services, do not use ngxs `Store` directly.
 */
@Injectable()
export class AppStateService extends ServiceBase {

    constructor(
        protected store: Store,
    ) {
        super();
    }

    /**
     * Returns the current raw value of the state.
     *
     * An alternative to `snapshot()` to allow easier code migration from GCMS UI.
     */
    get now(): AppState {
        return this.snapshot();
    }

    /**
     * Dispatches one or multiple events to change the AppState.
     */
    dispatch(event: any | any[]): Observable<any> {
        return this.store.dispatch(event);
    }

    /**
     * Selects a specific slice from the AppState.
     */
    select<R>(selector: (state: AppState) => R): Observable<R> {
        return this.store.select(selector);
    }

    /**
     * Selects a specific slice from the AppState and returns an Observable
     * that will emit once and then close itself.
     */
    selectOnce<R>(selector: (state: AppState) => R): Observable<R> {
        return this.store.selectOnce(selector);
    }

    /**
     * Returns the current raw value of the state.
     */
    snapshot(): AppState {
        return this.store.snapshot();
    }

}
