import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Node } from '@gentics/cms-models';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { NodeBranch, NodeHierarchyBuilder } from '../../../core/providers/node-hierarchy-builder/node-hierarchy-builder.service';

/**
 * A component for selecting the current active node.
 */
@Component({
    selector: 'node-selector',
    templateUrl: './node-selector.tpl.html',
    styleUrls: ['./node-selector.scss'],
    providers: [NodeHierarchyBuilder],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class NodeSelector implements OnInit {
    /** An array of folders representing the root folder of each available node. */
    @Input() set nodes(value: Node[]) {
        this.nodes$.next(value);
    }
    nodes$ = new BehaviorSubject<Node[]>([]);

    /** The ID of the selected node. */
    @Input() set activeNodeId(value: number) {
        this.activeNodeId$.next(value);
    }
    activeNodeId$ = new BehaviorSubject<number>(null);

    /** Fired when a node has been selected. */
    @Output() nodeSelected = new EventEmitter<Node>();

    /** Shows the name of the selected node or a small dropdown arrow (default). */
    @Input() showName = false;

    /** Use router links inside the node selector (default) or only emit events. */
    @Input() useLinks = true;

    nodeBranches$: Observable<NodeBranch[]>;
    currentSelectedNode$: Observable<Node>;

    constructor(
        private entityResolver: EntityResolver,
        private hierarchyBuilder: NodeHierarchyBuilder
    ) { }

    ngOnInit(): void {

        this.nodeBranches$ = this.nodes$
            .pipe(map(nodes => this.hierarchyBuilder.getNodeHierarchy(nodes)));

        this.currentSelectedNode$ = this.activeNodeId$
            .pipe(map(activeNodeId => this.entityResolver.getNode(activeNodeId)));
    }

    select(node: Node): void {
        this.nodeSelected.emit(node);
    }
}
