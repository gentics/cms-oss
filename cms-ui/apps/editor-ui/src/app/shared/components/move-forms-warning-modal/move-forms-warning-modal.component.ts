import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { Folder, Form, Node as NodeModel } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';

/**
 * A modal which warns the user, that moving forms to another node/channel
 * may break pages which link to these forms. The user has to confirm the move.
 */
@Component({
    selector: 'gtx-move-forms-warning-modal',
    templateUrl: './move-forms-warning-modal.component.html',
    styleUrls: ['./move-forms-warning-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class MoveFormsWarningModal extends BaseModal<Form[]> {

    /** The forms which are about to be moved. */
    @Input()
    public forms: Form[] = [];

    /** The folder the forms are being moved to. */
    @Input()
    public targetFolder: Folder;

    /** The node/channel the forms are being moved to. */
    @Input()
    public targetNode: NodeModel;

    confirm(): void {
        this.closeFn(this.forms);
    }
}
