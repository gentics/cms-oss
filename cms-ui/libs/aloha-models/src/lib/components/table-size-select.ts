import { AlohaComponent, AlohaCoreComponentNames } from './base-component';

export interface TableSize {
    columns: number;
    rows: number;
}

export interface AlohaTableSizeSelectComponent extends Omit<AlohaComponent, 'type'> {
    type: AlohaCoreComponentNames.TABLE_SIZE_SELECT;

    maxColumns: number;
    maxRows: number;

    selectedPosition: TableSize;
}
