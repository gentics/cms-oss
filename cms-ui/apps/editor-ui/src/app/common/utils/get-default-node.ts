import {Node} from '../models';

/**
 * Given an array of nodes, return a single node which could be considered the "default" or starting node.
 * The algorithm is as follows:
 * - Consider only nodes, not channels, and select the node with the lowest id.
 * - If there are only channels, select the channel with the lowest id.
 */
export function getDefaultNode(nodes: Node[]): Node | undefined {
    const nodesOnly = nodes.filter(node => node.type === 'node');
    const candidates = 1 <= nodesOnly.length ? nodesOnly : nodes.slice();
    return candidates.sort((a, b) => a.id - b.id)[0];
}
