import { AppStateService, SwitchEditorTab } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { isEqual } from'lodash-es'
import { Observable } from 'rxjs';
import { distinctUntilChanged, map, switchMap, tap } from 'rxjs/operators';

/** The name of the editorTab route parameter. */
export const EDITOR_TAB = 'editorTab';

/**
 * Facilitates the tracking of the currently active editor tab parameter in the routes
 * of the details view.
 *
 * To use this service, follow this procedure:
 *
 * 1. Define an observable used for tracking the active editor tab and initialize it in
 * `ngOnInit()` using `this.activeTabId$ = this.editorTabTracker.trackEditorTab(this.route);`.
 *
 * 2. Add a `pure` `gtx-tabs` component to your detail component's template that receives the
 * active tab ID from the previously defined observable:
 * `<gtx-tabs pure [activeId]="activeTabId$ | async">`
 *
 * 3. For each `gtx-tab` that you add, assign an `id` and apply the `gtxDetailTabLink` directive, e.g.,
 * ```
 * <gtx-tab
 *       [id]="NodeDetailTabs.properties"
 *       gtxDetailTabLink
 *       [title]="'shared.title_properties' | i18n"
 * >
 * ```
 */
@Injectable()
export class EditorTabTrackerService {

    constructor(
        private appState: AppStateService,
    ) { }

    /**
     * Tracks the `EDITOR_TAB` parameter in the `route` and updates the
     * AppState on every change.
     *
     * @returns An observable that emits the currently active editor tab,
     * based on the AppState.
     */
    trackEditorTab(route: ActivatedRoute): Observable<string> {
        return route.paramMap.pipe(
            map(paramMap => paramMap.get(EDITOR_TAB)),
            tap(editorTab => this.appState.dispatch(new SwitchEditorTab(editorTab))),
            switchMap(() => this.appState.select(state => state.ui.editorTab)),
            distinctUntilChanged(isEqual),
        );
    }

}
