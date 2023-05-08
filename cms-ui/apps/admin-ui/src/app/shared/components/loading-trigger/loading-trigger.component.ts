import { AppStateService, DecrementMasterLoading, IncrementMasterLoading } from '@admin-ui/state';
import { AfterViewInit, Component, OnInit } from '@angular/core';

/**
 * This component increments masterLoading during the initialization of its parent component, which will increment masterLoading, when making a rest call.
 * If we would rely on the rest call alone, the parent component would be briefly shown before the loading spinner is visible (SUP-8755).
 */
@Component({
    selector: 'gtx-loading-trigger',
    template: '',
})
export class LoadingTriggerComponent implements OnInit, AfterViewInit {

    constructor(private appState: AppStateService) { }

    ngOnInit(): void {
        this.appState.dispatch(new IncrementMasterLoading());
    }

    ngAfterViewInit(): void {
        this.appState.dispatch(new DecrementMasterLoading());
    }
}
