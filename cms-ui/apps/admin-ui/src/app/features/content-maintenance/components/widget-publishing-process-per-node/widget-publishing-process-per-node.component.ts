import { AdminOperations, ErrorHandler, I18nNotificationService } from '@admin-ui/core';
import { NodeDataService } from '@admin-ui/shared';
import { animate, state, style, transition, trigger } from '@angular/animations';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChanges
} from '@angular/core';
import { Node, PublishQueue, Raw } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';
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

    @Input()
    public queueStatus: PublishQueue;

    /** emits selected node IDs on checkbox clicked */
    @Output()
    selectedIdsChange = new EventEmitter<number[]>();

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

        // this.adminOps.getPublishQueue()
        // set loading indicator
        // this.tableIsLoading = false;

        // const infoNodesKeys = info && Object.keys(info.nodes);

        // if (!infoNodesKeys.length || !allNodes.length) {
        //     return;
        // }
        // // assemble component state
        // infoNodesKeys.forEach((nodeId: string) => {
        //     const node: Node<Raw> = allNodes.find(node => node.id.toString() === nodeId);

        //     const isSelected: boolean = this.selectedIds.includes(parseInt(nodeId, 10));
        //     if (this.infoStatsPerNodeState[nodeId]) {
        //         this.infoStatsPerNodeState[nodeId].selected = isSelected;
        //     } else {
        //         this.infoStatsPerNodeState[nodeId] = {
        //             nodeId,
        //             name: node.name,
        //             selected: isSelected,
        //             collapsed: false,
        //             hidden: this.infoStatsPerNodeState[nodeId]?.hidden ?? false,
        //             disablePublish: node.disablePublish,
        //         };
        //     }
        // });
    }

    ngOnChanges(changes: SimpleChanges): void {

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
