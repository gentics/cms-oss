import {
    MOVE_DOWN_ACTION,
    MOVE_TO_BOTTOM_ACTION,
    MOVE_TO_TOP_ACTION,
    MOVE_UP_ACTION,
} from '@admin-ui/common';
import { TableAction } from '@gentics/ui-core';
import { I18nService } from '../providers/i18n/i18n.service';

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
