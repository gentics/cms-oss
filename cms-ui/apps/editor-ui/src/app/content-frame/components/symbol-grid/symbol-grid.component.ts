import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { BaseFormElementComponent, generateFormProvider } from '@gentics/ui-core';

@Component({
    selector: 'gtx-symbol-grid',
    templateUrl: './symbol-grid.component.html',
    styleUrls: ['./symbol-grid.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(SymbolGridComponent)],
})
export class SymbolGridComponent extends BaseFormElementComponent<string> {

    @Input()
    public symbols: string[];

    public selectSymbol(symbol: string): void {
        this.triggerChange(symbol);
    }

    protected onValueChange(): void {
    }
}
