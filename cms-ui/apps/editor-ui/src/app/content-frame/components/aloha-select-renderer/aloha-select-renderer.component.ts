import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AlohaSelectComponent } from '@gentics/aloha-models';
import { generateFormProvider } from '@gentics/ui-core';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';
import { patchMultipleAlohaFunctions } from '../../utils';

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

        patchMultipleAlohaFunctions(this.settings, {
            setLabel: (label) => {
                this.settings.label = label;
                this.changeDetector.markForCheck();
            },
            setOptions: (options) => {
                this.settings.options = options;
                this.changeDetector.markForCheck();
            },
            setMultiple: (multiple) => {
                this.settings.multiple = multiple;
                this.changeDetector.markForCheck();
            },
            setClearable: (clearable) => {
                this.settings.clearable = clearable;
                this.changeDetector.markForCheck();
            },
            setPlaceholder: (placeholder) => {
                this.settings.placeholder = placeholder;
                this.changeDetector.markForCheck();
            },
        });
    }

    protected override onValueChange(): void {
        super.onValueChange();

        if (!this.settings) {
            return;
        }

        this.settings.value = this.value;
    }
}
