import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChange } from '@angular/core';
import { Node, Raw } from '@gentics/cms-models';
import { BehaviorSubject, combineLatest, Observable } from 'rxjs';
import { filter, flatMap, map } from 'rxjs/operators';
import { FilterService } from '../../services/filter/filter.service';
import { NodeStats } from '../../services/link-checker/link-checker.service';
import { NodeBranch, NodeHierarchyBuilderService } from '../../services/node-hierarchy-builder/node-hierarchy-builder.service';

/**
 * A component for selecting the current active node.
 */
@Component({
    selector: 'gtxct-node-selector',
    templateUrl: './node-selector.component.html',
    styleUrls: ['./node-selector.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})

export class NodeSelectorComponent implements OnInit {
    /** An array of folders representing the root folder of each available node. */
    @Input() set nodes(value: Node<Raw>[]) {
        if (value !== null) {
            this.nodes$.next(value);
        }
    }
    nodes$ = new BehaviorSubject<Node<Raw>[]>([]);

    @Input() stats: Observable<NodeStats>;

    /** The ID of the selected node. */
    @Input() set currentNodeId(value: number) {
        this.currentNodeId$.next(value);
    }
    currentNodeId$ = new BehaviorSubject<number>(null);

    /** Shows the name of the selected node or a small dropdown arrow (default). */
    @Input() showName = false;

    get node(): any {
        return this.nodes.find(node => node.id === this.filterService.options.nodeId);
    }

    nodeBranches$: Observable<NodeBranch[]>;
    currentSelectedNode$: Observable<Node<Raw>>;

    /** Fired when a node has been selected. */
    @Output() nodeSelected = new EventEmitter<number>();

    constructor(private filterService: FilterService,
                private hierarchyBuilder: NodeHierarchyBuilderService) {}

    ngOnInit(): void {
        this.nodeBranches$ = this.nodes$
            .pipe(map(nodes => this.hierarchyBuilder.getNodeHierarchy(nodes)));

        this.currentSelectedNode$ = combineLatest(this.currentNodeId$, this.nodes$)
            .pipe(
                filter(([currentNodeId, nodes]) => !!currentNodeId && !!nodes),
                flatMap(([currentNodeId, nodes]) => nodes.filter(node => node.id === currentNodeId))
            );
    }

    select(node: Node): void {
        this.nodeSelected.emit(node.id);
        this.nodes$.next(this.nodes$.getValue());
    }
}
