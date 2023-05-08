import {appendTypeIdToUrl} from './common';

describe('ContentFrame common functions', () => {

    describe('appendTypeIdToUrl()', () => {

        let unaffectedUrl = '/.Node/?sid=15674&time=1477301498&errchk=true&edit_id=440';
        let affectedUrl = unaffectedUrl + '&do=10010';

        it('does not modify urls without the correct "do" code', () => {
            let mockPage: any = { id: 1, type: 'page' };
            let result = appendTypeIdToUrl(mockPage, unaffectedUrl);
            expect(result).toEqual(unaffectedUrl);
        });

        it('handles unexpected input', () => {
            let mockPage: any = { id: 1, type: 'page' };

            expect(appendTypeIdToUrl(mockPage, <any> undefined)).toBeUndefined('undefined');
            expect(appendTypeIdToUrl(mockPage, <any> null)).toBeNull('null');
            expect(appendTypeIdToUrl(mockPage, <any> 1)).toBe(<any> 1, 'number');
            expect(appendTypeIdToUrl(mockPage, <any> [1, 2, 3])).toEqual(<any> [1, 2, 3], 'array');
            expect(appendTypeIdToUrl(mockPage, <any> {})).toEqual(<any> {}, 'object');
        });

        it('adds type ids to affected urls', () => {
            const mockEntity = (type: string): any => ({ id: 1, type });

            expect(appendTypeIdToUrl(mockEntity('folder'), affectedUrl)).toEqual(affectedUrl + '&FOLDER_ID=1', 'folder');
            expect(appendTypeIdToUrl(mockEntity('page'), affectedUrl)).toEqual(affectedUrl + '&PAGE_ID=1', 'page');
            expect(appendTypeIdToUrl(mockEntity('file'), affectedUrl)).toEqual(affectedUrl + '&FILE_ID=1', 'file');
            expect(appendTypeIdToUrl(mockEntity('image'), affectedUrl)).toEqual(affectedUrl + '&FILE_ID=1', 'image');
        });
    });
});
