import { PUBLISH_PROCESS_REFRESH_INTERVAL } from '@admin-ui/common';
import { AdminOperations, ErrorHandler, I18nNotificationService } from '@admin-ui/core';
import { NodeDataService } from '@admin-ui/shared';
import { animate, state, style, transition, trigger } from '@angular/animations';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { Node, PublishQueue, Raw } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { isEqual } from 'lodash';
import { BehaviorSubject, Observable, Subscription, timer } from 'rxjs';
import { catchError, distinctUntilChanged, filter, map, mergeMap, switchMap, tap } from 'rxjs/operators';
import { MaintenanceActionModalAction, MaintenanceActionModalComponent } from '../maintenance-action-modal/maintenance-action-modal.component';

/**
 * A table dynmically fetching and displaying all nodes and their publish queue status details.
 */
@Component({
    selector: 'gtx-widget-publishing-process-per-node',
    templateUrl: './widget-publishing-process-per-node.component.html',
    styleUrls: ['./widget-publishing-process-per-node.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: [
    trigger('slideAnim', [
        state('in', style({
            opacity: 1,
            height: '*',
            'padding-top': '*',
            'padding-bottom': '*',
            'margin-top': '*',
            'margin-bottom': '*',
        })),
        transition(':enter', [
            style({
                opacity: 0,
                height: '0rem',
                'padding-top': '0',
                'padding-bottom': '0',
                'margin-top': '0',
                'margin-bottom': '0',
                }),
            animate(100),
        ]),
        transition(':leave',
            animate(100, style({
                opacity: 0,
                height: '0rem',
                'padding-top': '0',
                'padding-bottom': '0',
                'margin-top': '0',
                'margin-bottom': '0',
            })),
        ),
        ]),
    ],
})
export class WidgetPublishingProcessPerNodeComponent implements OnInit, OnChanges, OnDestroy {

    public readonly MaintenanceActionModalAction = MaintenanceActionModalAction;

    /** If TRUE node rows are selectable and component emits IDs on selection changed. */
    @Input()
    selectable = false;

    /** node IDs selected in component */
    @Input()
    selectedIds: number[] = [];

    /** If TRUE component polls and refreshs the data display in an intervall defined in `lifeSyncIntervall` */
    @Input()
    lifeSyncEnabled = true;

    /** Determines the amount of seconds between polling information. */
    @Input()
    public lifeSyncIntervall: number;

    /** emits selected node IDs on checkbox clicked */
    @Output()
    selectedIdsChange = new EventEmitter<number[]>();

    /** DOM element of table */
    @ViewChild('table')
    tableElementView: ElementRef<HTMLDivElement>;

    /** All nodes currently existing in global app state */
    allNodes$: Observable<Node<Raw>[]>;

    /** information of the current publish process per node */
    infoStatsPerNodeData$ = new BehaviorSubject<PublishQueue>(null);

    /** internal component state */
    infoStatsPerNodeState: {
        [id: number]: {
            nodeId: number;
            name: string;
            selected: boolean;
            collapsed: boolean;
            hidden: boolean;
            disablePublish: boolean;
        }
    } = {};

    /** If TRUE table is in loading state. */
    tableIsLoading = true;

    allCollapsed = true;

    searchTerm = '';

    tableElementViewHeight: number;

    private syncIntervall$ = new BehaviorSubject<number>(PUBLISH_PROCESS_REFRESH_INTERVAL);

    private subscriptions: Subscription[] = [];

    constructor(
        private adminOps: AdminOperations,
        private nodeDataService: NodeDataService,
        private errorHandler: ErrorHandler,
        private modalService: ModalService,
        private notification: I18nNotificationService,
        private adminOperations: AdminOperations,
        private changeDetectorRef: ChangeDetectorRef,
    ) { }

    public ngOnInit(): void {
        this.allNodes$ = this.nodeDataService.watchAllEntities().pipe(
            map(allNodes => allNodes.sort((a, b) => a.name.localeCompare(b.name))),
        );

        const intervall$ = this.syncIntervall$.pipe(
            distinctUntilChanged(isEqual),
            switchMap(milliseconds => timer(0, milliseconds)),
            filter(() => this.lifeSyncEnabled),
            // start loading indicator
            tap(() => this.tableIsLoading = true),
        );

        // initialize data stream of node publish status info
        this.subscriptions.push(intervall$.pipe(
            // request data
            switchMap(() => this.adminOps.getPublishQueue()),
            // validate response
            filter(data => data instanceof Object),
            // get all nodes from state
            mergeMap(data => this.allNodes$.pipe(map(allNodes => [data, allNodes]))),

            catchError(error => this.errorHandler.catch(error)),
        ).subscribe(([info, allNodes]: [PublishQueue, Node<Raw>[]]) => {
            // emit latest data
            this.infoStatsPerNodeData$.next(info);

            // set loading indicator
            this.tableIsLoading = false;

            const infoNodesKeys = info && Object.keys(info.nodes);

            if (!infoNodesKeys.length || !allNodes.length) {
                return;
            }
            // assemble component state
            infoNodesKeys.forEach((nodeId: string) => {
                const node: Node<Raw> = allNodes.find(node => node.id.toString() === nodeId);

                const isSelected: boolean = this.selectedIds.includes(parseInt(nodeId, 10));
                if (this.infoStatsPerNodeState[nodeId]) {
                    this.infoStatsPerNodeState[nodeId].selected = isSelected;
                } else {
                    this.infoStatsPerNodeState[nodeId] = {
                        nodeId,
                        name: node.name,
                        selected: isSelected,
                        collapsed: false,
                        hidden: this.infoStatsPerNodeState[nodeId]?.hidden ?? false,
                        disablePublish: node.disablePublish,
                    };
                }
            });

            // notify change detection
            this.changeDetectorRef.markForCheck();
            // set min height for container element to prevent page from scrolling during filter
            this.tableElementViewHeight = this.tableElementView.nativeElement.offsetHeight;
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
    identify(index: number, node: Node<Raw>): number {
        return node.id;
    }

    checkAll(value: boolean): void {
        if (value) {
            Object.keys(this.infoStatsPerNodeState).forEach(nodeId => {
                this.infoStatsPerNodeState[nodeId].selected = true;
                this.selectedIds.push(parseInt(nodeId, 10));
            });
        } else {
            Object.keys(this.infoStatsPerNodeState).forEach(nodeId => {
                this.infoStatsPerNodeState[nodeId].selected = false;
            });
            this.selectedIds = [];
        }
        this.selectedIdsChange.emit(this.selectedIds);
    }

    /**
     * Collapses or expands node data row.
     *
     * @param nodeId to adress component state segment
     */
    toggleRow(nodeId: number): void {
        if (this.infoStatsPerNodeState[nodeId]) {
            this.infoStatsPerNodeState[nodeId].collapsed = !this.infoStatsPerNodeState[nodeId].collapsed;
        }
    }

    toggleRowAll(): void {
        this.allCollapsed = !this.allCollapsed;
        Object.values(this.infoStatsPerNodeState).forEach(nodeState => {
            nodeState.collapsed = !this.allCollapsed;
        });
    }

    checkRow(nodeId: number): void {
        const alreadySelected = this.selectedIds.includes(nodeId);
        if (this.infoStatsPerNodeState[nodeId]) {
            this.infoStatsPerNodeState[nodeId].selected = alreadySelected ? false : true;
        }
        this.selectedIds = Object.keys(this.infoStatsPerNodeState)
            .filter(nodeId => this.infoStatsPerNodeState[nodeId].selected)
            .map(i => parseInt(i, 10));
        this.selectedIdsChange.emit(this.selectedIds);
    }

    public async btnMaintenanceActionClicked(modalAction: MaintenanceActionModalAction): Promise<void> {
        // ReloadConfiguration doesn't require a modal
        if (modalAction === MaintenanceActionModalAction.RELOAD_CONFIGURATION) {
            return this.adminOperations.reloadConfiguration().toPromise().then(() => {});
        }

        // StopPublishing doesn't require a modal
        if (modalAction === MaintenanceActionModalAction.STOP_PUBLISHING) {
            return this.adminOperations.stopPublishing().toPromise().then(() => {});
        }

        if (this.selectedIds.length === 0) {
            this.notification.show({
                message: 'contentmaintenance.error_no_nodes_selected',
                type: 'warning',
            });
            return;
        }
        this.modalService.fromComponent(
            MaintenanceActionModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
            { modalAction, selectedNodeIds: this.selectedIds },
        )
            .then(modal => modal.open())
            .catch(this.errorHandler.catch);
    }

    filterNodes(searchTerm: string): void {
        this.searchTerm = searchTerm;
        Object.keys(this.infoStatsPerNodeState).forEach(nodeId => {
            const currentNode = this.infoStatsPerNodeState[nodeId];
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            currentNode.hidden = currentNode.name.match(new RegExp(`.*${searchTerm}.*`, 'gi')) ? false : true;
        });
    }

}
