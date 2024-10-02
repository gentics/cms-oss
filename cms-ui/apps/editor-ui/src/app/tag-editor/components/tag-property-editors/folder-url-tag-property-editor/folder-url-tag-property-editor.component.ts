import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, ViewChild } from '@angular/core';
import { ObservableStopper } from '@editor-ui/app/common/utils/observable-stopper/observable-stopper';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { RepositoryBrowserClient } from '@editor-ui/app/shared/providers';
import { SelectedItemHelper } from '@editor-ui/app/shared/util/selected-item-helper/selected-item-helper';
import { FolderActionsService } from '@editor-ui/app/state';
import { TagEditorContext, TagEditorError, TagPropertiesChangedFn, TagPropertyEditor } from '@gentics/cms-integration-api-models';
import {
    EditableTag,
    FileOrImage,
    FileUpload,
    Folder,
    FolderTagPartProperty,
    ItemInNode,
    Page,
    Raw,
    TagPart,
    TagPartProperty,
    TagPropertyMap,
    TagPropertyType,
} from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { Observable, merge, of } from 'rxjs';
import { catchError, map, switchMap, take, takeUntil, tap } from 'rxjs/operators';
import { ExpansionButtonComponent } from '../../shared/expansion-button/expansion-button.component';

/**
 * Used to edit the UrlFolder and FolderUpload TagParts.
 */
@Component({
    selector: 'folder-url-tag-property-editor',
    templateUrl: './folder-url-tag-property-editor.component.html',
    styleUrls: ['./folder-url-tag-property-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FolderUrlTagPropertyEditor implements TagPropertyEditor, OnDestroy {

    /** The TagPart that the hosted TagPropertyEditor is responsible for. */
    tagPart: TagPart;

    /** The TagProperty that we are editing. */
    tagProperty: FolderTagPartProperty;

    /** The string that should be displayed in the input field. */
    displayValue$: Observable<string>;

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

    /** Page this edited tag belongs to */
    private page?: Page<Raw>;

    /** The onChange function registered by the TagEditor. */
    private onChangeFn: TagPropertiesChangedFn;

    private stopper = new ObservableStopper();

    constructor(
        private client: GCMSRestClientService,
        private changeDetector: ChangeDetectorRef,
        private folderActions: FolderActionsService,
        private repositoryBrowserClient: RepositoryBrowserClient,
        private i18n: I18nService,
    ) { }

    ngOnDestroy(): void {
        this.stopper.stop();
    }

    initTagPropertyEditor(tagPart: TagPart, tag: EditableTag, tagProperty: TagPartProperty, context: TagEditorContext): void {
        this.selectedFolder = new SelectedItemHelper('folder', context.node.id, this.client);

        this.displayValue$ = merge(
            this.selectedFolder.selectedItem$.pipe(
                map((selectedItem: Folder<Raw>) => {
                    if (selectedItem) {
                        return selectedItem.name;
                    } else {
                        /**
                         * null is emitted, when nothing is selected.
                         * Also, null is emitted in case a referenced folder got deleted and the tag property data was refetched.
                         * (Since the folderId in tagProperty gets removed)
                         */
                        return this.i18n.translate('editor.folder_no_selection');
                    }
                }),
            ),
            this.selectedFolder.loadingError$.pipe(
                map((error: { error: any, item: { itemId: number, nodeId?: number } }) => {
                    /**
                     * When a folder that is referenced gets deleted, but the tag property data is not refetched, this tag property editor gets a folderId
                     * in tagProperty.
                     * When we try to fetch the folder information we get an error message.
                     * In that case we want to inform the user that the folder got deleted (and thus avoid suggesting that a valid folder is still selected).
                     */
                    if (this.tagProperty && this.tagProperty.folderId) {
                        /** additional check, in case the loadingError$ Subject is changed to a BehaviorSubject in the future.
                         * This could trigger an emission before this.tagProperty is set in updateTagProperty
                         */
                        return this.i18n.translate('editor.folder_not_found', { id: this.tagProperty.folderId });
                    } else {
                        return '';
                    }
                }),
            ),
        ).pipe(
            tap(() => this.changeDetector.markForCheck()),
        );

        this.tagPart = tagPart;
        this.readOnly = context.readOnly;
        this.page = context.page;
        this.updateTagProperty(tagProperty);

        this.selectedFolder.selectedItem$
            .pipe(
                switchMap((selectedFolder) => {
                    if (selectedFolder) {
                        return this.client.folder.get(selectedFolder.id)
                            .pipe(
                                map(response => response.folder),
                                catchError(err => of(err)),
                                tap((folder: Folder<Raw>) => {
                                    this.uploadDestination = folder;
                                    this.changeDetector.markForCheck();
                                }),
                            )
                    }

                    return this.client.folder.get(this.page.folderId)
                        .pipe(
                            map(response => response.folder),
                            catchError(err => of(err)),
                            tap((folder: Folder<Raw>) => {
                                this.uploadDestination = folder;
                                this.changeDetector.markForCheck();
                            }),
                        )
                }),
                takeUntil(this.stopper.stopper$),
            )
            .subscribe();
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

    /**
     * Opens the repository browser to allow the user to select a folder.
     */
    browseForItem(): void {
        let contentLanguage: string;
        if (this.page) {
            contentLanguage = this.page.language;
        }
        this.repositoryBrowserClient.openRepositoryBrowser({
            allowedSelection: 'folder',
            selectMultiple: false,
            contentLanguage,
            startFolder: this.uploadDestination ? this.uploadDestination.id : undefined,
        }).then(selectedItem => this.changeSelectedItem(selectedItem));
    }

    onUpload(uploadedItem: FileUpload): void {
        this.uploadedFiles.push(uploadedItem.file);
        this.expandUploadButton.collapse();
        this.changeDetector.markForCheck();
    }

    onCreateSubfolderClick(): void {
        this.creatingSubfolder = true;
        this.selectedFolder.selectedItem$.pipe(take(1)).subscribe(parentFolder => {
            this.folderActions.createNewFolder({
                name: this.subfolderName,
                description: '',
                publishDir: parentFolder.publishDir,
                motherId: parentFolder.id,
                nodeId: parentFolder.nodeId,
                failOnDuplicate: true,
            })
                .then(subfolder => {
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
        this.tagProperty = newValue ;

        this.selectedFolder.setSelectedItem(this.tagProperty.folderId, this.tagProperty.nodeId);
        this.changeDetector.markForCheck();
    }

}
