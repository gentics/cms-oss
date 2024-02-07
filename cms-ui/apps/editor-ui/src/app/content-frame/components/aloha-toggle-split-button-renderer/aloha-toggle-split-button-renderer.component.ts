import { ChangeDetectionStrategy, Component, OnChanges, SimpleChanges } from '@angular/core';
import { AlohaToggleSplitButtonComponent, ButtonIcon } from '@gentics/aloha-models';
import { generateFormProvider } from '@gentics/ui-core';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';

@Component({
    selector: 'gtx-aloha-toggle-split-button-renderer',
    templateUrl: './aloha-toggle-split-button-renderer.component.html',
    styleUrls: ['./aloha-toggle-split-button-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaToggleSplitButtonRendererComponent)],
})
export class AlohaToggleSplitButtonRendererComponent extends BaseAlohaRendererComponent<AlohaToggleSplitButtonComponent, boolean> implements OnChanges {

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

        this.settings.setActive = (active) => {
            this.settings.active = active;
            this.changeDetector.markForCheck();
        }
    }

    public handleClick(): void {
        if (!this.settings) {
            return;
        }
        this.triggerTouch();
        const switched = !this.settings.active;
        if (!this.settings.pure) {
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            this.settings.toggleActivation();
        }
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.settings.click?.();
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.settings.onToggle?.(switched);
    }

    public handleSecondaryClick(): void {
        if (!this.settings) {
            return;
        }
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.settings.secondaryClick?.();
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
