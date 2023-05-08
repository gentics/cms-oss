import { Injectable } from '@angular/core';

import { Folder, Node } from '@gentics/cms-models';

export interface NodeBranch {
    node: Node;
    children: NodeBranch[];
}


@Injectable()
export class NodeHierarchyBuilderService {

    folders: Folder[] = [];

    constructor() { }

    /**
     * Converts a flat array of Nodes into an array of parent-child trees,
     * based on the channel inheritance data in the inheritedFromId property.
     */
    getNodeHierarchy(nodes: Node[]): NodeBranch[] {
        const branchesById: { [id: number]: NodeBranch } = {};
        const branches: NodeBranch[] = nodes.map(node => {
            const branch: NodeBranch = {
                node,
                children: []
            };
            return branchesById[node.id] = branch;
        });

        const nonInheritedNodes: NodeBranch[] = [];
        for (const branch of branches) {
            const parentId = branch.node.inheritedFromId;
            if (parentId && parentId !== branch.node.id && branchesById[parentId]) {
                branchesById[parentId].children.push(branchesById[branch.node.id]);
            } else {
                nonInheritedNodes.push(branch);
            }
        }

        return nonInheritedNodes;
    }

}
