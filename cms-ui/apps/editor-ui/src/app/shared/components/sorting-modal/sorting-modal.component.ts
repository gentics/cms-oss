import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { ItemType, SortField } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';

/**
 * A dialog used to select the sorting field and direction for a given type.
 */
@Component({
    selector: 'sorting-modal',
    templateUrl: './sorting-modal.tpl.html',
    styleUrls: ['./sorting-modal.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class SortingModal implements IModalDialog, OnInit {

    // Injected when modal is created
    itemType: ItemType | 'wastebin' | 'contenttag' | 'templatetag';
    sortBy: SortField;
    sortOrder: 'asc' | 'desc';

    availableFields: SortField[] = [];

    constructor(private entityResolver: EntityResolver) {}

    ngOnInit(): void {
        const commonFields: SortField[] = ['name' , 'cdate' , 'edate'];
        const pageFields: SortField[] = ['customordefaultcdate', 'customordefaultedate', 'pdate' , 'filename' , 'template' , 'priority' ];
        const fileFields: SortField[] = ['type', 'filesize'];
        const wastebinFields: SortField[] = ['deletedat'];

        switch (this.itemType) {
            case 'page':
                this.availableFields = commonFields.concat(pageFields);
                break;
            case 'file':
            case 'image':
                this.availableFields = commonFields.concat(fileFields);
                break;
            case 'wastebin':
                this.availableFields = commonFields.concat(wastebinFields);
                break;
            case 'contenttag':
            case 'templatetag':
                this.availableFields = ['name'];
                break;
            default:
                this.availableFields = commonFields;
                break;
        }
    }

    toggleSort(field: SortField): void {
        if (field === this.sortBy) {
            this.sortOrder = this.sortOrder === 'asc' ? 'desc' : 'asc';
        } else {
            this.sortBy = field;
            this.sortOrder = 'asc';
        }
    }

    setSort(): void {
        this.closeFn({
            sortBy: this.sortBy,
            sortOrder: this.sortOrder
        });
    }

    closeFn(val: any): void { }
    cancelFn(val?: any): void { }

    registerCloseFn(close: (val: any) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val: any) => void): void {
        this.cancelFn = cancel;
    }
}
