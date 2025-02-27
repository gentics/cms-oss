import { ChangeDetectionStrategy, Component } from '@angular/core';
import { InjectDocumentation } from '../../common/docs-loader';
import { IDocumentation } from '../../common/docs';

@Component({
    templateUrl: './base-form-element-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BaseFormElementDemoPage {

    @InjectDocumentation('base-form-element.component')
    documentation: IDocumentation;
}
