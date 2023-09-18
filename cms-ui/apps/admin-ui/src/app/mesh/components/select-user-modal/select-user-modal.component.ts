import { MeshUserBO } from '@admin-ui/mesh/common';
import { MeshUserTableLoaderService } from '@admin-ui/mesh/providers';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'gtx-mesh-select-user-modal',
    templateUrl: './select-user-modal.component.html',
    styleUrls: ['./select-user-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectUserModal extends BaseModal<MeshUserBO | MeshUserBO[]> {

    @Input()
    public title: string;

    @Input()
    public multiple = true;

    @Input()
    public selected: string[] = [];

    constructor(
        private loader: MeshUserTableLoaderService,
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
