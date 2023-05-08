import { Injectable } from '@angular/core';
import { Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { AppState } from '../../../common/models';

@Injectable()
export class ApplicationStateService {

    constructor(
        protected store: Store,
    ) { }

    /** Get the current application state object. */
    get now(): AppState {
        return this.store.snapshot();
    }

    snapshot(): AppState {
        return this.store.snapshot();
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
}
