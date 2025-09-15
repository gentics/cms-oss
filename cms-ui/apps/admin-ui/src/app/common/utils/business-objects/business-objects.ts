import { BO_ID, BO_NEW_SORT_ORDER, BO_ORIGINAL_SORT_ORDER, BO_PERMISSIONS, BusinessObject, SortableBusinessObject } from '@admin-ui/common/models';
import { I18nService } from '@admin-ui/core';
import {
    MOVE_DOWN_ACTION,
    MOVE_TO_BOTTOM_ACTION,
    MOVE_TO_TOP_ACTION,
    MOVE_UP_ACTION,
} from '@admin-ui/shared/components/base-sortable-entity-table/base-sortable-entity-table.component';
import { PermissionListResponse } from '@gentics/cms-models';
import { TableAction, TableRow } from '@gentics/ui-core';
import { sortBy } from 'lodash-es';

export function createMoveActions<T>(i18n: I18nService, enabled: boolean): TableAction<T>[] {
    return [
        {
            id: MOVE_TO_TOP_ACTION,
            icon: 'vertical_align_top',
            label: i18n.instant('shared.move'),
            enabled,
            type: 'secondary',
            single: true,
        },
        {
            id: MOVE_UP_ACTION,
            icon: 'keyboard_arrow_up',
            label: i18n.instant('shared.move'),
            enabled,
            type: 'secondary',
            single: true,
        },
        {
            id: MOVE_DOWN_ACTION,
            icon: 'keyboard_arrow_down',
            label: i18n.instant('shared.move'),
            enabled,
            type: 'secondary',
            single: true,
        },
        {
            id: MOVE_TO_BOTTOM_ACTION,
            icon: 'vertical_align_bottom',
            label: i18n.instant('shared.move'),
            enabled,
            type: 'secondary',
            single: true,
        },
    ];
}

export function resetEntitySorting<T extends SortableBusinessObject>(rows: TableRow<T>[]): TableRow<T>[] {
    return sortBy(rows.map(row => {
        row.item[BO_NEW_SORT_ORDER] = row.item[BO_ORIGINAL_SORT_ORDER];
        row.hash = row.item[BO_ORIGINAL_SORT_ORDER];
        return row;
    }), [(row => row.item[BO_NEW_SORT_ORDER])]);
}

export function sortEntityRow<T extends SortableBusinessObject>(rows: TableRow<T>[], from: number, to: number): TableRow<T>[] {
    const copy = rows.slice();
    const removed = copy.splice(from, 1);
    copy.splice(to, 0, ...removed);

    return copy.map((row, idx) => {
        row.item[BO_NEW_SORT_ORDER] = idx;
        row.hash = `${idx}`;
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
