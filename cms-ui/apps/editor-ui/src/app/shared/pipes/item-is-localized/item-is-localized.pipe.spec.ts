import { Folder, Item } from '@gentics/cms-models';
import { ItemIsLocalizedPipe } from './item-is-localized.pipe';

describe('IsLocalizedPipe', () => {
    let pipe: ItemIsLocalizedPipe;

    beforeEach(() => {
        pipe = new ItemIsLocalizedPipe();
    });

    it('can be created', () => {
        expect(pipe).toBeDefined();
    });

    describe('with Content.Node >= 5.27.0', () => {

        it('returns false for a local, non-inherited item', () => {
            const localItem = createLocalItem();
            const actual = pipe.transform(localItem);
            expect(actual).toBe(false);
        });

        it('returns false for an inherited item', () => {
            const inheritedItem = createInheritedItem();
            const actual = pipe.transform(inheritedItem);
            expect(actual).toBe(false);
        });

        it('returns true for a localized item', () => {
            const localizedItem = createLocalizedItem();
            const actual = pipe.transform(localizedItem);
            expect(actual).toBe(true);
        });

    });

    describe('with ContentNode 5.26.x', () => {

        it('returns false for a local, non-inherited item', () => {
            const localItem = withoutInheritanceIds(createLocalItem());
            const actual = pipe.transform(localItem);
            expect(actual).toBe(false);
        });

        it('returns false for an inherited item', () => {
            const inheritedItem = withoutInheritanceIds(createInheritedItem());
            const actual = pipe.transform(inheritedItem);
            expect(actual).toBe(false);
        });

        it('returns true for a localized item', () => {
            const localizedItem = withoutInheritanceIds(createLocalizedItem());
            const actual = pipe.transform(localizedItem);
            expect(actual).toBe(true);
        });

    });

});

const NODE_ONE_ID = 123;
const NODE_TWO_ID = 456;

function createLocalItem(): Item {
    const localFolder: Partial<Folder> = {
        type: 'folder',
        inherited: false,
        inheritedFromId: NODE_ONE_ID,
        inheritedFrom: 'Node One',
        masterNodeId: NODE_ONE_ID,
        masterNode: 'Node One',
        path: '/Node One/Folder/',
    };
    return localFolder as Folder;
}

function createInheritedItem(): Item {
    const inheritedFolder: Partial<Folder> = {
        type: 'folder',
        inherited: true,
        inheritedFromId: NODE_ONE_ID,
        inheritedFrom: 'Node One',
        masterNodeId: NODE_ONE_ID,
        masterNode: 'Node One',
        path: '/Node Two/Folder/',
    };
    return inheritedFolder as Folder;
}

function createLocalizedItem(): Item {
    const localizedFolder: Partial<Folder> = {
        type: 'folder',
        inherited: false,
        inheritedFromId: NODE_TWO_ID,
        inheritedFrom: 'Node Two',
        masterNodeId: NODE_ONE_ID,
        masterNode: 'Node One',
        path: '/Node Two/Folder/',
    };
    return localizedFolder as Folder;
}

function withoutInheritanceIds(item: Item): Item {
    const result = { ...item } as any;
    delete result.masterNodeId;
    delete result.inheritedFromId;
    return result;
}
