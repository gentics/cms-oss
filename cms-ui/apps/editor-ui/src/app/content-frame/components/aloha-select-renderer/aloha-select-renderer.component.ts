import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AlohaSelectComponent } from '@gentics/aloha-models';
import { generateFormProvider } from '@gentics/ui-core';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';

@Component({
    selector: 'gtx-alohal-select-renderer',
    templateUrl: './aloha-select-renderer.component.html',
    styleUrls: ['./aloha-select-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaSelectRendererComponent)],
})
export class AlohaSelectRendererComponent extends BaseAlohaRendererComponent<AlohaSelectComponent, string | string[]> {

    protected override setupAlohaHooks(): void {
        super.setupAlohaHooks();

        if (!this.settings) {
            return;
        }

        this.settings.setLabel = (label) => {
            this.settings.label = label;
            this.changeDetector.markForCheck();
        };
        this.settings.setOptions = (options) => {
            this.settings.options = options;
            this.changeDetector.markForCheck();
        };
        this.settings.setMultiple = (multiple) => {
            this.settings.multiple = multiple;
            this.changeDetector.markForCheck();
        };
        this.settings.setClearable = (clearable) => {
            this.settings.clearable = clearable;
            this.changeDetector.markForCheck();
        };
        this.settings.setPlaceholder = (placeholder) => {
            this.settings.placeholder = placeholder;
            this.changeDetector.markForCheck();
        };
    }

    protected override onValueChange(): void {
        super.onValueChange();

        if (!this.settings) {
            return;
        }

        this.settings.value = this.value;
    }
}
