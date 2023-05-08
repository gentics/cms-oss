import {Folder, Page, File, Image, Item} from '../models';
import {AppState} from '../models/app-state';

export function nodeIdOfItem(item: Folder | Page | File | Image | Item, appState: AppState): number;
export function nodeIdOfItem(item: Folder | Page | File | Image, appState: AppState): number {
    if (!item.inherited && item.inheritedFromId) {
        return item.inheritedFromId;
    }

    // If an items path is "/Node Name/Folder", we parse it for the node name
    const nodeName = item.path.split('/')[1];
    const nodes = appState.entities.node;
    return Object.keys(nodes).map(Number).find(nodeId => nodes[nodeId].name === nodeName);
}
