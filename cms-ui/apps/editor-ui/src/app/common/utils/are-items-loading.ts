import { FolderState, ItemsInfo } from '../../common/models';

/**
 * Returns true if any of the entities in the "folder" state branch are currently
 * undergoing some async process.
 */
export function areItemsLoading(folderState: FolderState): boolean {
    for (let type of ['nodes', 'folders', 'pages', 'files', 'images', 'breadcrumbs']) {
        let info: ItemsInfo = (<any> folderState)[type];
        if (info && (info.creating || info.fetching || info.saving || info.deleting.length > 0)) {
            return true;
        }
    }
    return false;
}
