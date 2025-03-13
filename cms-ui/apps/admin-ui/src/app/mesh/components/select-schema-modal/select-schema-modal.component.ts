import { MeshSchemaBO } from '@admin-ui/mesh/common';
import { ChangeDetectionStrategy, Component, forwardRef, Input, ViewChild } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';
import { SchemaTableComponent } from '../schema-table/schema-table.component';
@Component({
    selector: 'gtx-mesh-select-schema-modal',
    templateUrl: './select-schema-modal.component.html',
    styleUrls: ['./select-schema-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectSchemaModal extends BaseModal<MeshSchemaBO | MeshSchemaBO[]> {

    @Input()
    public title: string;

    @Input()
    public multiple = true;

    @Input()
    public selected: string[] = [];

    @ViewChild(forwardRef(() => SchemaTableComponent))
    public table: SchemaTableComponent;

    confirmSelection(): void {
        const entities = this.table.getSelectedEntities();
        this.closeFn(this.multiple ? entities : entities[0]);
    }
}
