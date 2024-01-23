import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { AlohaSplitButtonComponent } from '@gentics/aloha-models';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';

@Component({
    selector: 'gtx-aloha-split-button-renderer',
    templateUrl: './aloha-split-button-renderer.component.html',
    styleUrls: ['./aloha-split-button-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AlohaSplitButtonRendererComponent extends BaseAlohaRendererComponent<AlohaSplitButtonComponent, void> implements OnChanges {

    @Input()
    public settings?: AlohaSplitButtonComponent | Partial<AlohaSplitButtonComponent> | Record<string, any>;

    public hasText = false;
    public hasIcon = false;

    public ngOnChanges(changes: SimpleChanges): void {
        this.hasText = !!this.settings?.text || !!this.settings?.html;
        this.hasIcon = !!this.settings?.icon;
    }

    public handleClick(): void {
        if (!this.settings) {
            return;
        }
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.settings.click?.();
    }

    public handleSecondaryClick(): void {
        if (!this.settings) {
            return;
        }
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.settings.secondaryClick?.();
    }

}
