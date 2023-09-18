import { MeshGroupBO } from '@admin-ui/mesh/common';
import { MeshGroupTableLoaderService } from '@admin-ui/mesh/providers';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';

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

    constructor(
        private loader: MeshGroupTableLoaderService,
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
