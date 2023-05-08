import { File, Folder, Image, Item, Page } from '@gentics/cms-models';

export function itemIsLocalized(item: Folder | Page | File | Image | Item | undefined): boolean;
export function itemIsLocalized(item: any): boolean {
    if (!item || item.inherited) {
        return false;
    }

    if (item.inheritedFromId) {
        return item.inheritedFromId !== item.masterNodeId;
    } else {
        return item.inheritedFrom !== item.masterNode;
    }
}
