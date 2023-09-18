import { MeshMicroschemaBO } from '@admin-ui/mesh/common';
import { MicroschemaTableLoaderService } from '@admin-ui/mesh/providers';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'gtx-mesh-select-microschema-modal',
    templateUrl: './select-microschema-modal.component.html',
    styleUrls: ['./select-microschema-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectMicroschemaModal extends BaseModal<MeshMicroschemaBO | MeshMicroschemaBO[]> {

    @Input()
    public title: string;

    @Input()
    public multiple = true;

    @Input()
    public selected: string[] = [];

    constructor(
        private loader: MicroschemaTableLoaderService,
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
