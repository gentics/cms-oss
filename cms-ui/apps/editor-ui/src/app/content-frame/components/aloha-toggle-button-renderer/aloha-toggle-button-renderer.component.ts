import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AlohaToggleButtonComponent } from '@gentics/aloha-models';
import { generateFormProvider } from '@gentics/ui-core';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';

@Component({
    selector: 'gtx-aloha-toggle-button-renderer',
    templateUrl: './aloha-toggle-button-renderer.component.html',
    styleUrls: ['./aloha-toggle-button-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaToggleButtonRendererComponent)],
})
export class AlohaToggleButtonRendererComponent extends BaseAlohaRendererComponent<AlohaToggleButtonComponent, boolean> {

    protected override setupAlohaHooks(): void {
        super.setupAlohaHooks();

        if (!this.settings) {
            return;
        }

        this.settings.setIcon = (icon: string) => {
            this.settings.icon = icon;
            this.changeDetector.markForCheck();
        };
        this.settings.setText = (text: string) => {
            this.settings.text = text;
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
