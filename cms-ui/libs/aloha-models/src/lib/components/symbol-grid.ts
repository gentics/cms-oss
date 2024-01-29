import { AlohaComponent, AlohaCoreComponentNames } from './base-component';

export interface AlohaSymbolGridComponent extends AlohaComponent {
    type: AlohaCoreComponentNames.SYMBOL_GRID;

    symbols: string[];

    updateSymbols: (symbols: string[]) => void;
}
