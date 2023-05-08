import { Component, Input, ViewChild } from '@angular/core';
import { Folder } from '@gentics/cms-models';
import { BaseModal, IModalDialog } from '@gentics/ui-core';
import { FileUpload, UploadWithPropertiesComponent } from '../upload-with-properties/upload-with-properties.component';

/**
 * A modal for the user to upload a new image or file.
 */
@Component({
    selector: 'upload-with-properties-modal',
    templateUrl: './upload-with-properties-modal.component.html',
    styleUrls: ['./upload-with-properties-modal.component.scss']
})
export class UploadWithPropertiesModalComponent extends BaseModal<FileUpload> implements IModalDialog {

    /**
     * Determines if the user is allowed to select a destination folder himself.
     */
    @Input()
    allowFolderSelection = true;

    /**
     * The destination folder that is currently selected.
     */
    @Input()
    destinationFolder: Folder;

    /**
     * The item type that should be uploaded.
     */
    @Input()
    itemType: 'file' | 'image';

    @ViewChild(UploadWithPropertiesComponent, { static: true })
    uploadWithPropertiesComponent: UploadWithPropertiesComponent;

    uploadPossible: boolean = false;

    onUploadPossible(uploadPossible: boolean): void {
        this.uploadPossible = uploadPossible;
    }

    onUploadClick(): void {
        if (this.uploadWithPropertiesComponent) {
            this.uploadWithPropertiesComponent.triggerUpload();
        }
    }

    onUploadCompleted(upload: FileUpload): void {
        this.closeFn(upload);
    }
}
