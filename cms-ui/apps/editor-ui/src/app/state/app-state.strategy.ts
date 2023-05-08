import { Injectable } from '@angular/core';
import { AppStateStrategy } from '@gentics/cms-components';
import { Observable } from 'rxjs';
import { AppState } from '../common/models/app-state';
import { ApplicationStateService } from './application-state.service';

@Injectable()
export class EditorUiAppStateStrategy implements AppStateStrategy {
    constructor(private appState: ApplicationStateService) {}

    get now(): AppState {
        return this.appState.now;
    }

    select<R>(selector: (state: AppState) => R): Observable<R> {
        return this.appState.select(selector);
    }
}
