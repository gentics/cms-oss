import { MeshGroupBO } from '@admin-ui/mesh/common';
import { ChangeDetectionStrategy, Component, Input, ViewChild } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';
import { MeshGroupTableComponent } from '../mesh-group-table/mesh-group-table.component';

@Component({
    selector: 'gtx-mesh-select-group-modal',
    templateUrl: './select-group-modal.component.html',
    styleUrls: ['./select-group-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectGroupModal extends BaseModal<MeshGroupBO | MeshGroupBO[]> {

    @Input()
    public title: string;

    @Input()
    public multiple = true;

    @Input()
    public selected: string[] = [];

    @ViewChild(MeshGroupTableComponent)
    public table: MeshGroupTableComponent;

    confirmSelection(): void {
        const entities = this.table.getSelectedEntities();
        this.closeFn(this.multiple ? entities : entities[0]);
    }
}
