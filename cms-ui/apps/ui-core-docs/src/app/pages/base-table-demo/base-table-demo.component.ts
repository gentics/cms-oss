import { ChangeDetectionStrategy, Component } from '@angular/core';
import { InjectDocumentation } from '../../common/docs-loader';
import { IDocumentation } from '../../common/docs';

@Component({
    templateUrl: './base-table-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BaseTableDemoPage {

    @InjectDocumentation('base-table.component')
    documentation: IDocumentation;
}
