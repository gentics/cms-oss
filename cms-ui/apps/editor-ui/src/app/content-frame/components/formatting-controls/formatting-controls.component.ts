import { ChangeDetectionStrategy, Component } from '@angular/core';
import { BaseControlsComponent } from '../base-controls/base-controls.component';

@Component({
    selector: 'gtx-formatting-controls',
    templateUrl: './formatting-controls.component.html',
    styleUrls: ['./formatting-controls.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormattingControlsComponent extends BaseControlsComponent {

    public activeFormats: string[] = [];

    public linkCheckerEnabled = false;
    public linkValid = false;

    public toggleFormat(format: string): void {
        const idx = this.activeFormats.indexOf(format);

        // Push/Splice can't be used, as the change detection isn't triggered, because it's still
        // the same array. Even with `markForCheck`, it simply doesn't re-run the includes pipe.
        if (idx === -1) {
            this.activeFormats = [...this.activeFormats, format];
        } else {
            this.activeFormats = [
                ...this.activeFormats.slice(0, idx),
                ...this.activeFormats.slice(idx + 1),
            ];
        }
    }
}
