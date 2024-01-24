import { ChangeDetectionStrategy, Component, OnChanges, SimpleChanges } from '@angular/core';
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
export class AlohaToggleButtonRendererComponent extends BaseAlohaRendererComponent<AlohaToggleButtonComponent, boolean> implements OnChanges {

    public hasText = false;
    public hasIcon = false;

    public ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);
        this.hasText = !!this.settings?.text || !!this.settings?.html;
        this.hasIcon = !!this.settings?.icon;
    }

    public handleClick(): void {
        if (!this.settings) {
            return;
        }
        this.settings.active = !this.settings.active;
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.settings.click?.();
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
