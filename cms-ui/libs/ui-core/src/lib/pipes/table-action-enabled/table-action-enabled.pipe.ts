import { Pipe, PipeTransform } from '@angular/core';
import { TableAction } from '../../common';

@Pipe({
    name: 'gtxTableActionEnabled',
    standalone: false
})
export class TableActionEnabledPipe implements PipeTransform {

    transform<T>(action: TableAction<T>, item?: T): boolean {
        if (!action) {
            return false;
        }

        if (typeof action.enabled === 'boolean') {
            return action.enabled;
        }

        return action.enabled(item);
    }
}
