import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { AlohaSymbolSearchGridComponent, SymbolGridItem } from '@gentics/aloha-models';
import { generateFormProvider } from '@gentics/ui-core';
import { AlohaSymbolGridRendererComponent } from '../aloha-symbol-grid-renderer/aloha-symbol-grid-renderer.component';

interface NormalizedItem {
    inner: SymbolGridItem;
    items: string[];
}

@Component({
    selector: 'gtx-aloha-symbol-search-grid-renderer',
    templateUrl: './aloha-symbol-search-grid-renderer.component.html',
    styleUrls: ['./aloha-symbol-search-grid-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaSymbolSearchGridRendererComponent)],
})
export class AlohaSymbolSearchGridRendererComponent extends AlohaSymbolGridRendererComponent implements OnInit {

    @Input()
    public settings?: AlohaSymbolSearchGridComponent | Partial<AlohaSymbolSearchGridComponent> | Record<string, any>;

    public filteredSymbols: SymbolGridItem[] = [];

    protected normalizedSymbols: NormalizedItem[] = [];

    public searchText = '';

    public override ngOnInit(): void {
        super.ngOnInit();

        this.updateNormalizedSymbols();
        this.updateFilteredSymbols();
    }

    public updateSearchText(text: string): void {
        this.searchText = text;
        this.updateFilteredSymbols();
    }

    protected override onSymbolsChange(): void {
        this.updateNormalizedSymbols();
        this.updateFilteredSymbols();
    }

    protected updateNormalizedSymbols(): void {
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.normalizedSymbols = (this.settings.symbols || []).map((obj: SymbolGridItem) => {
            return {
                inner: obj,
                items: [(obj.label || ''), ...(obj.keywords || [])].map(item => item.toLocaleLowerCase()),
            };
        });
    }

    protected updateFilteredSymbols(): void {
        const txt = (this.searchText || '').toLocaleLowerCase();

        if (!txt) {
            this.filteredSymbols = this.settings?.symbols;
            return;
        }

        this.filteredSymbols = this.normalizedSymbols
            .filter(obj => obj.items.some(item => item.includes(txt)))
            .map(obj => obj.inner);
    }
}
