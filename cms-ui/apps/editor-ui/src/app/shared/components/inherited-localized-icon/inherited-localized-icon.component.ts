import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { InheritableItem } from '@gentics/cms-models';
import { I18nService } from '../../../core/providers/i18n/i18n.service';

@Component({
    selector: 'inherited-localized-icon',
    templateUrl: './inherited-localized-icon.tpl.html',
    styleUrls: ['./inherited-localized-icon.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class InheritedLocalizedIcon {
    @Input() item: InheritableItem;
    @Input() editorNodeId: number;

    constructor(private i18n: I18nService) {}
}
