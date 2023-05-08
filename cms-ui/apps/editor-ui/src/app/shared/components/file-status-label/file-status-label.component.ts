import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { FileOrImage } from '@gentics/cms-models';
import { I18nService } from '../../../core/providers/i18n/i18n.service';

@Component({
    selector: 'file-status-label',
    templateUrl: './file-status-label.tpl.html',
    styleUrls: ['./file-status-label.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class FileStatusLabel {
    // tslint:disable-next-line:no-input-rename
    @Input('item') file: FileOrImage;

    constructor(private i18n: I18nService) {}
}
