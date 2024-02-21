import { ChangeDetectionStrategy, Component } from '@angular/core';
import { FormControl } from '@angular/forms';
import { AlohaSelectMenuComponent, SelectMenuOption, SelectMenuSelectEvent } from '@gentics/aloha-models';
import { generateFormProvider } from '@gentics/ui-core';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';

@Component({
    selector: 'gtx-aloha-select-menu-renderer',
    templateUrl: './aloha-select-menu-renderer.component.html',
    styleUrls: ['./aloha-select-menu-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaSelectMenuRendererComponent)],
})
export class AlohaSelectMenuRendererComponent extends BaseAlohaRendererComponent<AlohaSelectMenuComponent, SelectMenuSelectEvent<any>> {

    public activeMultiStep: SelectMenuOption | null = null;
    public componentRequiresConfirm = false;
    public control: FormControl<any>;

    protected override setupAlohaHooks(): void {
        super.setupAlohaHooks();

        if (!this.settings) {
            return;
        }

        this.settings.setIconsOnly = (iconsOnly) => {
            this.settings.iconsOnly = iconsOnly;
            this.changeDetector.markForCheck();
        };
        this.settings.setOptions = (options) => {
            this.settings.options = options;
            this.changeDetector.markForCheck();
        };
    }

    public handleOptionClick(option: SelectMenuOption): void {
        if (!option.isMultiStep) {
            this.activeMultiStep = null;
            this.control = null;
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            this.settings.onSelect?.({ id: option.id });
            this.triggerChange({ id: option.id });
            return;
        }

        this.componentRequiresConfirm = false;
        this.control = new FormControl(option.multiStepContext.initialValue);
        this.activeMultiStep = option;
    }

    public stepBack(): void {
        this.activeMultiStep = null;
    }

    public multiStepConfirm(): void {
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.settings?.onSelect({ id: this.activeMultiStep.id, value: this.control.value });
        this.triggerChange({ id: this.activeMultiStep.id, value: this.control.value });
    }

    public handleComponentRequire(confirm: boolean): void {
        this.componentRequiresConfirm = confirm;
    }

    public handleComponentManualConfirm(): void {
        this.multiStepConfirm();
    }
}
