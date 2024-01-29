import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { AlohaSymbolGridComponent } from '@gentics/aloha-models';
import { generateFormProvider } from '@gentics/ui-core';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';

@Component({
    selector: 'gtx-aloha-symbol-grid-renderer',
    templateUrl: './aloha-symbol-grid-renderer.component.html',
    styleUrls: ['./aloha-symbol-grid-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaSymbolGridRendererComponent)],
})
export class AlohaSymbolGridRendererComponent extends BaseAlohaRendererComponent<AlohaSymbolGridComponent, string> implements OnInit {

    public control: FormControl<string>;

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

        if (!this.settings) {
            return;
        }

        this.settings.updateSymbols = (symbols) => {
            this.settings.symbols = symbols;
            this.changeDetector.markForCheck();
        };
    }

    protected override onValueChange(): void {
        super.onValueChange();

        if (this.control && this.control.value !== this.value) {
            this.control.setValue(this.value);
        }
    }
}
