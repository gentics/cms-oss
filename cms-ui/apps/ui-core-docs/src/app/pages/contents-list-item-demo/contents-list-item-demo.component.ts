import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './contents-list-item-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ContentsListItemDemoPage {

    @InjectDocumentation('contents-list-item.component')
    documentation: IDocumentation;

    listItems: string[] = [
        'foo',
        'bar',
        'baz',
    ];
}
