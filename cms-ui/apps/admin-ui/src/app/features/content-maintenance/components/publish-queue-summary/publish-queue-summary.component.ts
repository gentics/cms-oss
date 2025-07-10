import { ChangeDetectionStrategy, Component, Input, OnChanges } from '@angular/core';
import { Node, PublishQueue } from '@gentics/cms-models';
import { ChangesOf } from '@gentics/ui-core';
import { PUBLISH_PLURAL_MAPPING, PublishType } from '../../models';

interface DirtTotal {
    toPublish: number;
    delayed: number;
}

const PROPERTIES: (keyof DirtTotal)[] = ['toPublish', 'delayed'];

@Component({
    selector: 'gtx-publish-queue-summary',
    templateUrl: './publish-queue-summary.component.html',
    styleUrls: ['./publish-queue-summary.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class PublishQueueSummaryComponent implements OnChanges {

    public readonly PUBLISH_PLURAL_MAPPING = PUBLISH_PLURAL_MAPPING;
    public readonly PROPERTIES = PROPERTIES;

    @Input()
    public nodes: Node[] = [];

    @Input()
    public publishQueue: PublishQueue;

    public totals: Record<PublishType, DirtTotal> = {
        files: {
            toPublish: 0,
            delayed: 0,
        },
        folders:  {
            toPublish: 0,
            delayed: 0,
        },
        forms: {
            toPublish: 0,
            delayed: 0,
        },
        pages: {
            toPublish: 0,
            delayed: 0,
        },
    };

    ngOnChanges(changes: ChangesOf<this>): void {
        if (changes.publishQueue) {
            this.updateTotals();
        }
    }

    public updateTotals(): void {
        this.totals = Object.values(this.publishQueue?.nodes || {}).reduce((acc, queue) => {
            Object.keys(PUBLISH_PLURAL_MAPPING).forEach(type => {
                PROPERTIES.forEach(prop => {
                    acc[type][prop] += queue[type][prop];
                });
            });

            return acc;
        }, {
            files: {
                toPublish: 0,
                delayed: 0,
            },
            folders:  {
                toPublish: 0,
                delayed: 0,
            },
            forms: {
                toPublish: 0,
                delayed: 0,
            },
            pages: {
                toPublish: 0,
                delayed: 0,
            },
        });
    }
}
