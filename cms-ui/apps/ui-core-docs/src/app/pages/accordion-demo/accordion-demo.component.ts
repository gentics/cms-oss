import { ChangeDetectionStrategy, Component } from '@angular/core';
import { InjectDocumentation } from '../../common/docs-loader';
import { IDocumentation } from '../../common/docs';

@Component({
    templateUrl: './accordion-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class AccordionDemoPage {

    @InjectDocumentation('accordion.component')
    documentation: IDocumentation;
}
