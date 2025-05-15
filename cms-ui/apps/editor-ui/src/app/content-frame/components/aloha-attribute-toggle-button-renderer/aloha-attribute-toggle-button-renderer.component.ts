import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { generateFormProvider } from '@gentics/ui-core';
import { AlohaAttributeToggleButtonComponent } from '@gentics/aloha-models';
import { AlohaAttributeButtonRendererComponent } from '../aloha-attribute-button-renderer/aloha-attribute-button-renderer.component';
import { patchMultipleAlohaFunctions } from '../../utils';

@Component({
    selector: 'gtx-aloha-attribute-toggle-button-renderer',
    templateUrl: './aloha-attribute-toggle-button-renderer.component.html',
    styleUrls: ['./aloha-attribute-toggle-button-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaAttributeToggleButtonRendererComponent)],
    standalone: false
})
export class AlohaAttributeToggleButtonRendererComponent extends AlohaAttributeButtonRendererComponent {

    @Input()
    public settings?: AlohaAttributeToggleButtonComponent | Partial<AlohaAttributeToggleButtonComponent> | Record<string, any>;

    protected override setupAlohaHooks(): void {
        super.setupAlohaHooks();

        patchMultipleAlohaFunctions(this.settings, {
            setActive: (active) => {
                this.settings.active = active;
                this.changeDetector.markForCheck();
            },
        });
    }

    public override handleClick(): void {
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

}
