import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AlohaCheckboxComponent } from '@gentics/aloha-models';
import { generateFormProvider } from '@gentics/ui-core';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';

@Component({
    selector: 'gtx-aloha-checkbox-renderer',
    templateUrl: './aloha-checkbox-renderer.component.html',
    styleUrls: ['./aloha-checkbox-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaCheckboxRendererComponent)],
    standalone: false
})
export class AlohaCheckboxRendererComponent extends BaseAlohaRendererComponent<AlohaCheckboxComponent, boolean> {

    public handleCheckboxChange(state: boolean): void {
        this.triggerChange(state);
    }

    protected override onValueChange(): void {
        super.onValueChange();

        this.settings.checked = this.value;
    }
}
