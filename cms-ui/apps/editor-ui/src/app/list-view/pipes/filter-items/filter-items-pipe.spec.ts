import { Page } from '@gentics/cms-models';
import { getExamplePageData } from '../../../../testing/test-data.mock';
import { FilterItemsPipe } from './filter-items.pipe';


describe('FilterItemsPipe:', () => {

    let filterPipe: FilterItemsPipe;
    let items: Page[];
    beforeEach(() => {
        filterPipe = new FilterItemsPipe();
        items = [
            getExamplePageData({ id: 1 }),
            getExamplePageData({ id: 2 }),
            getExamplePageData({ id: 3 }),
            getExamplePageData({ id: 4 }),
            getExamplePageData({ id: 5 })
        ];

        items[0].name = 'First page';
        items[1].name = 'Second page';
        items[2].name = 'Third page';
        items[3].name = 'Fourth page';
        items[4].name = 'Fifth page';

        items[0].fileName = 'page1.html';
        items[1].fileName = 'page2.html';
        items[2].fileName = 'page3.html';
        items[3].fileName = 'page4.html';
        items[4].fileName = 'page5.css';
    });

    it('returns the input array when filter is empty', () => {
        const result = filterPipe.transform(items, '', true);
        expect(result).toBe(items);
    });

    it('returns right results if showPath is true', () => {
        const result = filterPipe.transform(items, 'html', true);
        const expected = items.slice(0, 4);

        expect(result).toEqual(expected);
    });

    it('returns no results if showPath is false', () => {
        const result = filterPipe.transform(items, 'html', false);

        expect(result.length).toEqual(0);
    });

    it('returns the input array when filter is not a string', () => {
        for (let testValue of [undefined, null, 0, 1, -1, true, false]) {
            const result = filterPipe.transform(items, testValue as any, true);
            expect(result).toBe(items, testValue);
        }
    });

    it('always returns the same reference when the input array is empty', () => {
        const firstResult = filterPipe.transform(null as any, 'text to filter for', true);
        const secondResult = filterPipe.transform('' as any, 'text to filter for', true);
        const thirdResult = filterPipe.transform([], 'text to filter for', true);
        expect(firstResult).toBe(secondResult, 'first !== second');
        expect(secondResult).toBe(thirdResult, 'second !== third');
    });

    it('returns the same reference when a different input array holds the same references', () => {
        const firstResult = filterPipe.transform(items, '', true);
        const differentInputArray = [...items];
        const secondResult = filterPipe.transform(differentInputArray, '', true);

        expect(firstResult === secondResult).toBe(true, 'first !== second');
    });

    it('returns a new array when a filter is passed', () => {
        const result = filterPipe.transform(items, 'text to filter for', true);
        expect(result === items).toBe(false);
    });

    it('returns only the items that match the filter', () => {
        const result = filterPipe.transform(items, 'text to filter for', true);
        expect(result).not.toBe(items);
    });

    it('returns items with a name that matches the filter', () => {
        const result = filterPipe.transform(items, 'th page', true);
        expect(result.length).toBe(2);
        expect(result[0] === items[3]).toBe(true, '[0] !== fourth item');
        expect(result[1] === items[4]).toBe(true, '[1] !== fifth item');
    });

    it('returns items with a filename that matches the filter', () => {
        items[4].fileName = 'lorem-ipsum-dolor.doc';
        const result = filterPipe.transform(items, 'ipsum', true);
        expect(result.length).toBe(1);
        expect(result[0]).toEqual(items[4]);
    });

    it('matches the item name case-insensitive', () => {
        items[2].name = 'First eXAMPle for case-insensitive filtering';
        items[3].name = 'Second example for case-insensitive filtering';

        const result = filterPipe.transform(items, 'Example', true);
        expect(result.length).toBe(2);
        expect(result[0] === items[2]).toBe(true, '[0] !== items[2]');
        expect(result[1] === items[3]).toBe(true, '[1] !== items[3]');
    });

    it('matches the filename case-insensitive', () => {
        items[2].fileName = 'Just-An-Example-For-Testing.txt';
        items[3].fileName = 'ANOTHER_EXAMPLE_FOR_TESTING.TXT';
        items[4].fileName = 'yet_another_example_filename.js';

        const result = filterPipe.transform(items, 'Example', true);
        expect(result.length).toBe(3);
        expect(result[0] === items[2]).toBe(true, '[0] != items[2]');
        expect(result[1] === items[3]).toBe(true, '[1] != items[3]');
        expect(result[2] === items[4]).toBe(true, '[2] != items[4]');
    });

    it('returns the same array reference if the same input is passed', () => {
        items[1].name = 'important and should be found';
        items[3].name = 'important and should be found';

        const firstResult = filterPipe.transform(items, 'important', true);
        items[1].name = 'page name modified but same reference';
        items[2].name = 'page name modified but same reference';

        const secondResult = filterPipe.transform(items, 'important', true);
        expect(firstResult.length).toBe(2, 'first');
        expect(secondResult.length).toBe(2, 'second');
        expect(firstResult === secondResult).toBe(true);
    });

    it('returns the same array reference if the result is the same', () => {
        items[1].name = 'important and should be found';
        items[3].name = 'important and should be found';

        const firstResult = filterPipe.transform(items, 'important', true);
        const secondResult = filterPipe.transform(items, 'important', true);
        expect(firstResult.length).toBe(2, 'first');
        expect(secondResult.length).toBe(2, 'second');
        expect(firstResult === secondResult).toBe(true);
    });

    it('returns a new array if an item changed (by reference)', () => {
        items[2].name = 'this page is important and can be found';
        items[3].name = 'this page is important and will be changed';

        const firstResult = filterPipe.transform(items, 'important', true);
        const changedItems = [...items];
        changedItems[3] = { ...items[3], folderId: 7777 };
        const secondResult = filterPipe.transform(changedItems, 'important', true);

        expect(firstResult.length).toBe(secondResult.length);
        expect(firstResult === secondResult).toBe(false);
    });

    it('returns the input array if all items match the filter', () => {
        const result = filterPipe.transform(items, ' page', true);
        expect(result).toBe(items);
    });

    it('returns the same array reference if no items match the filter', () => {
        const firstResult = filterPipe.transform(items, 'weird filter with no results', true);
        const secondResult = filterPipe.transform(items, 'another filter with no results', true);
        expect(firstResult).toBe(secondResult);
    });

});
