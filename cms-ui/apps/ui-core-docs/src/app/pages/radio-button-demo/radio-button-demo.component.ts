import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './radio-button-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RadioButtonDemoPage {

    @InjectDocumentation('radio-button.component')
    documentation: IDocumentation;

    radioValue: any;
    selectedColor: any;
    radioChecked: boolean;
}
