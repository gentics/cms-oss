import { ChangeDetectionStrategy, Component, OnChanges, SimpleChanges } from '@angular/core';
import { AlohaToggleButtonComponent, ButtonIcon } from '@gentics/aloha-models';
import { generateFormProvider } from '@gentics/ui-core';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';

@Component({
    selector: 'gtx-aloha-toggle-button-renderer',
    templateUrl: './aloha-toggle-button-renderer.component.html',
    styleUrls: ['./aloha-toggle-button-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaToggleButtonRendererComponent)],
})
export class AlohaToggleButtonRendererComponent extends BaseAlohaRendererComponent<AlohaToggleButtonComponent, boolean> implements OnChanges {

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

        this.settings.activate = () => {
            this.triggerChange(true);
            this.changeDetector.markForCheck();
        };
        this.settings.deactivate = () => {
            this.triggerChange(false);
            this.changeDetector.markForCheck();
        };
    }

    public handleClick(): void {
        if (!this.settings) {
            return;
        }
        this.triggerChange(!this.settings.active);
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.settings.click?.();
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.settings.onToggle?.(this.settings.active);
    }

    protected getFinalValue(): boolean {
        return this.settings.active;
    }

    protected override onValueChange(): void {
        if (!this.settings) {
            return;
        }

        this.settings.active = this.value;
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.settings.triggerChangeNotification?.();
    }
}
