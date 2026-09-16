import { forEachTreeNode } from './tree-utils';

interface TestNode {
    id: string;
    children?: TestNode[];
}

/**
 * ```
 * a
 * |- b
 * |  '- d
 * '- c
 * ```
 */
function createTree(): TestNode[] {
    return [{
        id: 'a',
        children: [
            { id: 'b', children: [{ id: 'd' }] },
            { id: 'c', children: [] },
        ],
    }];
}

describe('tree-utils', () => {

    describe('forEachTreeNode()', () => {

        it('visits the nodes in pre-order', () => {
            const visited: string[] = [];

            forEachTreeNode(createTree(), (node) => visited.push(node.id));

            expect(visited).toEqual(['a', 'b', 'd', 'c']);
        });

        it('visits every node exactly once', () => {
            const visited: TestNode[] = [];

            forEachTreeNode(createTree(), (node) => visited.push(node));

            expect(visited.length).toBe(4);
            expect(new Set(visited).size).toBe(4);
        });

        it('walks multiple root nodes', () => {
            const visited: string[] = [];

            forEachTreeNode([{ id: 'a' }, { id: 'b', children: [{ id: 'c' }] }], (node) => visited.push(node.id));

            expect(visited).toEqual(['a', 'b', 'c']);
        });

        it('does nothing for an empty, null or undefined tree', () => {
            const visit = jasmine.createSpy('visit');

            forEachTreeNode([], visit);
            forEachTreeNode(null, visit);
            forEachTreeNode(undefined, visit);

            expect(visit).not.toHaveBeenCalled();
        });

        it('treats a missing children property as a leaf', () => {
            const visited: string[] = [];

            forEachTreeNode([{ id: 'a', children: undefined }], (node) => visited.push(node.id));

            expect(visited).toEqual(['a']);
        });
    });
});
