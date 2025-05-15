import { TemplateTagBO } from '@admin-ui/common';
import { BaseTableMasterComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AnyModelType, NormalizableEntityTypesMap, TemplateTag } from '@gentics/cms-models';
import { ModalService, TableRow } from '@gentics/ui-core';
import { TemplateTagTableLoaderService } from '../../providers';
import { CreateTemplateTagModalComponent } from '../create-template-tag-modal/create-template-tag-modal.component';
import { EditTemplateTagModalComponent } from '../edit-template-tag-modal/edit-template-tag-modal.component';

@Component({
    selector: 'gtx-template-tag-master',
    templateUrl: './template-tag-master.component.html',
    styleUrls: ['./template-tag-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class TemplateTagMasterComponent extends BaseTableMasterComponent<TemplateTag, TemplateTagBO> {

    @Input()
    public disabled = false;

    @Input()
    public nodeId: number;

    @Input()
    public templateId: number | string;

    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'templateTag';

    constructor(
        changeDetector: ChangeDetectorRef,
        router: Router,
        route: ActivatedRoute,
        appState: AppStateService,
        protected modalService: ModalService,
        protected tableLoader: TemplateTagTableLoaderService,
    ) {
        super(
            changeDetector,
            router,
            route,
            appState,
        );
    }

    public override async handleRowClick(row: TableRow<TemplateTagBO>): Promise<void> {
        // Disable editing when it has no permission/when it's disabled/readonly
        if (this.disabled) {
            return;
        }

        const dialog = await this.modalService.fromComponent(EditTemplateTagModalComponent, {
            closeOnEscape: false,
            closeOnOverlayClick: false,
        }, {
            nodeId: this.nodeId,
            templateId: this.templateId,
            tag: row.item,
        });

        const updated = await dialog.open();

        if (!updated) {
            return;
        }

        this.tableLoader.reload();
    }

    public async handleCreate(): Promise<void> {
        // Disable editing when it has no permission/when it's disabled/readonly
        if (this.disabled) {
            return;
        }

        const dialog = await this.modalService.fromComponent(CreateTemplateTagModalComponent, {
            closeOnEscape: false,
            closeOnOverlayClick: false,
        }, {
            nodeId: this.nodeId,
            templateId: this.templateId,
        });

        const created = await dialog.open();

        if (!created) {
            return;
        }

        this.tableLoader.reload();
    }
}
