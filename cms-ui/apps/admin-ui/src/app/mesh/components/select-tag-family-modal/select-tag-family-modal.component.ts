import { MeshTagFamilyBO } from '@admin-ui/mesh/common';
import { TagFamilyTableLoaderService } from '@admin-ui/mesh/providers';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';

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

    constructor(
        private loader: TagFamilyTableLoaderService,
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
