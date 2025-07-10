import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './side-menu-demo.component.html',
    styleUrls: ['./side-menu-demo.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class SideMenuDemoPage {

    @InjectDocumentation('side-menu.component')
    documentation: IDocumentation;

    displayMenu1 = false;
    displayMenu2 = false;
    menuPosition = 'left';
    menuWidth = '400px';
}
