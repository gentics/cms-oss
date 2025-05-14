import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { FormElementDropInformation, FORM_ELEMENT_MIME_TYPE_TYPE } from '@gentics/cms-models';
import { GTX_FORM_EDITOR_ANIMATIONS } from '../../animations/form-editor.animations';

@Component({
    selector: 'gtx-form-element-drop-zone',
    templateUrl: './form-element-drop-zone.component.html',
    styleUrls: ['./form-element-drop-zone.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: GTX_FORM_EDITOR_ANIMATIONS,
    standalone: false
})
export class FormElementDropZoneComponent {

    @Input() hidden = false;

    @Input() cannotReceive: string[] = [];

    @Output()
    elementDropped = new EventEmitter<FormElementDropInformation>();

    dragEnterDepth = 0; // count due to child elements
    isIllegal = false;

    onDragEnter(event: DragEvent): void {
        const dataTransferItem: DataTransferItem = this.getValidDataTransferItem(event, this.cannotReceive);
        this.dragEnterDepth++;
        if (dataTransferItem) {
            event.preventDefault(); // prevents item rejection
            this.setDropEffect(event);
            this.isIllegal = false;
        } else {
            this.isIllegal = true;
        }
    }

    onDragOver(event: DragEvent): void {
        const dataTransferItem: DataTransferItem = this.getValidDataTransferItem(event, this.cannotReceive);
        if (dataTransferItem) {
            event.preventDefault(); // prevents item rejection
            this.setDropEffect(event);
            this.isIllegal = false;
        } else {
            this.isIllegal = true;
        }
    }

    onDragLeave(event: DragEvent): void {
        event.preventDefault(); // has no default action
        this.dragEnterDepth--;
        if (this.dragEnterDepth === 0) {
            this.isIllegal = false;
        }
    }

    onDrop(event: DragEvent): void {
        this.dragEnterDepth--;
        this.isIllegal = false;
        event.preventDefault();
        const dataTransferItem: DataTransferItem = this.getValidDataTransferItem(event, this.cannotReceive);
        if (!this.hidden && dataTransferItem) {
            // TODO: do proper checking
            const rawData = event.dataTransfer.getData(dataTransferItem.type);
            const elementDropInformation: FormElementDropInformation = JSON.parse(rawData);
            this.elementDropped.emit(elementDropInformation);
        }
    }

    private setDropEffect(event: DragEvent): void {
        if (event.dataTransfer.effectAllowed) {
            if (event.dataTransfer.effectAllowed === 'move'
                || event.dataTransfer.effectAllowed === 'copyMove'
                || event.dataTransfer.effectAllowed === 'linkMove'
                || event.dataTransfer.effectAllowed === 'all'
                || event.dataTransfer.effectAllowed === 'uninitialized') {
                event.dataTransfer.dropEffect = 'move';
            } else if (event.dataTransfer.effectAllowed === 'copy'
                || event.dataTransfer.effectAllowed === 'copyLink') {
                event.dataTransfer.dropEffect = 'copy';
            }
        }
    }

    private getValidDataTransferItem(event: DragEvent, cannotReceive: string[]): DataTransferItem {
        // pick the first item that has a gt form element data type
        let item: DataTransferItem = null;
        let i = 0;
        for (i; i < event.dataTransfer.items.length; i++) {
            const currentItem: DataTransferItem = event.dataTransfer.items[i];
            if (currentItem.type.startsWith(`${FORM_ELEMENT_MIME_TYPE_TYPE}/`)) {
                const currentElementType = currentItem.type.substring(FORM_ELEMENT_MIME_TYPE_TYPE.length + 1);
                if (cannotReceive.includes(currentElementType)) {
                    continue;
                } else {
                    item = currentItem;
                    break;
                }
            }
        }

        return item;
    }
}
