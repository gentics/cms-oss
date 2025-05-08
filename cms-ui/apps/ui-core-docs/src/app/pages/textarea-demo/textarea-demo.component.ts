import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './textarea-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class TextareaDemoPage {

    @InjectDocumentation('textarea.component')
    documentation: IDocumentation;

    readonly: boolean;
    required: boolean;
    disabled: boolean;
    message: string;
    validatedMessage: string;

    pattern = '^[0-9]*\n*[0-9]*$';
    validationErrorTooltip = 'Please use the correct format';
}
