import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './concat-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConcatDemoPage {

    @InjectDocumentation('concat.pipe')
    documentation: IDocumentation;
}
