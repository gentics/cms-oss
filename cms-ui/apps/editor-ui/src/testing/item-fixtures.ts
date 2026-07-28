import { Folder, Item } from '@gentics/cms-models';

const NODE_ONE_ID = 123;
const NODE_TWO_ID = 456;

export function createLocalItem(): Item {
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

export function createInheritedItem(): Item {
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

export function createLocalizedItem(): Item {
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

/** Simulates old Content.Node versions with no "inheritedFromId" property */
export function itemWithoutInheritanceIds(item: Item): Item {
    const result = { ...item } as any;
    delete result.masterNodeId;
    delete result.inheritedFromId;
    return result;
}
