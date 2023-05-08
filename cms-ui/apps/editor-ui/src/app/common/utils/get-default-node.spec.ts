import {getDefaultNode} from './get-default-node';

describe('getDefaultNode()', () => {

    it('returns lowest id node', () => {
        const nodes: any[] = [
            { id: 4, type: 'node' },
            { id: 6, type: 'node' },
            { id: 3, type: 'node' },
            { id: 1, type: 'channel' }
        ];

        expect(getDefaultNode(nodes)).toBe(nodes[2]);
    });

    it('returns lowest id channel if no nodes', () => {
        const nodes: any[] = [
            { id: 3, type: 'channel' },
            { id: 1, type: 'channel' },
            { id: 4, type: 'channel' },
            { id: 6, type: 'channel' }
        ];

        expect(getDefaultNode(nodes)).toBe(nodes[1]);
    });

    it('does not mutate the input array', () => {
        const nodes: any[] = [
            { id: 3, type: 'channel' },
            { id: 1, type: 'channel' },
            { id: 2, type: 'channel' }
        ];
        const nodesClone = [...nodes];

        getDefaultNode(nodes);

        expect(nodes).toEqual(nodesClone);
    });

    it('returns undefined for empty input', () => {
        expect(getDefaultNode([])).toBeUndefined();
    });

});
