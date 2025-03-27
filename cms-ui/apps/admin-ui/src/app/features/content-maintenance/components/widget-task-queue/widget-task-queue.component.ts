import { Component, Input } from '@angular/core';
import { DirtQueueEntry, DirtQueueSummaryResponse } from '@gentics/cms-models';

@Component({
    selector: 'gtx-widget-task-queue',
    templateUrl: './widget-task-queue.component.html',
    styleUrls: ['./widget-task-queue.component.scss'],
})
export class WidgetTaskQueueComponent {

    @Input()
    public summary: DirtQueueSummaryResponse;

    @Input()
    public failedTasks: DirtQueueEntry[] = [];
}
