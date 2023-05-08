import { Injectable } from '@angular/core';
import { Folder, Node } from '@gentics/cms-models';
import { EntityResolver } from '../entity-resolver/entity-resolver';

export type NodeBranch = {
    node: Node;
    folder: Folder;
    children: NodeBranch[];
};


@Injectable()
export class NodeHierarchyBuilder {

    constructor(private entityResolver: EntityResolver) { }

    /**
     * Converts a flat array of Nodes into an array of parent-child trees,
     * based on the channel inheritance data in the inheritedFromId property.
     */
    getNodeHierarchy(nodes: Node[]): NodeBranch[] {
        let branchesById: { [id: number]: NodeBranch } = {};
        let branches: NodeBranch[] = nodes.map(node => {
            let branch: NodeBranch = {
                node,
                children: [],
                folder: this.entityResolver.getFolder(node.folderId)
            };
            return branchesById[node.id] = branch;
        });

        let nonInheritedNodes: NodeBranch[] = [];
        for (let branch of branches) {
            let parentId = branch.node.inheritedFromId;
            if (parentId && parentId !== branch.node.id && branchesById[parentId]) {
                branchesById[parentId].children.push(branchesById[branch.node.id]);
            } else {
                nonInheritedNodes.push(branch);
            }
        }

        return nonInheritedNodes;
    }
}
