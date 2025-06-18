import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AlohaToggleSplitButtonComponent } from '@gentics/aloha-models';
import { generateFormProvider } from '@gentics/ui-core';
import { patchMultipleAlohaFunctions } from '../../utils';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';

@Component({
    selector: 'gtx-aloha-toggle-split-button-renderer',
    templateUrl: './aloha-toggle-split-button-renderer.component.html',
    styleUrls: ['./aloha-toggle-split-button-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaToggleSplitButtonRendererComponent)],
})
export class AlohaToggleSplitButtonRendererComponent extends BaseAlohaRendererComponent<AlohaToggleSplitButtonComponent, boolean> {

    protected override setupAlohaHooks(): void {
        super.setupAlohaHooks();

        patchMultipleAlohaFunctions(this.settings, {
            setIcon: icon => {
                this.settings.icon = icon;
                this.changeDetector.markForCheck();
            },
            setIconOnly: only => {
                this.settings.iconOnly = only;
                this.changeDetector.markForCheck();
            },
            setIconHollow: hollow => {
                this.settings.iconHollow = hollow;
                this.changeDetector.markForCheck();
            },
            setText: text => {
                this.settings.text = text;
                this.changeDetector.markForCheck();
            },
            setTooltip: tooltip => {
                this.settings.tooltip = tooltip;
                this.changeDetector.markForCheck();
            },
            setSecondaryVisible: visible => {
                this.settings.secondaryVisible = visible;
                this.changeDetector.markForCheck();
            },
            setActive: active => {
                this.writeValue(active);
                this.changeDetector.markForCheck();
            },
        });
    }

    public handleClick(): void {
        if (!this.settings) {
            this.aloha.restoreSelection();
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
        this.aloha.restoreSelection();
    }

    public handleSecondaryClick(): void {
        if (!this.settings) {
            this.aloha.restoreSelection();
            return;
        }
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.settings.secondaryClick?.();
        this.aloha.restoreSelection();
    }

    protected override getFinalValue(): boolean {
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
