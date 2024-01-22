import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { AlohaButtonComponent } from '@gentics/aloha-models';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';

@Component({
    selector: 'gtx-aloha-button-renderer',
    templateUrl: './aloha-button-renderer.component.html',
    styleUrls: ['./aloha-button-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AlohaButtonRendererComponent extends BaseAlohaRendererComponent<AlohaButtonComponent, void> implements OnChanges {

    @Input()
    public settings?: AlohaButtonComponent | Partial<AlohaButtonComponent> | Record<string, any>;

    public hasText = false;
    public hasIcon = false;

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
    }

    public ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);
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
}
