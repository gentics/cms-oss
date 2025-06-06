import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { IModalDialog, ISortableEvent } from '@gentics/ui-core';

export interface LabeledField {
    name: string;
    label: string;
}

const COMMON_FIELDS = [
    { name: 'cdate', label: 'common.date_created_label' },
    { name: 'creator', label: 'common.creator_label' },
    { name: 'edate', label: 'common.date_edited_label' },
    { name: 'editor', label: 'common.editor_label' },
    { name: 'id', label: 'common.id_label' },
    { name: 'globalId', label: 'editor.item_global_id_label' }
];

const PAGE_FIELDS = [
    { name: 'customCdate', label: 'common.date_custom_created_label' },
    { name: 'customEdate', label: 'common.date_custom_edited_label' },
    { name: 'priority', label: 'common.priority_label' },
    { name: 'template', label: 'common.template_label' },
    { name: 'usage', label: 'common.usage_label' },
    { name: 'at', label: 'editor.publish_planned_publish_at_date_label' },
    { name: 'offlineAt', label: 'editor.publish_planned_take_offline_at_date_label' },
    { name: 'queuedPublish', label: 'editor.publish_queue_publish_at_date_label' },
    { name: 'queuedOffline', label: 'editor.publish_queue_take_offline_at_date_label' },
];

/**
 * The DisplayFieldSelector is used to select and order the item fields
 * to be displayed in each row of the ContentList.
 */
@Component({
    selector: 'display-field-selector',
    templateUrl: './display-field-selector.component.tpl.html',
    styleUrls: ['./display-field-selector.scss'],
    standalone: false
})
export class DisplayFieldSelectorComponent implements IModalDialog, OnInit {
    fields: string[] = [];

    showPath: boolean;

    closeFn: (val: any) => void;
    cancelFn: (val?: any) => void;

    availableFields: LabeledField[] = [];
    selected: { [fieldName: string]: boolean } = {};

    constructor(private changeDetector: ChangeDetectorRef) { }

    ngOnInit(): void {
        let fieldsByType = COMMON_FIELDS;
        fieldsByType = fieldsByType.concat(PAGE_FIELDS);

        // order the availableFields so that the current (checked) fields come first in
        // sequence, with the unchecked ones following.
        const isValidFieldName = (name: string): boolean => -1 < fieldsByType.map(i => i.name).indexOf(name);
        const nameToFieldObject = (name: string): LabeledField => fieldsByType.filter(f => f.name === name)[0];

        if (!(this.fields instanceof Array)) {
            this.fields = [];
        }

        const orderedFields = this.fields.filter(isValidFieldName).map(nameToFieldObject);
        fieldsByType.forEach(f => {
            if (orderedFields.map(j => j.name).indexOf(f.name) === -1) {
                orderedFields.push(f);
            }
        });
        this.availableFields = orderedFields;

        // set the currently-selected fields.
        this.fields.forEach(field => {
            this.selected[field] = true;
        });
    }

    sortList(e: ISortableEvent): void {
        this.availableFields = e.sort(this.availableFields);
        this.changeDetector.markForCheck();
    }

    /**
     * The user closed the modal, so if "okay" was clicked, we can resolve the modal with the
     * new selection.
     */
    updateAndClose(): void {
        const selection = this.availableFields
            .map(f => f.name)
            .filter(name => !!this.selected[name]);

        this.closeFn({ selection, showPath: this.showPath });
    }

    registerCloseFn(close: (val: any) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val: any) => void): void {
        this.cancelFn = cancel;
    }

}
