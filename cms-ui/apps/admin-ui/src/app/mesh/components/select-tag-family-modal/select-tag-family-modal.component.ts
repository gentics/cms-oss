import { MeshTagFamilyBO } from '@admin-ui/mesh/common';
import { ChangeDetectionStrategy, Component, forwardRef, Input, ViewChild } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';
import { TagFamilyTableComponent } from '../tag-family-table/tag-family-table.component';

@Component({
    selector: 'gtx-mesh-select-tag-family-modal',
    templateUrl: './select-tag-family-modal.component.html',
    styleUrls: ['./select-tag-family-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectTagFamilyModal extends BaseModal<MeshTagFamilyBO | MeshTagFamilyBO[]> {

    @Input()
    public title: string;

    @Input()
    public multiple = true;

    @Input()
    public selected: string[] = [];

    @ViewChild(forwardRef(() => TagFamilyTableComponent))
    public table: TagFamilyTableComponent;

    confirmSelection(): void {
        const entities = this.table.getSelectedEntities();
        this.closeFn(this.multiple ? entities : entities[0]);
    }
}
