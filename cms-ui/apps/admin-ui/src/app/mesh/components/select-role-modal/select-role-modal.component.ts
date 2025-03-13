import { MeshRoleBO } from '@admin-ui/mesh/common';
import { ChangeDetectionStrategy, Component, forwardRef, Input, ViewChild } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';
import { MeshRoleTableComponent } from '../mesh-role-table/mesh-role-table.component';

@Component({
    selector: 'gtx-mesh-select-role-modal',
    templateUrl: './select-role-modal.component.html',
    styleUrls: ['./select-role-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectRoleModal extends BaseModal<MeshRoleBO | MeshRoleBO[]> {

    @Input()
    public title: string;

    @Input()
    public multiple = true;

    @Input()
    public selected: string[] = [];

    @ViewChild(forwardRef(() => MeshRoleTableComponent))
    public table: MeshRoleTableComponent;

    confirmSelection(): void {
        const entities = this.table.getSelectedEntities();
        this.closeFn(this.multiple ? entities : entities[0]);
    }
}
