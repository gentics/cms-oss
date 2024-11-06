import { MeshProjectBO } from '@admin-ui/mesh/common';
import { ProjectTableLoaderService } from '@admin-ui/mesh/providers/project-table-loader/project-table-loader.service';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';

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

    constructor(
        private loader: ProjectTableLoaderService,
    ) {
        super();
    }

    confirmSelection(): void {
        if (this.multiple) {
            this.closeFn(this.loader.getEntitiesByIds(this.selected));
        } else {
            this.closeFn(this.loader.getEntityById(this.selected[0]));
        }
    }
}
