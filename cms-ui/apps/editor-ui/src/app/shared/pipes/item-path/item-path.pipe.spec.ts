import {ItemPathPipe} from './item-path.pipe';

let itemPathPipe = new ItemPathPipe();
let testItems: any;

describe('ItemPathPipe:', () => {

    beforeEach(() => {
        testItems = [
            {
                id: 1,
                type: 'folder',
                path: '/node/parent/myFolder/',
                name: 'myFolder',
                hasSubfolders: true
            },
            {
                id: 2,
                type: 'page',
                path: '/node/parent/',
                publishPath: '/Content.Node/pages/myPage.en.html',
                fileName: 'myPage.en.html'
            },
            {
                id: 3,
                type: 'file',
                path: '/node/parent/',
                publishPath: '/Content.Node/files/myFile.doc',
                fileName: 'myFile.doc'
            },
            {
                id: 4,
                type: 'image',
                path: '/node/parent/',
                publishPath: '/Content.Node/images/someImage.jpg',
                fileName: 'someImage.jpg'
            }
        ];
    });

    it('should display the publish path for a page', () => {
        expect(itemPathPipe.transform(testItems[1])).toBe('/Content.Node/pages/myPage.en.html');
    });
    it('should display the publish path for a file', () => {
        expect(itemPathPipe.transform(testItems[2])).toBe('/Content.Node/files/myFile.doc');
    });
    it('should display the publish path for an image', () => {
        expect(itemPathPipe.transform(testItems[3])).toBe('/Content.Node/images/someImage.jpg');
    });
});
