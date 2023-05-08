import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ColorThemes, TooltipAlignment, TooltipPosition } from '@gentics/ui-core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './tooltip-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TooltipDemoPage {

    @InjectDocumentation('tooltip.component')
    documentation: IDocumentation;

    type: ColorThemes = 'secondary';
    position: TooltipPosition = 'top';
    align: TooltipAlignment = 'center';
    delay = 1_000;
}
