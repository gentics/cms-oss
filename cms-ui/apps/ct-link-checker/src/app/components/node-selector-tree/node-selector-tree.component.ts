import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

import { ExternalLinkStatistics, Node } from '@gentics/cms-models';
import { NodeStats } from '../../services/link-checker/link-checker.service';
import { NodeBranch } from '../../services/node-hierarchy-builder/node-hierarchy-builder.service';

/**
 * A recursive component used to build up a tree view of nodes, visually nesting channels inside the master nodes
 * they inherit from.
 */
@Component ({
    selector: 'gtxct-node-selector-tree',
    templateUrl: './node-selector-tree.tpl.html',
    styleUrls: ['./node-selector-tree.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class NodeSelectorTreeComponent {
    @Input() branches: NodeBranch[] = [];
    @Input() depth = 0;
    /** If true, a routerLink will be used to make each list item a link */
    @Input() useLinks = true;
    @Output() nodeSelected = new EventEmitter<Node>();

    @Input() stats: NodeStats;

    getStats(nodeId: number): ExternalLinkStatistics {
        if (this.stats) {
            return this.stats[nodeId];
        } else {
            return {} as ExternalLinkStatistics;
        }
    }
}
