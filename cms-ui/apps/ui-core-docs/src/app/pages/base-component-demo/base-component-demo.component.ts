import { ChangeDetectionStrategy, Component } from '@angular/core';
import { InjectDocumentation } from '../../common/docs-loader';
import { IDocumentation } from '../../common/docs';

@Component({
    templateUrl: './base-component-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BaseComponentDemoPage {

    @InjectDocumentation('base.component')
    documentation: IDocumentation;
}
