import { MeshProjectBO } from '@admin-ui/mesh/common';
import { ChangeDetectionStrategy, Component, Input, ViewChild } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';
import { ProjectTableComponent } from '../project-table/project-table.component';

@Component({
    selector: 'gtx-mesh-select-project-modal',
    templateUrl: './select-project-modal.component.html',
    styleUrls: ['./select-project-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectProjectModal extends BaseModal<MeshProjectBO | MeshProjectBO[]> {

    @Input()
    public title: string;

    @Input()
    public multiple = true;

    @Input()
    public selected: string[] = [];

    @ViewChild(ProjectTableComponent)
    public table: ProjectTableComponent;

    confirmSelection(): void {
        const entities = this.table.getSelectedEntities();
        this.closeFn(this.multiple ? entities : entities[0]);
    }
}
