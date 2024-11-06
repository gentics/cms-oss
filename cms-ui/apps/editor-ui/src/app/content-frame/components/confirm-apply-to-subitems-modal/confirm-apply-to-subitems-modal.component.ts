import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { ItemWithObjectTags } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';

/**
 * A modal for the user to confirm that he wants to apply an object property to all subitems of a certain type.
 */
@Component({
    selector: 'confirm-apply-to-subitems-modal',
    templateUrl: './confirm-apply-to-subitems-modal.component.html',
    styleUrls: ['./confirm-apply-to-subitems-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmApplyToSubitemsModalComponent extends BaseModal<boolean> {

    @Input()
    public item: ItemWithObjectTags;

    @Input()
    public objPropId: string;
}
