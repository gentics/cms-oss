import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { SymbolGridItem } from '@gentics/aloha-models';
import { BaseFormElementComponent, generateFormProvider } from '@gentics/ui-core';

@Component({
    selector: 'gtx-symbol-grid',
    templateUrl: './symbol-grid.component.html',
    styleUrls: ['./symbol-grid.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(SymbolGridComponent)],
})
export class SymbolGridComponent extends BaseFormElementComponent<SymbolGridItem> {

    @Input()
    public symbols: SymbolGridItem[];

    public selectSymbol(symbol: SymbolGridItem): void {
        this.triggerChange(symbol);
    }

    protected onValueChange(): void {
    }
}
