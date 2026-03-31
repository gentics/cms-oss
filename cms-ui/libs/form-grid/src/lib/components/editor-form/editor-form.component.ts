import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    computed,
    input,
    model,
} from '@angular/core';
import { FormElement, FormTypeConfiguration } from '@gentics/cms-models';
import { BaseComponent } from '@gentics/ui-core';

@Component({
    selector: 'gtx-editor-form',
    templateUrl: './editor-form.component.html',
    styleUrls: ['./editor-form.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditorFormComponent extends BaseComponent {

    public config = input<FormTypeConfiguration>();
    public restricted = input<boolean>();
    public language = input<string>();

    public activeElement = model<string>();

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
    }

}
