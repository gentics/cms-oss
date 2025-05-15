import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CmsFormElementBO } from '@gentics/cms-models';

@Component({
    selector: 'gtx-form-element-preview',
    templateUrl: './form-element-preview.component.html',
    styleUrls: ['./form-element-preview.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class FormElementPreviewComponent {

    @Input()
    element: CmsFormElementBO;

    @Input()
    isPreview: boolean;

}
