import { AlohaCoreComponentNames } from './base-component';
import { AlohaSymbolGridComponent } from './symbol-grid';

export interface AlohaSymbolSearchGridComponent extends Omit<AlohaSymbolGridComponent, 'type'> {
    type: AlohaCoreComponentNames.SYMBOL_SEARCH_GRID;

    searchLabel?: string;
}
