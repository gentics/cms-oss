import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AlohaButtonComponent } from '@gentics/aloha-models';
import { generateFormProvider } from '@gentics/ui-core';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';

@Component({
    selector: 'gtx-aloha-button-renderer',
    templateUrl: './aloha-button-renderer.component.html',
    styleUrls: ['./aloha-button-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaButtonRendererComponent)],
})
export class AlohaButtonRendererComponent extends BaseAlohaRendererComponent<AlohaButtonComponent, void> {

    protected override setupAlohaHooks(): void {
        super.setupAlohaHooks();

        if (!this.settings) {
            return;
        }

        this.settings.setIcon = icon => {
            this.settings.icon = icon;
            this.changeDetector.markForCheck();
        };
        this.settings.setIconOnly = only => {
            this.settings.iconOnly = only;
            this.changeDetector.markForCheck();
        }
        this.settings.setIconHollow = hollow => {
            this.settings.iconHollow = hollow;
            this.changeDetector.markForCheck();
        };
        this.settings.setText = text => {
            this.settings.text = text;
            this.changeDetector.markForCheck();
        };
        this.settings.setTooltip = tooltip => {
            this.settings.tooltip = tooltip;
            this.changeDetector.markForCheck();
        };
    }

    public handleClick(): void {
        if (!this.settings) {
            return;
        }
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.settings.click?.();
        this.aloha.restoreSelection();
    }
}
