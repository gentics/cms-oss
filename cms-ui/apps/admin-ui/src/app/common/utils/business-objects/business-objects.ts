import { BO_ID, BO_NEW_SORT_ORDER, BO_ORIGINAL_SORT_ORDER, BO_PERMISSIONS, BusinessObject, SortableBusinessObject } from '@admin-ui/common/models';

import { PermissionListResponse } from '@gentics/cms-models';
import { TableAction, TableRow } from '@gentics/ui-core';
import { sortBy } from 'lodash-es';

export function resetEntitySorting<T extends SortableBusinessObject>(rows: TableRow<T>[]): TableRow<T>[] {
    return sortBy(rows.map(row => {
        row.item[BO_NEW_SORT_ORDER] = row.item[BO_ORIGINAL_SORT_ORDER];
        row.hash = row.item[BO_ORIGINAL_SORT_ORDER];
        return row;
    }), [(row => row.item[BO_NEW_SORT_ORDER])]);
}

export function sortEntityRow<T extends SortableBusinessObject>(rows: TableRow<T>[], from: number, to: number): TableRow<T>[] {
    const copy = [...rows];
    const removed = copy.splice(from, 1);
    copy.splice(to, 0, ...removed);

    return copy.map((row, idx) => {
        row.item[BO_NEW_SORT_ORDER] = idx;
        row.hash = idx;
        return row;
    });
}

export function applyPermissions(bos: BusinessObject[], response: PermissionListResponse<any>): void {
    if (!response?.perms) {
        return;
    }

    for (const bo of bos) {
        const perms = response.perms[bo[BO_ID]];
        if (!perms) {
            continue;
        }
        bo[BO_PERMISSIONS] = perms;
    }
}
