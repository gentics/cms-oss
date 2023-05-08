import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './split-view-container-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SplitViewContainerDemoPage {

    @InjectDocumentation('split-view-container.component')
    documentation: IDocumentation;
}
