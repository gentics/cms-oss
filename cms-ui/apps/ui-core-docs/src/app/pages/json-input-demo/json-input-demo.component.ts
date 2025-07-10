import { ChangeDetectionStrategy, Component } from '@angular/core';
import { FormControl } from '@angular/forms';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './json-input-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class JsonInputDemoPage {

    @InjectDocumentation('json-input.component')
    documentation: IDocumentation;

    control = new FormControl<string>('');

    raw = false;
}
