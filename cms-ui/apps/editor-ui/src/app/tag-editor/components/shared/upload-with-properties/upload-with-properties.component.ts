import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChange
} from '@angular/core';
import { EditableFileProps, FileCreateRequest, FileOrImage, Folder, Raw } from '@gentics/cms-models';
import { UploadResponse } from '@gentics/cms-rest-clients-angular';
import { IFileDropAreaOptions, ModalService } from '@gentics/ui-core';
import { Observable, of, Subscription } from 'rxjs';
import { finalize, switchMap, tap } from 'rxjs/operators';
import { I18nService } from '../../../../core/providers/i18n/i18n.service';
import { UploadConflictService } from '../../../../core/providers/upload-conflict/upload-conflict.service';
import { GtxExternalAssetManagementApiRootObject } from '../../../../shared/components/external-assets-modal/external-assets-modal.component';
import { RepositoryBrowserClient } from '../../../../shared/providers/repository-browser-client/repository-browser-client.service';
import { FolderActionsService } from '../../../../state';

/**
 * Associates an uploaded file with its destination folder.
 */
export interface FileUpload {

    /** The folder that the file was uploaded to. */
    destinationFolder: Folder;

    /** The file that has been uploaded. */
    file: FileOrImage<Raw>;

}

/**
 * Allows the user to upload a file or image and modify its editable properties right after the upload.
 */
@Component({
    selector: 'upload-with-properties',
    templateUrl: './upload-with-properties.component.html',
    styleUrls: ['./upload-with-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UploadWithPropertiesComponent implements OnInit, OnChanges, OnDestroy {

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

    /**
     * Whether the upload button should be shown.
     * In cases where a custom upload button is used, the default one can be disabled
     */
    @Input()
    showUploadButton = true;

    /**
     * This event is fired when a file has been uploaded and its properties have been set successfully.
     */
    @Output()
    uploadPossible = new EventEmitter<boolean>();

    /**
     * This event is fired when a file has been uploaded and its properties have been set successfully.
     */
    @Output()
    upload = new EventEmitter<FileUpload>();

    /** true if an upload is currently in progress. */
    uploadInProgress = false;

    fileDropAreaOptions: IFileDropAreaOptions = {};

    fileToUpload: File;
    selectionToUpload: GtxExternalAssetManagementApiRootObject;

    fileProperties: EditableFileProps;

    private subscriptions = new Subscription();

    constructor(
        private changeDetector: ChangeDetectorRef,
        private folderActions: FolderActionsService,
        private i18nService: I18nService,
        private modalService: ModalService,
        private repositoryBrowserClient: RepositoryBrowserClient,
        private uploadConflictService: UploadConflictService,
    ) { }

    ngOnInit(): void {
        this.uploadPossible.emit(false);
    }

    ngOnChanges(changes: { [K in keyof UploadWithPropertiesComponent]: SimpleChange }): void {
        if (changes.itemType) {
            this.fileDropAreaOptions.accept = changes.itemType.currentValue === 'image' ? 'image/*' : '*';
        }
    }

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }

    browseForFolder(): void {
        this.repositoryBrowserClient.openRepositoryBrowser({
            allowedSelection: 'folder',
            selectMultiple: false,
        }).then(selectedItem => {
            this.destinationFolder = selectedItem;
            this.changeDetector.detectChanges();
        });
    }

    /** Triggered if files have been selected from user's local file system to be uploaded. */
    onFilesSelected(files: File[]): void {
        const file = files[0];
        // clean up
        this.selectionToUpload = undefined;
        if (this.itemType === 'image' || !file.type.toLowerCase().startsWith('image/')) {
            this.fileToUpload = file;
            this.uploadPossible.emit(true);
            this.fileProperties = {
                name: this.fileToUpload.name,
            };
            this.changeDetector.markForCheck();
        } else {
            this.modalService.dialog({
                title: this.i18nService.translate('tag_editor.not_allowed'),
                body: this.i18nService.translate('tag_editor.images_not_allowed'),
                buttons: [
                    { label: this.i18nService.translate('tag_editor.okay_button'), type: 'default', returnValue: true },
                ],
            })
            .then(dialog => dialog.open());
        }
    }

    /** Triggered if previoulsy uploaded files available in CMS have been selected. */
    onAssetsSelected(data: GtxExternalAssetManagementApiRootObject[]): void {
        // response can have more than one selections.
        // Since it is not possible to limit selection to single in a generic way, extract first selection.
        this.selectionToUpload = data[0];
        // clean up
        this.fileToUpload = undefined;

        // check if correct entity type
        if (this.itemType === 'image' && this.selectionToUpload.fileCategory !== 'image') {
            this.modalService.dialog({
                title: this.i18nService.translate('tag_editor.not_allowed'),
                body: this.i18nService.translate('tag_editor.files_not_allowed'),
                buttons: [
                    { label: this.i18nService.translate('tag_editor.okay_button'), type: 'default', returnValue: true },
                ],
            })
            .then(dialog => dialog.open());
        }
        if (this.itemType !== 'image' && this.selectionToUpload.fileCategory === 'image') {
            this.modalService.dialog({
                title: this.i18nService.translate('tag_editor.not_allowed'),
                body: this.i18nService.translate('tag_editor.images_not_allowed'),
                buttons: [
                    { label: this.i18nService.translate('tag_editor.okay_button'), type: 'default', returnValue: true },
                ],
            })
            .then(dialog => dialog.open());
        }

        this.uploadPossible.emit(true);
        this.fileProperties = {
            name: this.selectionToUpload.name,
            description: this.selectionToUpload.description,
            niceUrl: this.selectionToUpload.niceUrl,
            alternateUrls: this.selectionToUpload.alternateUrls,
        };
        this.uploadInProgress = false;
        this.changeDetector.markForCheck();
    }

    onUploadClick(): void {
        if (this.fileToUpload) {
            this._onUploadClick();
        }
        if (this.selectionToUpload) {
            this._onAssetUploadClick();
        }
    }

    private _onUploadClick(): void {
        const upload$ = this.uploadFileOrImage(this.fileToUpload, this.destinationFolder, this.removeUnsetProperties(this.fileProperties));
        if (upload$) {
            const sub = upload$.subscribe(uploadedItem => {
                if (!uploadedItem) {
                    this.uploadInProgress = false;
                    this.changeDetector.markForCheck();
                    throw new Error(`No response data on file upload to folder with ID ${this.destinationFolder.id} returned by REST API.`);
                }
                this.upload.emit({ destinationFolder: this.destinationFolder, file: uploadedItem });
            });
            this.subscriptions.add(sub);
        }
    }

    private _onAssetUploadClick(): void {
        const fileCategory = this.selectionToUpload.fileCategory === 'image' ? 'image' : 'file';
        const payload: FileCreateRequest = {
            overwriteExisting: false,
            folderId: this.destinationFolder.id,
            nodeId: this.destinationFolder.nodeId,
            name: this.fileProperties.name,
            description: this.fileProperties.description,
            sourceURL: this.selectionToUpload['@odata.mediaReadLink'],
            niceURL: this.fileProperties.niceUrl,
            alternateURLs: this.fileProperties.alternateUrls,
            properties: this.selectionToUpload.properties,
        };

        this.uploadInProgress = true
        this.folderActions.uploadFromSourceUrl(fileCategory, payload).pipe(
            tap((data) => this.upload.emit({ destinationFolder: this.destinationFolder, file: data.file })),
            finalize(() => {
                this.uploadInProgress = false;
                // this.changeDetector.markForCheck();
            }),
        ).toPromise();
    }

    filePropertiesChanged(changes: EditableFileProps): void {
        this.fileProperties = changes;
    }

    private removeUnsetProperties(properties: EditableFileProps): EditableFileProps {
        const ret: EditableFileProps = {};
        for (let key of (Object.keys(properties) as (keyof EditableFileProps)[])) {
            const value = properties[key];
            if (value !== null && value !== undefined) {
                ret[key as any] = value;
            }
        }
        return ret;
    }

    triggerUpload(): void {
        this.onUploadClick();
        this.changeDetector.markForCheck();
    }

    /**
     * Uploads the specified file/image and then sets its editable properties afterwards.
     */
    uploadFileOrImage(file: File, destFolder: Folder, properties: EditableFileProps): Observable<FileOrImage<Raw> | void> {
        this.uploadInProgress = true;
        return this.uploadConflictService.uploadFilesWithConflictsCheck([file], destFolder.nodeId, destFolder.id).pipe(
            switchMap((uploadResponses: UploadResponse[][]) => {
                const uploadResponse = [].concat(...uploadResponses)[0]; // Since only one file was uploaded, there can only be one response.
                const uploadedItem = uploadResponse.response.file;
                if (uploadedItem) {
                    const uploadedItemId = uploadedItem.id;
                    let updatePropertiesRequest: Promise<FileOrImage<Raw> | void>;
                    if (this.itemType === 'file') {
                        updatePropertiesRequest = this.folderActions
                            .updateFileProperties(uploadedItemId, properties, { showNotification: true, fetchForUpdate: false });
                    } else {
                        updatePropertiesRequest = this.folderActions
                            .updateImageProperties(uploadedItemId, properties, { showNotification: true, fetchForUpdate: false });
                    }
                    return Observable.fromPromise(updatePropertiesRequest);
                } else {
                    // emit undefined instead of throwing an error to be consistent with the folder actions used above
                    return of(undefined);
                }
            }),
        );
    }

}
