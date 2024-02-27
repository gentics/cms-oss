import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { AlohaContextToggleButtonComponent } from '@gentics/aloha-models';
import { generateFormProvider } from '@gentics/ui-core';
import { AlohaContextButtonRendererComponent } from '../aloha-context-button-renderer/aloha-context-button-renderer.component';

@Component({
    selector: 'gtx-aloha-context-toggle-button-renderer',
    templateUrl: './aloha-context-toggle-button-renderer.component.html',
    styleUrls: ['./aloha-context-toggle-button-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaContextToggleButtonRendererComponent)],
})
export class AlohaContextToggleButtonRendererComponent<T> extends AlohaContextButtonRendererComponent<T> {

    @Input()
    public settings: AlohaContextToggleButtonComponent<T> | Partial<AlohaContextToggleButtonComponent<T>> | Record<string, any>;

    protected override setupAlohaHooks(): void {
        super.setupAlohaHooks();

        if (!this.settings) {
            return;
        }

        this.settings.setActive = (active) => {
            this.settings.active = active;
            this.changeDetector.markForCheck();
        }
    }

    public override handleClick(): void {
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

        this.handleContext();
    }
}
