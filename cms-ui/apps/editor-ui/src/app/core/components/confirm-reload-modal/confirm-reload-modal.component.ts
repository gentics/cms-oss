import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';
import { ApplicationStateService, MarkContentAsModifiedAction, MarkObjectPropertiesAsModifiedAction } from '../../../state';

@Component({
    selector: 'app-confirm-reload-modal',
    templateUrl: './confirm-reload-modal.component.html',
    styleUrls: ['./confirm-reload-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmReloadModal extends BaseModal<boolean> implements OnInit {

    public contentModified: boolean;
    public objectPropertiesModified: boolean;

    constructor(
        protected appState: ApplicationStateService,
    ) {
        super();
    }

    ngOnInit(): void {
        const state = this.appState.now.editor;
        this.contentModified = state.contentModified;
        this.objectPropertiesModified = state.objectPropertiesModified;
    }

    reload(): void {
        this.appState.dispatch(new MarkContentAsModifiedAction(false));
        this.appState.dispatch(new MarkObjectPropertiesAsModifiedAction(false, false));
        this.closeFn(true);
    }

    cancelAndClose(): void {
        this.closeFn(false);
    }
}
