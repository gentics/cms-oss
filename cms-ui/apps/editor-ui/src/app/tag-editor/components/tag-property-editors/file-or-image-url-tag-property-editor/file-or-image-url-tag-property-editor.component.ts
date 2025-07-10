import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { EditorOverlayService } from '@editor-ui/app/editor-overlay/providers/editor-overlay.service';
import { RepositoryBrowserClient } from '@editor-ui/app/shared/providers';
import { SelectedItemHelper } from '@editor-ui/app/shared/util/selected-item-helper/selected-item-helper';
import { ApplicationStateService } from '@editor-ui/app/state/index';
import { RepositoryBrowserOptions, TagEditorContext, TagEditorError, TagPropertiesChangedFn, TagPropertyEditor } from '@gentics/cms-integration-api-models';
import {
    EditableTag,
    FileOrImage,
    FileTagPartProperty,
    FileUpload,
    Folder,
    Image,
    ImageTagPartProperty,
    ItemInNode,
    Page,
    Raw,
    TagPart,
    TagPartProperty,
    TagPropertyMap,
    TagPropertyType,
} from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { ModalService } from '@gentics/ui-core';
import { Observable, Subscription, merge } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { UploadWithPropertiesModalComponent } from '../../shared/upload-with-properties-modal/upload-with-properties-modal.component';

/**
 * Used to edit the following TagParts:
 * - UrlImage
 * - UrlFile
 * - FileUpload
 */
@Component({
    selector: 'file-or-image-url-tag-property-editor',
    templateUrl: './file-or-image-url-tag-property-editor.component.html',
    styleUrls: ['./file-or-image-url-tag-property-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class FileOrImageUrlTagPropertyEditor implements TagPropertyEditor, OnInit, OnDestroy {

    /** The TagPart that the hosted TagPropertyEditor is responsible for. */
    tagPart: TagPart;

    /** The TagProperty that we are editing. */
    tagProperty: FileTagPartProperty | ImageTagPartProperty;

    /** The current TagEditorContext. */
    context: TagEditorContext;

    /** The string that should be displayed in the input field. */
    displayValue$: Observable<string>;

    /** The destination folder for a file/image upload. */
    uploadDestination: Folder<Raw>;

    /** The breadcrumbs path of the selected item. */
    selectedItemPath: string;

    /** Indicates if the imagemanipulation2 feature is enabled. */
    imageManipulationEnabled$: Observable<boolean>;

    /** Indicates if the image_upload_in_tagfill feature is enabled. */
    imageUploadInTagfillEnabled$: Observable<boolean>;

    /** The type of item we are dealing with. */
    itemType: 'file' | 'image';

    /** Page this edited tag belongs to */
    private page?: Page<Raw>;

    /** The onChange function registered by the TagEditor. */
    private onChangeFn: TagPropertiesChangedFn;

    /** The helper for managing and loading the selected item. */
    private selectedItem: SelectedItemHelper<ItemInNode<FileOrImage<Raw>>>;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private client: GCMSRestClientService,
        private appState: ApplicationStateService,
        private editorOverlayService: EditorOverlayService,
        private repositoryBrowserClient: RepositoryBrowserClient,
        private i18n: I18nService,
        private modalService: ModalService,
    ) { }

    ngOnInit(): void {
        this.imageManipulationEnabled$ = this.appState.select(state => state.features.imagemanipulation2);
        this.imageUploadInTagfillEnabled$ = this.appState.select(state => state.features.enable_image_upload_in_tagfill);
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    initTagPropertyEditor(tagPart: TagPart, tag: EditableTag, tagProperty: TagPartProperty, context: TagEditorContext): void {
        switch (tagProperty.type) {
            case TagPropertyType.FILE:
                this.itemType = 'file';
                break;
            case TagPropertyType.IMAGE:
                this.itemType = 'image';
                break;
            default:
                throw new TagEditorError(`TagPropertyType ${tagProperty.type} not supported by FileOrImageUrlTagPropertyEditor.`);
        }

        this.selectedItem = new SelectedItemHelper(this.itemType, context.node.id, this.client);

        this.displayValue$ = merge(
            this.selectedItem.selectedItem$.pipe(
                map((selectedItem: FileOrImage<Raw>) => {
                    this.selectedItemPath = this.generateBreadcrumbsPath(selectedItem);
                    if (selectedItem) {
                        return selectedItem.name;
                    } else {
                        /**
                         * null is emitted, when nothing is selected.
                         *
                         * Notice, that the following only holds for images, not for folders:
                         * Also, null is emitted in case a referenced image got deleted and the tag property data was refetched.
                         * (Since the imageId in tagProperty gets removed).
                         */
                        switch (tagProperty.type) {
                            case TagPropertyType.FILE:
                                return this.i18n.translate('editor.file_no_selection');
                            case TagPropertyType.IMAGE:
                                return this.i18n.translate('editor.image_no_selection');
                            default:
                                return '';
                        }
                    }
                }),
            ),
            this.selectedItem.loadingError$.pipe(
                map((error: { error: any, item: { itemId: number, nodeId?: number } }) => {
                    /**
                     * When a file or an image that is referenced gets deleted, the fileId or imageId is kept in tagProperty.
                     * When we try to fetch the file or image information we get an error message.
                     * In that case we want to inform the user that the file or image got deleted
                     * (and thus avoid suggesting that a valid file or image is still selected).
                     */
                    if (this.tagProperty) {
                        /** additional check, in case the loadingError$ Subject is changed to a BehaviorSubject in the future.
                         * This could trigger an emission before this.tagProperty is set in updateTagProperty
                         */
                        switch (this.tagProperty.type) {
                            case TagPropertyType.FILE:
                                return this.i18n.translate('editor.file_not_found', { id: this.tagProperty.fileId });
                            case TagPropertyType.IMAGE:
                                return this.i18n.translate('editor.image_not_found', { id: this.tagProperty.imageId });
                            default:
                                return '';
                        }
                    } else {
                        return '';
                    }
                }),
            ),
        ).pipe(
            tap(() => this.changeDetector.markForCheck()),
        );

        this.tagPart = tagPart;
        this.context = context;
        this.page = context.page;
        this.updateTagProperty(tagProperty);

        setTimeout(() => {
            this.loadUploadDestinationFolder(context);
            this.changeDetector.markForCheck();
        });
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
     * Changes the values of this.tagProperty and this.selectedItem$ according
     * to newSelectedItem. This method must only be called in response to
     * user input.
     */
    changeSelectedItem(newSelectedItem: ItemInNode<FileOrImage<Raw>>): void {
        const idProp: keyof (FileTagPartProperty & ImageTagPartProperty) = this.tagProperty.type === TagPropertyType.FILE ? 'fileId' : 'imageId';
        if (newSelectedItem) {
            (<any>this.tagProperty)[idProp] = newSelectedItem.id;
            this.tagProperty.nodeId = newSelectedItem.nodeId;
        } else {
            (<any>this.tagProperty)[idProp] = 0;
            this.tagProperty.nodeId = 0;
        }

        if (this.onChangeFn) {
            const changes: Partial<TagPropertyMap> = {};
            changes[this.tagPart.keyword] = this.tagProperty;
            this.onChangeFn(changes);
        }
        this.selectedItem.setSelectedItem(newSelectedItem);
    }

    /**
     * Opens the repository browser to allow the user to select an item.
     */
    browseForItem(): void {
        let contentLanguage: string;
        if (this.page) {
            contentLanguage = this.page.language;
        }
        const options: RepositoryBrowserOptions = {
            allowedSelection: this.itemType,
            selectMultiple: false,
            contentLanguage,
            startFolder: this.uploadDestination ? this.uploadDestination.id : undefined,
        };

        this.repositoryBrowserClient.openRepositoryBrowser(options)
            .then((selectedItem: ItemInNode<FileOrImage<Raw>>) => this.changeSelectedItem(selectedItem));
    }

    /**
     * Opens an upload modal to allow the user to upload an item.
     */
    uploadItem(): void {
        this.modalService.fromComponent(
            UploadWithPropertiesModalComponent,
            { padding: true, width: '1000px' },
            {
                allowFolderSelection: true,
                destinationFolder: this.uploadDestination,
                itemType: this.itemType,
            },
        )
            .then(modal => modal.open())
            .then((uploadedItem: FileUpload) => {
                if (!uploadedItem) {
                    return;
                }
                const itemWithNode: ItemInNode<FileOrImage<Raw>> = {
                    ...uploadedItem.file,
                    nodeId: uploadedItem.destinationFolder.nodeId,
                };
                this.changeSelectedItem(itemWithNode);
                this.changeDetector.markForCheck();
            });
    }

    editImage(nodeId: number, imageId: number): void {
        if (!nodeId) {
            nodeId = this.context.node.id;
        }
        this.editorOverlayService.editImage({ nodeId: nodeId, itemId: imageId })
            .then(newImage => {
                if (!newImage) {
                    return;
                }
                const imageWithNodeId: ItemInNode<Image<Raw>> = newImage as any;
                imageWithNodeId.nodeId = nodeId;
                this.changeSelectedItem(imageWithNodeId);
            });
    }

    /**
     * Used to update the currently edited TagProperty with external changes.
     */
    private updateTagProperty(newValue: TagPartProperty): void {
        if (newValue.type !== TagPropertyType.FILE && newValue.type !== TagPropertyType.IMAGE) {
            throw new TagEditorError(`TagPropertyType ${newValue.type} not supported by FileOrImageUrlTagPropertyEditor.`);
        }
        this.tagProperty = newValue;

        let itemId: number;
        switch (this.tagProperty.type) {
            case TagPropertyType.FILE:
                itemId = this.tagProperty.fileId;
                break;
            case TagPropertyType.IMAGE:
                itemId = this.tagProperty.imageId;
                break;
        }
        itemId = itemId || null;
        this.selectedItem.setSelectedItem(itemId, this.tagProperty.nodeId);
        this.changeDetector.markForCheck();
    }

    /**
     * Sets this.uploadDestination to the initial upload destination folder
     * based on the context, loading the folder if necessary.
     */
    private loadUploadDestinationFolder(context: TagEditorContext): void {
        let folderId: number = null;
        let folderObj: Folder<Raw> = null;

        if (context.page) {
            folderId = context.page.folderId;
            folderObj = context.page.folder;
        }

        if (context.template) {
            folderId = context.template.folderId;
        }

        if (context.file) {
            folderId = context.file.folderId;
        }

        if (context.image) {
            folderId = context.image.folderId;
        }

        switch (this.itemType) {
            case 'file':
                if (context.node.defaultFileFolderId) {
                    folderId = context.node.defaultFileFolderId;
                    folderObj = null;
                }
                break;
            case 'image':
                if (context.node.defaultImageFolderId) {
                    folderId = context.node.defaultImageFolderId;
                    folderObj = null;
                }
                break;
        }

        if (context.folder) {
            folderObj = context.folder;
        }

        this.selectedItem.selectedItem$.subscribe(selectedItem => {
            if (selectedItem) {
                const sub = this.client.folder.get(selectedItem.folderId, { nodeId: selectedItem.nodeId }).subscribe(res => {
                    this.uploadDestination = res.folder;
                    this.changeDetector.markForCheck();
                });
                this.subscriptions.push(sub);
            } else if (folderObj) {
                this.uploadDestination = folderObj;
                this.changeDetector.markForCheck();
            } else {
                const sub = this.client.folder.get(folderId, { nodeId: context.node?.id }).subscribe(res => {
                    this.uploadDestination = res.folder;
                    this.changeDetector.markForCheck();
                });
                this.subscriptions.push(sub);
            }
        });
    }

    /**
     * @returns A string with the breadcrumbs path of the specified File or Image.
     */
    private generateBreadcrumbsPath(selectedItem: FileOrImage): string {
        let breadcrumbsPath = '';
        if (selectedItem) {
            breadcrumbsPath = selectedItem.path.replace('/', '');
            if (breadcrumbsPath.length > 0 && breadcrumbsPath.charAt(breadcrumbsPath.length - 1) === '/') {
                breadcrumbsPath = breadcrumbsPath.substring(0, breadcrumbsPath.length - 1);
            }
            breadcrumbsPath = breadcrumbsPath.split('/').join(' > ');
        }
        return breadcrumbsPath;
    }

}
