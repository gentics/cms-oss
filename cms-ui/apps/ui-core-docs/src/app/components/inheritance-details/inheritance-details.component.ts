import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { InheritanceInfo } from '../../common/docs';

@Component({
    selector: 'gtx-inheritance-details',
    templateUrl: './inheritance-details.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InheritanceDetailsComponent {

    @Input()
    public prefix: string | null = null;

    @Input()
    public info: InheritanceInfo;
}
