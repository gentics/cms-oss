import { PUBLISH_PROCESS_REFRESH_INTERVAL } from '@admin-ui/common';
import { AdminOperations, ErrorHandler, NodeOperations } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges } from '@angular/core';
import { ContentMaintenanceType, Node, PublishInfo, PublishObjectsCount, PublishQueue, Raw } from '@gentics/cms-models';
import { isEqual } from'lodash-es'
import { BehaviorSubject, Subscription, forkJoin, timer } from 'rxjs';
import { catchError, distinctUntilChanged, filter, startWith, switchMap, tap } from 'rxjs/operators';

interface WidgetPublishingProcessPerNodeComponentState {
    publishType: ContentMaintenanceType;
    toPublishTotal: number;
    delayedTotal: number;
    publishedTotal: number;
    remainingTotal: number;
    nodes: WidgetPublishingProcessPerNodeComponentStateNode[],
}

interface WidgetPublishingProcessPerNodeComponentStateNode {
    nodeId: number;
    name: string;
    disablePublish: boolean;
    toPublish: NodeState;
    delayed: NodeState;
    published: NodeState;
    remaining: NodeState;
}

interface NodeState {
    amount: number;
    percentage: number;
}

const WIDGET_PUBLISHING_PROCESS_STATUS_KEYS = [
    'toPublish',
    'delayed',
    'published',
    'remaining',
] as const;

/**
 * A table dynmically fetching and displaying all nodes and their publish queue status details.
 */
@Component({
    selector: 'gtx-widget-publishing-process-per-node-overview',
    templateUrl: './widget-publishing-process-per-node-overview.component.html',
    styleUrls: ['./widget-publishing-process-per-node-overview.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WidgetPublishingProcessPerNodeOverviewComponent implements OnInit, OnChanges, OnDestroy {

    /** If TRUE node rows are selectable and component emits IDs on selection changed. */
    @Input()
    public selectable = false;

    /** node IDs selected in component */
    @Input()
    public selectedIds: number[] = [];

    /** If TRUE component polls and refreshs the data display in an intervall defined in `lifeSyncIntervall` */
    @Input()
    public lifeSyncEnabled = true;

    /** Determines the amount of seconds between polling information. */
    @Input()
    public lifeSyncIntervall: number;

    /** emits selected node IDs on checkbox clicked */
    @Output()
    public selectedIdsChange = new EventEmitter<number[]>();

    private syncIntervall$ = new BehaviorSubject<number>(PUBLISH_PROCESS_REFRESH_INTERVAL);

    /** information of the current publish process per node */
    infoStatsPerNodeData$ = new BehaviorSubject<PublishQueue>(null);

    widgetPublishingProcessStatusKeys = WIDGET_PUBLISHING_PROCESS_STATUS_KEYS;

    /** inferred data for visualization */
    publishState: WidgetPublishingProcessPerNodeComponentState[] = [
        {
            publishType: ContentMaintenanceType.page,
            toPublishTotal: 0,
            delayedTotal: 0,
            publishedTotal: 0,
            remainingTotal: 0,
            nodes: [],
        },
        {
            publishType: ContentMaintenanceType.file,
            toPublishTotal: 0,
            delayedTotal: 0,
            publishedTotal: 0,
            remainingTotal: 0,
            nodes: [],
        },
        {
            publishType: ContentMaintenanceType.folder,
            toPublishTotal: 0,
            delayedTotal: 0,
            publishedTotal: 0,
            remainingTotal: 0,
            nodes: [],
        },
        {
            publishType: ContentMaintenanceType.form,
            toPublishTotal: 0,
            delayedTotal: 0,
            publishedTotal: 0,
            remainingTotal: 0,
            nodes: [],
        },
    ];

    /** If TRUE table is in loading state. */
    public tableIsLoading = true;

    private subscriptions: Subscription[] = [];

    constructor(
        private adminOps: AdminOperations,
        private nodeOps: NodeOperations,
        private errorHandler: ErrorHandler,
        private changeDetectorRef: ChangeDetectorRef,
    ) { }

    ngOnInit(): void {
        const intervall$ = this.syncIntervall$.asObservable().pipe(
            distinctUntilChanged(isEqual),
            switchMap(milliseconds => timer(0, milliseconds)),
            filter(() => this.lifeSyncEnabled),
        );

        // initialize data stream of node publish status info
        this.subscriptions.push(intervall$.pipe(
            startWith(null),
            // start loading indicator
            tap(() => {
                this.tableIsLoading = true;
            }),
            // request data
            switchMap(() => forkJoin([
                this.adminOps.getPublishInfo(),
                this.adminOps.getPublishQueue(),
                this.nodeOps.getAll(),
            ])),
            catchError(error => this.errorHandler.catch(error)),
        ).subscribe(([info, queue, allNodeEntities]: [PublishInfo, PublishQueue, Node<Raw>[]]) => {
            // emit latest data
            this.infoStatsPerNodeData$.next(queue);

            // set loading indicator
            this.tableIsLoading = false;

            // null check
            const infoNodesKeys = info && Object.keys(queue.nodes);
            if (!infoNodesKeys.length || !allNodeEntities.length) {
                return;
            }

            // assemble component state
            this.publishState.forEach((publishableEntityState: WidgetPublishingProcessPerNodeComponentState) => {
                const publishTypeKey = `${publishableEntityState.publishType}s`;

                // sum totals
                let toPublishTotal = 0;
                let delayedTotal = 0;
                let publishedTotal = 0;
                let remainingTotal = 0;

                Object.keys(queue.nodes).forEach(nodeId => {
                    const queueInfoPerType: PublishObjectsCount = queue.nodes[nodeId][publishTypeKey];
                    toPublishTotal += queueInfoPerType.toPublish;
                    delayedTotal += queueInfoPerType.delayed;
                    publishedTotal += queueInfoPerType.published;
                    remainingTotal += queueInfoPerType.remaining;
                });

                publishableEntityState.toPublishTotal = toPublishTotal;
                publishableEntityState.delayedTotal = delayedTotal;
                publishableEntityState.publishedTotal = publishedTotal;
                publishableEntityState.remainingTotal = remainingTotal;

                // assemble state per node
                publishableEntityState.nodes = allNodeEntities.map(node => {
                    const queueInfoPerType: PublishObjectsCount = queue.nodes[node.id][publishTypeKey];

                    const retVal: WidgetPublishingProcessPerNodeComponentStateNode = {
                        nodeId: node.id,
                        name: node.name,
                        disablePublish: node.disablePublish,
                        toPublish: {
                            amount: 0,
                            percentage: 0,
                        },
                        delayed: {
                            amount: 0,
                            percentage: 0,
                        },
                        published: {
                            amount: 0,
                            percentage: 0,
                        },
                        remaining: {
                            amount: 0,
                            percentage: 0,
                        },
                    };

                    this.widgetPublishingProcessStatusKeys.forEach(statusKey => {
                        retVal[statusKey] = this.assembleNodeStateObject(
                            publishableEntityState,
                            queueInfoPerType,
                            statusKey,
                        );
                    });
                    return retVal;
                });
            });

            // notify change detection
            this.changeDetectorRef.markForCheck();
        }));
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.lifeSyncIntervall) {
            this.syncIntervall$.next(this.lifeSyncIntervall);
        }
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    /**
     * Tracking function for ngFor for better performance.
     */
    identify(index: number, state: WidgetPublishingProcessPerNodeComponentState): ContentMaintenanceType {
        return state.publishType;
    }

    private assembleNodeStateObject(
        publishableEntityState: WidgetPublishingProcessPerNodeComponentState,
        currentNodeState: PublishObjectsCount,
        key: keyof PublishObjectsCount,
    ): NodeState {
        // division by zero not allowed
        const percentageNew = publishableEntityState[`${key}Total`] > 0
            ? (currentNodeState[key] / publishableEntityState[`${key}Total`]) * 100
            : 0;
        return {
            amount: currentNodeState[key],
            percentage: percentageNew,
        };
    }
}
