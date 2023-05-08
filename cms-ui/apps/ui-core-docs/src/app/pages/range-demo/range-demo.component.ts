import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './range-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RangeDemoPage {

    @InjectDocumentation('range.component')
    documentation: IDocumentation;

    rangeValDynamic = 35;
    rangeVal: any = 0;
    showThumb = true;
}
