import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AlohaSplitButtonComponent } from '@gentics/aloha-models';
import { generateFormProvider } from '@gentics/ui-core';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';
import { patchMultipleAlohaFunctions } from '../../utils';

@Component({
    selector: 'gtx-aloha-split-button-renderer',
    templateUrl: './aloha-split-button-renderer.component.html',
    styleUrls: ['./aloha-split-button-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaSplitButtonRendererComponent)],
})
export class AlohaSplitButtonRendererComponent extends BaseAlohaRendererComponent<AlohaSplitButtonComponent, void> {

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
        });
    }

    public handleClick(): void {
        if (!this.settings) {
            this.aloha.restoreSelection();
            return;
        }
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.settings.click?.();
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

}
