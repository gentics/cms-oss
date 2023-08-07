import { MeshRoleBO } from '@admin-ui/mesh/common';
import { MeshRoleTableLoaderService } from '@admin-ui/mesh/providers';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';

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

    constructor(
        private loader: MeshRoleTableLoaderService,
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
