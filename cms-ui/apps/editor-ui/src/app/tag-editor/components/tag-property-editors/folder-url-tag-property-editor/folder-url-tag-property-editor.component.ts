import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, ViewChild } from '@angular/core';
import { I18nService } from '@gentics/cms-components';
import { TagEditorContext, TagEditorError, TagPropertiesChangedFn, TagPropertyEditor } from '@gentics/cms-integration-api-models';
import {
    EditableTag,
    FileOrImage,
    FileUpload,
    Folder,
    FolderTagPartProperty,
    ItemInNode,
    Raw,
    TagPart,
    TagPartProperty,
    TagPropertyMap,
    TagPropertyType,
} from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { of } from 'rxjs';
import { catchError, map, switchMap, take, takeUntil, tap } from 'rxjs/operators';
import { ObservableStopper } from '../../../../common/utils/observable-stopper/observable-stopper';
import { SelectedItemHelper } from '../../../../shared/util/selected-item-helper/selected-item-helper';
import { FolderActionsService } from '../../../../state';
import { ExpansionButtonComponent } from '../../shared/expansion-button/expansion-button.component';

/**
 * Used to edit the UrlFolder and FolderUpload TagParts.
 */
@Component({
    selector: 'folder-url-tag-property-editor',
    templateUrl: './folder-url-tag-property-editor.component.html',
    styleUrls: ['./folder-url-tag-property-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FolderUrlTagPropertyEditor implements TagPropertyEditor, OnDestroy {

    /** The TagPart that the hosted TagPropertyEditor is responsible for. */
    tagPart: TagPart;

    /** The TagProperty that we are editing. */
    tagProperty: FolderTagPartProperty;

    @ViewChild('expandUploadButton')
    expandUploadButton: ExpansionButtonComponent;

    @ViewChild('expandCreateSubfolderButton')
    expandCreateSubfolderButton: ExpansionButtonComponent;

    uploadDestination: Folder<Raw>;

    /** The name of the subfolder to create (if any). */
    subfolderName = '';

    /** Whether we are currently creating a subfolder. */
    creatingSubfolder = false;

    /**
     * The files that have been uploaded.
     */
    uploadedFiles: FileOrImage[] = [];

    /** The helper for managing and loading the selected folder. */
    selectedFolder: SelectedItemHelper<ItemInNode<Folder<Raw>>>;

    /** Whether the tag is opened in read-only mode. */
    readOnly: boolean;

    /** The initial base/destination folder id, taken from the context. */
    private baseFolderId?: number;
    /** The initial base/destination language, taken from the context. */
    public baseLanguage?: string;

    /** The onChange function registered by the TagEditor. */
    private onChangeFn: TagPropertiesChangedFn;

    private stopper = new ObservableStopper();

    constructor(
        private client: GCMSRestClientService,
        private changeDetector: ChangeDetectorRef,
        private folderActions: FolderActionsService,
        private i18n: I18nService,
    ) { }

    ngOnDestroy(): void {
        this.stopper.stop();
    }

    initTagPropertyEditor(tagPart: TagPart, tag: EditableTag, tagProperty: TagPartProperty, context: TagEditorContext): void {
        this.selectedFolder = new SelectedItemHelper('folder', context.node.id, this.client);
        this.tagPart = tagPart;
        this.readOnly = context.readOnly;
        this.baseFolderId = context.page?.folderId
          ?? context.folder?.id
          ?? context.image?.folderId
          ?? context.file?.folderId
          ?? context.node.folderId;

        this.baseLanguage = context.page?.language;
        this.updateTagProperty(tagProperty);

        this.selectedFolder.selectedItem$.pipe(
            switchMap((selectedFolder) => {
                if (selectedFolder) {
                    return this.client.folder.get(selectedFolder.id)
                        .pipe(
                            map((response) => response.folder),
                            catchError((err) => of(err)),
                            tap((folder: Folder<Raw>) => {
                                this.uploadDestination = folder;
                                this.changeDetector.markForCheck();
                            }),
                        );
                }

                if (this.baseFolderId) {
                    return this.client.folder.get(this.baseFolderId)
                        .pipe(
                            map((response) => response.folder),
                            catchError((err) => of(err)),
                            tap((folder: Folder<Raw>) => {
                                this.uploadDestination = folder;
                                this.changeDetector.markForCheck();
                            }),
                        );
                }

                return of();
            }),
            takeUntil(this.stopper.stopper$),
        ).subscribe();
    }

    registerOnChange(fn: TagPropertiesChangedFn): void {
        this.onChangeFn = fn;
    }

    writeChangedValues(values: Partial<TagPropertyMap>): void {
        // We only care about changes to the TagProperty that this control is responsible for.
        const tagProp = values[this.tagPart.keyword];
        if (tagProp) {
            this.updateTagProperty(tagProp);
        }
    }

    /**
     * Changes the values of this.tagProperty and this.selectedItem according
     * to newSelectedItem. This method must only be called in response to
     * user input.
     */
    changeSelectedItem(newSelectedItem: Folder<Raw>): void {
        if (newSelectedItem) {
            this.tagProperty.folderId = newSelectedItem.id;
            this.tagProperty.nodeId = newSelectedItem.nodeId;
        } else {
            this.tagProperty.folderId = 0;
            this.tagProperty.nodeId = 0;
        }

        if (this.onChangeFn) {
            const changes: Partial<TagPropertyMap> = {};
            changes[this.tagPart.keyword] = this.tagProperty;
            this.onChangeFn(changes);
        }
        this.selectedFolder.setSelectedItem(newSelectedItem);
    }

    onUpload(uploadedItem: FileUpload): void {
        this.uploadedFiles.push(uploadedItem.file);
        this.expandUploadButton.collapse();
        this.changeDetector.markForCheck();
    }

    onCreateSubfolderClick(): void {
        this.creatingSubfolder = true;
        this.selectedFolder.selectedItem$.pipe(take(1)).subscribe((parentFolder) => {
            this.folderActions.createNewFolder({
                name: this.subfolderName,
                description: '',
                publishDir: parentFolder.publishDir,
                motherId: parentFolder.id,
                nodeId: parentFolder.nodeId,
                failOnDuplicate: true,
            })
                .then((subfolder) => {
                    if (subfolder) {
                        this.expandCreateSubfolderButton.collapse();
                        this.subfolderName = '';
                        this.changeSelectedItem(subfolder);
                    }
                    this.creatingSubfolder = false;
                    this.changeDetector.markForCheck();
                })
                .catch(() => {
                    this.creatingSubfolder = false;
                    this.changeDetector.markForCheck();
                });
        });
    }

    /**
     * Used to update the currently edited TagProperty with external changes.
     */
    private updateTagProperty(newValue: TagPartProperty): void {
        if (newValue.type !== TagPropertyType.FOLDER) {
            throw new TagEditorError(`TagPropertyType ${newValue.type} not supported by FolderUrlTagPropertyEditor.`);
        }
        this.tagProperty = newValue;

        this.selectedFolder.setSelectedItem(this.tagProperty.folderId, this.tagProperty.nodeId);
        this.changeDetector.markForCheck();
    }

}
