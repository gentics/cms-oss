import {parentFolderOfItem} from './parent-folder-of-item';

describe('parentFolderOfItem', () => {

    it('returns folderId as parent folder', () => {
        const item: any = {
            folderId: 14
        };
        expect(parentFolderOfItem(item)).toBe(14);
    });

    it('returns motherId as parent folder', () => {
        const item: any = {
            motherId: 11,
            folderId: 14
        };
        expect(parentFolderOfItem(item)).toBe(11);
    });

});
