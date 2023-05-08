import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './top-bar-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TopBarDemoPage {

    @InjectDocumentation('top-bar.component')
    documentation: IDocumentation;

}
