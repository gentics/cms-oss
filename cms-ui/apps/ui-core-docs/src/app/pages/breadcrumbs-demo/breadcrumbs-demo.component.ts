import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './breadcrumbs-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BreadcrumbsDemoPage {

    @InjectDocumentation('breadcrumb.component')
    documentation: IDocumentation;

    collapsedEnabledColor = '#0096DC';
    collapsedDisabledColor = 'rgb(110, 110, 110)';
    isDisabled = true;

    isChanged = false;

    multiline = true;
    multilineExpanded = false;
}
