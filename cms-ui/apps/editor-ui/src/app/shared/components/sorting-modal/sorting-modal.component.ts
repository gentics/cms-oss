import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { ItemType, SortField, SortOrder } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';

interface SortOption {
    sortBy: SortField,
    sortOrder: SortOrder,
}

/**
 * A dialog used to select the sorting field and direction for a given type.
 */
@Component({
    selector: 'sorting-modal',
    templateUrl: './sorting-modal.component.html',
    styleUrls: ['./sorting-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SortingModal extends BaseModal<SortOption> implements OnInit {

    @Input()
    itemType: ItemType | 'wastebin' | 'contenttag' | 'templatetag';

    @Input()
    sortBy: SortField;

    @Input()
    sortOrder: SortOrder;

    availableFields: SortField[] = [];

    constructor() {
        super();
    }

    ngOnInit(): void {
        const commonFields: SortField[] = ['name' , 'cdate' , 'edate'];
        const pageFields: SortField[] = ['customordefaultcdate', 'customordefaultedate', 'pdate' , 'filename' , 'template' , 'priority' ];
        const fileFields: SortField[] = ['customordefaultcdate', 'customordefaultedate', 'type', 'filesize'];
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
            sortOrder: this.sortOrder,
        });
    }
}
