import { ROUTE_IS_EDITOR_ROUTE } from '@admin-ui/common';
import { AppStateService, CloseEditor, SetUIFocusEntity } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ActivationStart, Router } from '@angular/router';
import { InitializableServiceBase } from '@gentics/cms-components';
import { filter, takeUntil } from 'rxjs/operators';

@Injectable()
export class EditorCloserService extends InitializableServiceBase {

    constructor(
        private router: Router,
        private appState: AppStateService,
    ) {
        super();
    }

    protected onServiceInit(): void {
        this.router.events.pipe(
            filter(event => event instanceof ActivationStart),
            takeUntil(this.stopper.stopper$),
        ).subscribe((event: ActivationStart) => {
            if (!event.snapshot.data[ROUTE_IS_EDITOR_ROUTE] && this.appState.now.ui.editorIsOpen) {
                this.appState.dispatch(new CloseEditor());
                this.appState.dispatch(new SetUIFocusEntity(null, null, null));
            }
        });
    }

}
