import { TableRow } from '@gentics/ui-core';
import { BO_DISPLAY_NAME, BO_ID, BO_NEW_SORT_ORDER, BO_ORIGINAL_SORT_ORDER, BO_PERMISSIONS, SortableBusinessObject } from '../../models';
import { sortEntityRow } from './business-objects';

fdescribe('Business Objects', () => {
    let counter = 0;
    const makeTestRow: () => TableRow<SortableBusinessObject> = () => {
        const id = counter++;
        return {
            id: `${id}`,
            item: {
                [BO_ID]: `${id}`,
                [BO_DISPLAY_NAME]: `${id}`,
                [BO_PERMISSIONS]: [],
                [BO_ORIGINAL_SORT_ORDER]: id,
                [BO_NEW_SORT_ORDER]: id,
            },
        };
    };

    const items: TableRow<SortableBusinessObject>[] = [
        makeTestRow(),
        makeTestRow(),
        makeTestRow(),
        makeTestRow(),
        makeTestRow(),
    ];

    it('should move from front to back correctly', () => {
        expect(sortEntityRow(items, 2, 3).map(row => row.id)).toEqual([
            items[0].id,
            items[1].id,
            items[3].id,
            items[2].id,
            items[4].id,
        ]);
    });

    fit('should move from back to front correctly', () => {
        expect(sortEntityRow(items, 3, 1).map(row => row.id)).toEqual([
            items[0].id,
            items[3].id,
            items[1].id,
            items[2].id,
            items[4].id,
        ]);
    });
});