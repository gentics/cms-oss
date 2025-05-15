import { Pipe, PipeTransform } from '@angular/core';
import { TableColumn, TableColumnMappingFn } from '../../common';

@Pipe({
    name: 'gtxTableCellMapper',
    standalone: false
})
export class TableCellMapperPipe implements PipeTransform {

    // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
    transform<T>(value: any, mappingFn: TableColumnMappingFn<T>, column: TableColumn<T>): any {
        if (typeof mappingFn !== 'function') {
            return value;
        }

        return mappingFn(value, column);
    }
}
