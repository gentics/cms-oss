import {
    AppStateService,
    FocusEditor,
    FocusList,
    LoadingStateModel,
    SelectState,
    UIStateModel,
} from '@admin-ui/state';
import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { delay, map } from 'rxjs/operators';

/**
 * This component can be used as a parent component for child routes if
 * the children should completely replace the parent component with Master-Detail view.
 */
@Component({
    selector: 'gtx-split-view-router-outlet',
    templateUrl: './split-view-router-outlet.component.html',
    styleUrls: ['./split-view-router-outlet.component.scss'],
    standalone: false
})
export class SplitViewRouterOutletComponent implements OnInit {

    @SelectState(state => state.ui)
    stateUI$: Observable<UIStateModel>;

    @SelectState(state => state.loading)
    stateLoading$: Observable<LoadingStateModel>;

    editorIsFocused$: Observable<boolean>;
    focusMode$: Observable<boolean>;
    editorIsOpen$: Observable<boolean>;
    splitFocus: 'left' | 'right' = 'left';

    loadingMaster$: Observable<boolean>;
    loadingMasterDelayed$: Observable<boolean>;
    loadingMasterMessage$: Observable<string>;
    loadingDetail$: Observable<boolean>;
    loadingDetailDelayed$: Observable<boolean>;
    loadingDetailMessage$: Observable<string>;

    public detailsVisible = false;

    constructor(
        private appState: AppStateService,
    ) { }

    ngOnInit(): void {
        this.editorIsFocused$ = this.stateUI$.pipe(
            map(uiState => uiState.editorIsFocused),
            delay(0),
        );
        this.focusMode$ = this.stateUI$.pipe(
            map(uiState => uiState.focusMode),
            delay(0),
        );
        this.editorIsOpen$ = this.stateUI$.pipe(
            map(uiState => uiState.editorIsOpen),
            delay(0),
        );

        this.loadingMaster$ = this.stateLoading$.pipe(
            map(loadingState => loadingState.masterLoading !== 0),
        );

        this.loadingMasterDelayed$ = this.loadingMaster$.pipe(
            delay(0), // to make sure we don't get ExpressionChangedAfterItWasChecked
        );

        this.loadingMasterMessage$ = this.stateLoading$.pipe(
            map(loadingState => loadingState.masterLoadingMessage),
        );

        this.loadingDetail$ = this.stateLoading$.pipe(
            map(loadingState => loadingState.detailLoading !== 0),
        );

        this.loadingDetailDelayed$ = this.loadingDetail$.pipe(
            delay(0), // to make sure we don't get ExpressionChangedAfterItWasChecked
        );

        this.loadingDetailMessage$ = this.stateLoading$.pipe(
            map(loadingState => loadingState.detailLoadingMessage),
        );
    }

    public updateDetailsActivation(value: boolean): void {
        setTimeout(() => {
            this.detailsVisible = value;
        });
    }

    setSplitFocus(focus: 'left' | 'right'): void {
        if (focus === 'right') {
            this.appState.dispatch(new FocusEditor());
        } else {
            this.appState.dispatch(new FocusList());
        }
    }
}
