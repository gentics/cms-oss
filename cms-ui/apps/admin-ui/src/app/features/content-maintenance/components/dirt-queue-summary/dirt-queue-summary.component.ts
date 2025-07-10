import { Component, Input } from '@angular/core';
import { DirtQueueEntry, DirtQueueSummaryResponse } from '@gentics/cms-models';

@Component({
    selector: 'gtx-dirt-queue-summary',
    templateUrl: './dirt-queue-summary.component.html',
    styleUrls: ['./dirt-queue-summary.component.scss'],
    standalone: false,
})
export class DirtQueueSummaryComponent {

    @Input()
    public summary: DirtQueueSummaryResponse;

    @Input()
    public failedTasks: DirtQueueEntry[] = [];
}
