import { AlohaComponent, AlohaCoreComponentNames } from './base-component';

export interface SymbolGridItem {
    symbol: string;
    label: string;
    keywords?: string[];
}

export interface AlohaSymbolGridComponent extends AlohaComponent {
    type: AlohaCoreComponentNames.SYMBOL_GRID;

    symbols: SymbolGridItem[];

    setSymbols: (symbols: string[]) => void;
}
