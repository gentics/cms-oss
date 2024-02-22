import { File, Folder, Image, Item, Page } from '@gentics/cms-models';

export function parentFolderOfItem(item: Folder | Page | File | Image | Item): number {
    return (item as Folder).motherId || (item as Page).folderId;
}
