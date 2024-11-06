import { MeshSchemaBO } from '@admin-ui/mesh/common';
import { SchemaTableLoaderService } from '@admin-ui/mesh/providers';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'gtx-mesh-select-schema-modal',
    templateUrl: './select-schema-modal.component.html',
    styleUrls: ['./select-schema-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectSchemaModal extends BaseModal<MeshSchemaBO | MeshSchemaBO[]> {

    @Input()
    public title: string;

    @Input()
    public multiple = true;

    @Input()
    public selected: string[] = [];

    constructor(
        private loader: SchemaTableLoaderService,
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
