import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './overlay-host-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OverlayHostDemoPage {

    @InjectDocumentation('overlay-host.component')
    documentation: IDocumentation;
}
