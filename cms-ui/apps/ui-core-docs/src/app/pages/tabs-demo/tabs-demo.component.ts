import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './tabs-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class TabsDemoPage {

    @InjectDocumentation('tabs.component')
    documentation: IDocumentation;

    activeTab = 'tab1';
    wrap = false;
}
