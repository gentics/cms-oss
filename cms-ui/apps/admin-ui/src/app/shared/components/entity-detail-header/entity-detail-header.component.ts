import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    Output,
} from '@angular/core';

@Component({
    selector: 'gtx-entity-detail-header',
    templateUrl: './entity-detail-header.component.html',
    styleUrls: ['./entity-detail-header.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EntityDetailHeaderComponent {

    @Input()
    title: string;

    /** The action ID that will be passed to the `gtxActionAllowed` directive on the 'Save' button. */
    @Input()
    saveActionAllowedId: string;

    /** The instance ID that will be passed to the `gtxActionAllowed` directive on the 'Save' button. */
    @Input()
    saveActionAllowedInstanceId: number;

    /** The node ID that will be passed to the `gtxActionAllowed` directive on the 'Save' button. */
    @Input()
    saveActionAllowedNodeId: number;

    /**
     * Event handler for the clicking of the 'Save' button.
     *
     * The 'Save' button will only be shown if there is at least one registered event handler.
     */
    @Output()
    saveClick = new EventEmitter<void>();

    /**
     * Determines if the 'Save' button should be disabled (e.g., when the current input is invalid).
     */
    @Input()
    saveDisabled: boolean;

    /**
     * Event handler for the clicking of the 'Cancel' button.
     *
     * The 'Cancel' button will only be shown if there is at least one registered event handler.
     */
    @Output()
    cancelClick = new EventEmitter<void>();

    constructor() { }

}
