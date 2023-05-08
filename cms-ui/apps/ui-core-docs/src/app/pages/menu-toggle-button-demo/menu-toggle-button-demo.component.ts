import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './menu-toggle-button-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MenuToggleButtonDemoPage {

    @InjectDocumentation('menu-toggle-button.component')
    documentation: IDocumentation;

    isActive: boolean;
}
