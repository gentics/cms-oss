import { BO_ID, BO_NODE_ID, ContentItemBO, ContentItemTypes, PickableEntity } from '@admin-ui/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
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
    public multiple = false;

    @Input()
    public selected: PickableEntity | PickableEntity[];

    public selectionMap: { [id: string]: ContentItemBO } = {};
    public selectedIds: string[];

    constructor(
        protected changeDetector: ChangeDetectorRef,
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

    confirmSelection(): void {
        let selected = this.selectedIds
            .map(id => this.selectionMap[id])
            .filter(item => item)
            .map(item => ({
                type: item.type,
                nodeId: item[BO_NODE_ID],
                entity: item,
            }));

        if (!this.multiple) {
            this.closeFn(selected?.[0]);
        } else {
            this.closeFn(selected);
        }
    }
}
