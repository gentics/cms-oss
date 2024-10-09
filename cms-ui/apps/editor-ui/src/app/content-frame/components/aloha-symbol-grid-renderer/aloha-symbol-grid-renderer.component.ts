import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { AlohaSymbolGridComponent, SymbolGridItem } from '@gentics/aloha-models';
import { generateFormProvider } from '@gentics/ui-core';
import { patchMultipleAlohaFunctions } from '../../utils';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';

@Component({
    selector: 'gtx-aloha-symbol-grid-renderer',
    templateUrl: './aloha-symbol-grid-renderer.component.html',
    styleUrls: ['./aloha-symbol-grid-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaSymbolGridRendererComponent)],
})
export class AlohaSymbolGridRendererComponent extends BaseAlohaRendererComponent<AlohaSymbolGridComponent, SymbolGridItem> implements OnInit {

    public control: FormControl<SymbolGridItem>;

    public override ngOnInit(): void {
        super.ngOnInit();

        this.control = new FormControl(this.value);
        this.subscriptions.push(this.control.valueChanges.subscribe(value => {
            if (this.value !== value) {
                this.triggerChange(value);
            }
        }));
    }

    protected override setupAlohaHooks(): void {
        super.setupAlohaHooks();

        patchMultipleAlohaFunctions(this.settings, {
            setSymbols: symbols => {
                this.settings.symbols = symbols;
                this.onSymbolsChange();
                this.changeDetector.markForCheck();
            },
        });
    }

    protected onSymbolsChange(): void {}

    protected override onValueChange(): void {
        super.onValueChange();

        if (this.control && this.control.value !== this.value) {
            this.control.setValue(this.value);
        }
    }
}
