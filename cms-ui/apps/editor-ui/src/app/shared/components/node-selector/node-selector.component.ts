import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { Node } from '@gentics/cms-models';
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
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class NodeSelectorComponent implements OnChanges {

    /** An array of folders representing the root folder of each available node. */
    @Input()
    public nodes: Node[] = []

    /** The ID of the selected node. */
    @Input()
    public activeNodeId: number;

    /** Shows the name of the selected node or a small dropdown arrow (default). */
    @Input()
    public showName = false;

    /** Use router links inside the node selector (default) or only emit events. */
    @Input()
    public useLinks = true;

    /** Fired when a node has been selected. */
    @Output()
    public nodeSelected = new EventEmitter<Node>();

    public nodeBranches: NodeBranch[];
    public currentSelectedNode: Node;

    constructor(
        private entityResolver: EntityResolver,
        private hierarchyBuilder: NodeHierarchyBuilder,
    ) { }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.nodes) {
            this.nodeBranches = this.hierarchyBuilder.getNodeHierarchy(this.nodes);
        }
        if (changes.activeNodeId) {
            this.currentSelectedNode = this.entityResolver.getNode(this.activeNodeId);
        }
    }

    select(node: Node): void {
        this.nodeSelected.emit(node);
    }
}
