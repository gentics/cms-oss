import { MeshTagBO } from '@admin-ui/mesh/common';
import { ChangeDetectionStrategy, Component, forwardRef, Input, ViewChild } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';
import { TagTableComponent } from '../tag-table/tag-table.component';

@Component({
    selector: 'gtx-mesh-select-tag-modal',
    templateUrl: './select-tag-modal.component.html',
    styleUrls: ['./select-tag-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class SelectTagModal extends BaseModal<MeshTagBO | MeshTagBO[]> {

    @Input()
    public title: string;

    @Input()
    public multiple = true;

    @Input()
    public selected: string[] = [];

    @ViewChild(forwardRef(() => TagTableComponent))
    public table: TagTableComponent;

    confirmSelection(): void {
        const entities = this.table.getSelectedEntities();
        this.closeFn(this.multiple ? entities : entities[0]);
    }
}
