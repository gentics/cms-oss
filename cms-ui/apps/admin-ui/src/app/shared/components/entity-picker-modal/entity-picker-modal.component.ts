import { BO_ID, BO_NODE_ID, ContentItemBO, ContentItemTypes, PickableEntity } from '@admin-ui/common';
import { ErrorHandler } from '@admin-ui/core';
import { ContentItemTrableLoaderService } from '@admin-ui/shared/providers';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { BaseModal, TrableRow } from '@gentics/ui-core';

@Component({
    selector: 'gtx-entity-picker-modal',
    templateUrl: './entity-picker-modal.component.html',
    styleUrls: ['./entity-picker-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EntityPickerModalComponent
    extends BaseModal<false | PickableEntity | PickableEntity[]>
    implements OnInit {

    @Input()
    public types: ContentItemTypes[] = [];

    @Input()
    public nodesAsFolder = false;

    @Input()
    public multiple = false;

    @Input()
    public selected: PickableEntity | PickableEntity[];

    public loading = false;

    public selectionMap: { [id: string]: ContentItemBO } = {};
    public selectedIds: string[];

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected client: GCMSRestClientService,
        protected loader: ContentItemTrableLoaderService,
        protected errorHandler: ErrorHandler,
    ) {
        super();
    }

    ngOnInit(): void {
        const tmp = this.selected == null ? [] : (Array.isArray(this.selected) ? this.selected : [this.selected]);
        tmp.forEach(element => {
            this.selectionMap[element.entity[BO_ID]] = element.entity;
        });
        this.selectedIds = Object.keys(this.selectionMap);
    }

    handleRowSelect(row: TrableRow<ContentItemBO>): void {
        this.selectionMap[row.id] = row.item;
    }

    selectionChanges(event: string[]): void {
        this.selectedIds = event;
    }

    async confirmSelection(): Promise<void> {
        const selected: PickableEntity[] = [];
        this.loading = true;
        this.changeDetector.markForCheck();

        for (const id of this.selectedIds) {
            const item = this.selectionMap[id];

            if (this.nodesAsFolder && (item.type === 'channel' || item.type === 'node')) {
                try {
                    const folder = await this.client.folder.get(item.folderId || item.id).toPromise();
                    const folderBO = this.loader.mapToBusinessObject(folder.folder, item);

                    selected.push({
                        type: 'folder',
                        nodeId: item.id,
                        entity: folderBO,
                    });
                    continue;
                } catch (err) {
                    this.errorHandler.catch(err);
                    this.loading = false;
                    this.changeDetector.markForCheck();
                    return;
                }
            }

            selected.push({
                type: item.type,
                nodeId: item[BO_NODE_ID],
                entity: item,
            });
        }

        this.loading = false;
        this.changeDetector.markForCheck();

        if (!this.multiple) {
            this.closeFn(selected?.[0]);
        } else {
            this.closeFn(selected);
        }
    }
}
