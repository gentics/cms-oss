import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { AlohaToggleSplitButtonComponent } from '@gentics/aloha-models';
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

    public ngOnChanges(changes: SimpleChanges): void {
        this.hasText = !!this.settings?.text || !!this.settings?.html;
        this.hasIcon = !!this.settings?.icon;
    }

    protected override setupAlohaHooks(): void {
        super.setupAlohaHooks();

        if (!this.settings) {
            return;
        }

        this.settings.activate = () => {
            this.settings.active = true;
            this.changeDetector.markForCheck();
        };
        this.settings.deactivate = () => {
            this.settings.active = false;
            this.changeDetector.markForCheck();
        };
    }

    public handleClick(): void {
        if (!this.settings) {
            return;
        }
        this.settings.active = !this.settings.active;
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.settings.onToggle?.(this.settings.active);
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
