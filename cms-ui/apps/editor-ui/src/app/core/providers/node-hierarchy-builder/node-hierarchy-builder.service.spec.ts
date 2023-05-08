import {NodeBranch, NodeHierarchyBuilder} from './node-hierarchy-builder.service';

describe('NodeHierarchyBuilder', () => {

    describe('getNodeHierarchy()', () => {

        let entityResolver: FakeEntityResolver;
        let hierarchyBuilder: NodeHierarchyBuilder;
        beforeEach(() => {
            entityResolver = new FakeEntityResolver();
            hierarchyBuilder = new NodeHierarchyBuilder(entityResolver as any);
        });

        it('does not nest sibling nodes', () => {
            let master1 = createFakeNode(1, 'master1');
            let master2 = createFakeNode(2, 'master2');
            let nodeArray: any = [master1, master2];

            let branches = hierarchyBuilder.getNodeHierarchy(nodeArray);

            expect(branches).toEqual([
                { node: master1, folder: {}, children: [] } as NodeBranch,
                { node: master2, folder: {}, children: [] } as NodeBranch
            ]);
        });

        it('nests a single child node', () => {
            let master1 = createFakeNode(1, 'master1');
            let channel1 = createFakeNode(2, 'channel1', 1, 1);
            let nodeArray: any = [master1, channel1];

            let branches = hierarchyBuilder.getNodeHierarchy(nodeArray);

            expect(branches).toEqual([
                { node: master1, folder: {}, children: [
                    { node: channel1, folder: {}, children: [] }
                ] } as NodeBranch
            ]);
        });

        it('nests 2 levels of child nodes', () => {
            let master1 = createFakeNode(1, 'master1');
            let channel1 = createFakeNode(2, 'channel1', 1, 1);
            let channel2 = createFakeNode(3, 'channel2', 2, 1);
            let nodeArray: any = [master1, channel1, channel2];

            let branches = hierarchyBuilder.getNodeHierarchy(nodeArray);

            expect(branches).toEqual([
                { node: master1, folder: {}, children: [
                    { node: channel1, folder: {}, children: [
                        { node: channel2, folder: {}, children: [] }
                    ] }
                ] } as NodeBranch
            ]);
        });

        it('nests 3 levels of child nodes', () => {
            let master1 = createFakeNode(1, 'master1');
            let channel1 = createFakeNode(2, 'channel1', 1, 1);
            let channel2 = createFakeNode(3, 'channel2', 2, 1);
            let channel3 = createFakeNode(4, 'channel3', 3, 1);
            let nodeArray: any = [master1, channel1, channel2, channel3];

            let branches = hierarchyBuilder.getNodeHierarchy(nodeArray);

            expect(branches).toEqual([
                { node: master1, folder: {}, children: [
                    { node: channel1, folder: {}, children: [
                        { node: channel2, folder: {}, children: [
                            { node: channel3, folder: {}, children: [] }
                        ] }
                    ] }
                ] } as NodeBranch
            ]);
        });

        it('correctly nests with unordered source array', () => {
            let master1 = createFakeNode(1, 'master1');
            let channel1 = createFakeNode(2, 'channel1', 1, 1);
            let channel2 = createFakeNode(3, 'channel2', 2, 1);
            let channel3 = createFakeNode(4, 'channel3', 3, 1);
            let expected: any = [
                { node: master1, folder: {}, children: [
                    { node: channel1, folder: {}, children: [
                        { node: channel2, folder: {}, children: [
                            { node: channel3, folder: {}, children: [] }
                        ] }
                    ] }
                ] }
            ];

            expect(hierarchyBuilder.getNodeHierarchy([master1, channel1, channel2, channel3])).toEqual(expected);
            expect(hierarchyBuilder.getNodeHierarchy([channel3, channel2, channel1, master1])).toEqual(expected);
            expect(hierarchyBuilder.getNodeHierarchy([channel2, channel1, master1, channel3])).toEqual(expected);
        });

        it('nests multiple channels in a single master', () => {
            let master1 = createFakeNode(1, 'master1');
            let channel1 = createFakeNode(2, 'channel1', 1, 1);
            let channel2 = createFakeNode(3, 'channel2', 1, 1);
            let channel3 = createFakeNode(4, 'channel3', 1, 1);
            let nodeArray = [master1, channel1, channel2, channel3];

            let branches = hierarchyBuilder.getNodeHierarchy(nodeArray);

            expect(branches).toEqual([
                { node: master1, folder: {}, children: [
                    { node: channel1, folder: {}, children: [] },
                    { node: channel2, folder: {}, children: [] },
                    { node: channel3, folder: {}, children: [] }
                ] } as NodeBranch
            ]);
        });

        it('handles a mixture of tree shapes', () => {
            let master1 = createFakeNode(1, 'master1');
            let channel11 = createFakeNode(2, 'channel1_1', 1, 1);
            let channel12 = createFakeNode(3, 'channel1_2', 1, 1);
            let master2 = createFakeNode(4, 'master2');
            let master3 = createFakeNode(5, 'master3');
            let channel31 = createFakeNode(6, 'channel3_1', 5, 5);
            let channel311 = createFakeNode(7, 'channel3_1_1', 6, 5);

            let nodeArray: any[] = [
                master1,
                channel11,
                channel12,
                master2,
                master3,
                channel31,
                channel311
            ];

            let branches = hierarchyBuilder.getNodeHierarchy(nodeArray);

            expect(branches).toEqual([
                { node: master1, folder: {}, children: [
                    { node: channel11, folder: {}, children: [] },
                    { node: channel12, folder: {}, children: [] }
                ] } as NodeBranch,
                { node: master2, folder: {}, children: [] } as NodeBranch,
                { node: master3, folder: {}, children: [
                    { node: channel31, folder: {}, children: [
                        { node: channel311, folder: {}, children: [] }
                    ]}
                ]} as NodeBranch
            ]);
        });

        it('works when not all parent nodes are visible to the user', () => {
            let master1 = undefined; // id: 1
            let channel11 = createFakeNode(2, 'channel1_1', 1, 1);
            let channel111 = createFakeNode(3, 'channel1_1', 2, 1);
            let channel12 = createFakeNode(4, 'channel1_2', 1, 1);

            let nodeArray: any[] = [
                channel11,
                channel111,
                channel12
            ];

            let branches = hierarchyBuilder.getNodeHierarchy(nodeArray);

            expect(branches).toEqual([
                { node: channel11, folder: {}, children: [
                    { node: channel111, folder: {}, children: [] }
                ] } as NodeBranch,
                { node: channel12, folder: {}, children: [] } as NodeBranch
            ]);
        });

    });
});


function createFakeNode(id: number, name: string, parent?: number, master?: number): any {
    return {
        id,
        name,
        inheritedFromId: parent || id,
        masterNodeId: master || id
    };
}


class FakeEntityResolver {
    getFolder(id: number): any {
        return {};
    }
}
