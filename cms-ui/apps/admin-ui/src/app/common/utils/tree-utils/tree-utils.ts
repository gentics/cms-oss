/**
 * Visits every node of a tree in pre-order, i.e. a parent before its children and children in
 * array order. Purely a traversal - it returns nothing and does not copy or reorder anything.
 * @param nodes The root nodes to walk. `null`/`undefined` is treated as an empty tree.
 * @param visit Called exactly once per node.
 */
export function forEachTreeNode<T extends { children?: T[] }>(nodes: T[] | undefined, visit: (node: T) => void): void {
    for (const node of nodes || []) {
        visit(node);
        forEachTreeNode(node.children, visit);
    }
}
