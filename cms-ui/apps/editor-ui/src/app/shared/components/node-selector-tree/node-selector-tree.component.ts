import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Node } from '@gentics/cms-models';
import { NodeBranch } from '../../../core/providers/node-hierarchy-builder/node-hierarchy-builder.service';

/**
 * A recursive component used to build up a tree view of nodes, visually nesting channels inside the master nodes
 * they inherit from.
 */
@Component ({
    selector: 'node-selector-tree',
    templateUrl: './node-selector-tree.tpl.html',
    styleUrls: ['./node-selector-tree.scss'],
})
export class NodeSelectorTree {

    @Input()
    public branches: NodeBranch[] = [];

    @Input()
    public depth = 0;

    /** If true, a routerLink will be used to make each list item a link */
    @Input()
    public useLinks = true;

    @Output()
    public nodeSelected = new EventEmitter<Node>();
}
