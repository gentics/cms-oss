import { ItemInNode, ItemRef } from '@gentics/cms-models';

export function sanitizeItemReference(item: ItemInNode): ItemRef {
    return {
        id: item.id,
        nodeId: item.nodeId,
        type: item.type,
        name: item.name,
    };
}
