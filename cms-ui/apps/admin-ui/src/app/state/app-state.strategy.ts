import { Injectable } from '@angular/core';
import { AppStateStrategy } from '@gentics/cms-components';
import { Observable } from 'rxjs';
import { AppState } from './app-state';
import { AppStateService } from './providers/app-state/app-state.service';

@Injectable()
export class AdminUiAppStateStrategy implements AppStateStrategy {
    constructor(private appState: AppStateService) {}

    get now(): AppState {
        return this.appState.snapshot();
    }

    select<R>(selector: (state: AppState) => R): Observable<R> {
        return this.appState.select(selector);
    }
}
