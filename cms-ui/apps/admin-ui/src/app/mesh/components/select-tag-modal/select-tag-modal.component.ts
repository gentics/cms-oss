import { MeshTagBO } from '@admin-ui/mesh/common';
import { TagTableLoaderService } from '@admin-ui/mesh/providers';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'gtx-mesh-select-tag-modal',
    templateUrl: './select-tag-modal.component.html',
    styleUrls: ['./select-tag-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectTagModal extends BaseModal<MeshTagBO | MeshTagBO[]> {

    @Input()
    public title: string;

    @Input()
    public multiple = true;

    @Input()
    public selected: string[] = [];

    constructor(
        private loader: TagTableLoaderService,
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
