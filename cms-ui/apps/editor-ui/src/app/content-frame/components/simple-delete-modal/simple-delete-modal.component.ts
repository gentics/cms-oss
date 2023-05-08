import { ChangeDetectionStrategy, Component } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'simple-delete-modal-modal',
    templateUrl: './simple-delete-modal.component.html',
    styleUrls: ['./simple-delete-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SimpleDeleteModalComponent extends BaseModal<boolean> {

    // Should be passed in by the function which creates the modal
    items: any[];

    itemType: string;

    idProperty: string;

    iconString: string;

    get deleteCount(): number {
        return this.items.length;
    }

    confirm(): void {
        this.closeFn(true);
    }
}
