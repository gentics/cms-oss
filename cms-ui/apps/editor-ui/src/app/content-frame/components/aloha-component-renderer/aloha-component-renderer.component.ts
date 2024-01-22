import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { AlohaComponent, AlohaCoreComponentNames } from '@gentics/aloha-models';

@Component({
    selector: 'gtx-aloha-component-renderer',
    templateUrl: './aloha-component-renderer.component.html',
    styleUrls: ['./aloha-component-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AlohaComponentRendererComponent {

    public readonly AlohaCoreComponentNames = AlohaCoreComponentNames;

    @Input()
    public slot?: string;

    @Input()
    public component?: AlohaComponent;

    @Input()
    public type?: string;

    @Input()
    public settings?: Record<string, any>;

    constructor(
        protected changeDetector: ChangeDetectorRef,
    ) {}
}
