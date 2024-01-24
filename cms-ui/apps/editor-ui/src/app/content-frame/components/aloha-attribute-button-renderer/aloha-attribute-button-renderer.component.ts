import { ChangeDetectionStrategy, Component, OnChanges, SimpleChanges } from '@angular/core';
import { generateFormProvider } from '@gentics/ui-core';
import { AlohaAttributeButtonComponent, ButtonIcon } from '@gentics/aloha-models';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';

@Component({
    selector: 'gtx-aloha-attribute-button-renderer',
    templateUrl: './aloha-attribute-button-renderer.component.html',
    styleUrls: ['./aloha-attribute-button-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaAttributeButtonRendererComponent)],
})
export class AlohaAttributeButtonRendererComponent
    extends BaseAlohaRendererComponent<AlohaAttributeButtonComponent, string>
    implements OnChanges {

    public hasText = false;
    public hasIcon = false;
    public iconToRender: string;

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        this.hasText = !!this.settings?.text || !!this.settings?.html;

        this.iconToRender = typeof this.settings?.icon === 'string'
            ? this.settings?.icon
            : this.settings?.icon?.primary;
        this.hasIcon = !!this.iconToRender;
    }

    protected override setupAlohaHooks(): void {
        super.setupAlohaHooks();

        if (!this.settings) {
            return;
        }

        this.settings.setIcon = (icon: ButtonIcon) => {
            this.settings.icon = icon;
            this.iconToRender = typeof this.settings?.icon === 'string'
                ? this.settings?.icon
                : this.settings?.icon?.primary;
            this.hasIcon = !!this.iconToRender;
            this.changeDetector.markForCheck();
        };
        this.settings.setText = (text: string) => {
            this.settings.text = text;
            this.hasText = !!this.settings.text || !!this.settings.html;
            this.changeDetector.markForCheck();
        };
        this.settings.setTooltip = (tooltip: string) => {
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
    }

    public handleSecondaryClick(): void {
        if (!this.settings) {
            return;
        }
        // TODO: Open dropdown with input
    }
}
