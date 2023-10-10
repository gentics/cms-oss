import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { BaseControlsComponent } from '../base-controls/base-controls.component';

@Component({
    selector: 'gtx-custom-editor-control',
    templateUrl: './custom-editor-control.component.html',
    styleUrls: ['./custom-editor-control.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CustomEditorControlComponent extends BaseControlsComponent {

    @Input()
    public editorUrl: string;

}
