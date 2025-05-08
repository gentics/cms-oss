import { MeshUserBO } from '@admin-ui/mesh/common';
import { ChangeDetectionStrategy, Component, forwardRef, Input, ViewChild } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';
import { MeshUserTableComponent } from '../mesh-user-table/mesh-user-table.component';

@Component({
    selector: 'gtx-mesh-select-user-modal',
    templateUrl: './select-user-modal.component.html',
    styleUrls: ['./select-user-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class SelectUserModal extends BaseModal<MeshUserBO | MeshUserBO[]> {

    @Input()
    public title: string;

    @Input()
    public multiple = true;

    @Input()
    public selected: string[] = [];

    @ViewChild(forwardRef(() => MeshUserTableComponent))
    public table: MeshUserTableComponent;

    confirmSelection(): void {
        const entities = this.table.getSelectedEntities();
        this.closeFn(this.multiple ? entities : entities[0]);
    }
}
