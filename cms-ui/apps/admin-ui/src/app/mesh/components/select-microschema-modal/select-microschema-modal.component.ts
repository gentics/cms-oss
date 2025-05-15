import { MeshMicroschemaBO } from '@admin-ui/mesh/common';
import { ChangeDetectionStrategy, Component, forwardRef, Input, ViewChild } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';
import { MicroschemaTableComponent } from '../microschema-table/microschema-table.component';

@Component({
    selector: 'gtx-mesh-select-microschema-modal',
    templateUrl: './select-microschema-modal.component.html',
    styleUrls: ['./select-microschema-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class SelectMicroschemaModal extends BaseModal<MeshMicroschemaBO | MeshMicroschemaBO[]> {

    @Input()
    public title: string;

    @Input()
    public multiple = true;

    @Input()
    public selected: string[] = [];

    @ViewChild(forwardRef(() => MicroschemaTableComponent))
    public table: MicroschemaTableComponent;

    confirmSelection(): void {
        const entities = this.table.getSelectedEntities();
        this.closeFn(this.multiple ? entities : entities[0]);
    }
}
