import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    Output,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import {
    EditMode,
    File as FileModel,
    Folder,
    Image as ImageModel,
    PermissionsMapCollection,
    RotateParameters,
    User,
} from '@gentics/cms-models';
import { ProgressBarComponent } from '@gentics/ui-core';
import { Observable, Subscription, of } from 'rxjs';
import { map, publishReplay, refCount, startWith, switchMap } from 'rxjs/operators';
import { getFileExtension } from '../../../common/utils/get-file-extension';
import { isEditableImage } from '../../../common/utils/is-editable-image';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { I18nNotification } from '../../../core/providers/i18n-notification/i18n-notification.service';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { PermissionService } from '../../../core/providers/permissions/permission.service';
import { ResourceUrlBuilder } from '../../../core/providers/resource-url-builder/resource-url-builder';
import { ApplicationStateService, ApplyImageDimensionsAction, FolderActionsService } from '../../../state';

@Component({
    selector: 'file-preview',
    templateUrl: './file-preview.tpl.html',
    styleUrls: ['./file-preview.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FilePreviewComponent implements OnChanges, OnDestroy {

    @Input()
    public file: FileModel | ImageModel;

    @Output()
    public imageLoading = new EventEmitter<boolean>();

    @ViewChild('thumbnailImage')
    public thumbnailImage: ElementRef;

    imageUrl: string;
    downloadUrl: string;
    fileExtension: string;
    keepFileName = true;
    nodeId: number;
    private maxImageFullsizeDimensions = this.calculateMaxImageDimensions();
    private subscriptions = new Subscription();
    isEditableImage = isEditableImage;
    hasFileEditPermission$: Observable<boolean>;

    get displayDimensions(): any {
        return (this.file.type === 'image' && this.file.sizeX !== 0 && this.file.sizeY !== 0) ?
            { width: this.file.sizeX, height: this.file.sizeY } :
            // If there are no valid sizes, this is the fallback size
            // to display SVGs in Internet Explorer (which should maintain its aspect ratio).
            { width: 480, height: 270 };
    }

    private dismissErrorMessage(): void {}

    constructor(private resourceUrlBuilder: ResourceUrlBuilder,
        private navigationService: NavigationService,
        private appState: ApplicationStateService,
        public permissions: PermissionService,
        private entityResolver: EntityResolver,
        private notification: I18nNotification,
        private folderActions: FolderActionsService) {
        this.subscriptions.add(
            this.appState.select(state => state.entities).subscribe(entities => {
                this.hasFileEditPermission$ = this.appState.select(() =>
                    this.file.type === 'file' ? entities.file[this.file.id].folderId : entities.image[this.file.id].folderId)
                    .pipe(
                        switchMap(folderId => this.appState.select(state => state.entities.folder[folderId])),
                        switchMap((folder: Folder) => {
                            if (folder) {
                                const permissionsMap = folder.permissionsMap;
                                const isFile = this.file.type === 'file';
                                if (!permissionsMap) {
                                    return this.permissions.getFolderPermissionMap(folder.id).pipe(
                                        map((permissionsMap: PermissionsMapCollection) => {
                                            return this.hasEditPermission(permissionsMap, isFile);
                                        }),
                                    );
                                }
                                return of(this.hasEditPermission(permissionsMap, isFile));
                            }
                            return of(false);
                        }),
                    );
            }),
        );
    }

    ngOnChanges(changes: SimpleChanges): void {
        const fileChanged = changes['file'];
        if (fileChanged) {
            if (this.file.type === 'image') {
                const prev: ImageModel = fileChanged.previousValue || {};
                const next: ImageModel = fileChanged.currentValue || {};
                if (next.id !== prev.id || next.sizeX !== prev.sizeX || next.sizeY !== prev.sizeY || next.edate !== prev.edate) {
                    this.dismissErrorMessage();
                    const imageUrl = this.generateImageUrl();
                    if (imageUrl !== this.imageUrl) {
                        this.imageLoading.emit(true);
                    }
                }
            }
            this.fileExtension = getFileExtension(this.file.name);
            this.generateDownloadUrl();
        }

        this.nodeId = this.appState.now.editor.nodeId;
    }

    ngOnDestroy(): void {
        this.dismissErrorMessage();
        this.subscriptions.unsubscribe();
    }

    editImage(): void {
        const nodeId = this.appState.now.editor.nodeId;
        this.navigationService.modal(nodeId, 'image', this.file.id, EditMode.EDIT).navigate();
    }

    rotateImage(direction: 'cw' | 'ccw'): void {
        let targetFormat;
        switch (this.file.fileType) {
            case 'image/jpeg':
                targetFormat = 'jpg';
                break;
            case 'image/png':
                targetFormat = 'png';
                break;
            default:
                targetFormat = 'png';
                break;
        }
        const rotateParameters: RotateParameters = {
            image: {
                id: this.file.id,
            },
            targetFormat,
            copyFile: false,
            rotate: direction,
        };
        this.folderActions.rotateImage(rotateParameters);
    }

    replaceFile(files: File[], uploadProgress: ProgressBarComponent): void {
        if (!files || !files.length) {
            console.error('No files selected in FilePreview::replaceFile()');
            return;
        }

        const nodeId = this.appState.now.editor.nodeId;
        const fileNameToReplace = this.keepFileName ? this.file.name : undefined;

        const upload = this.folderActions.replaceFile(
            this.file.type,
            this.file.id,
            files[0],
            fileNameToReplace,
            { nodeId },
        ).pipe(
            publishReplay(1),
            refCount(),
        );

        uploadProgress.start(upload.pipe(
            startWith(0),
            map(() => 1),
        ));

        if (this.keepFileName && (this.file.fileType !== files[0].type) ) {
            this.notification.show({
                message: 'message.file_type_changed_warning',
                translationParams: {
                    fileName: this.file.name,
                    newFileName: files[0].name,
                },
                type: 'alert',
                delay: 0,
            });
        }

        upload.subscribe(() => {
            if (!this.keepFileName) {
                this.notification.show({
                    message: 'message.file_replaced_with_success',
                    translationParams: {
                        fileName: this.file.name,
                        newFileName: files[0].name,
                    },
                    type: 'success',
                    delay: 5000,
                });
            } else {
                this.notification.show({
                    message: 'message.file_replaced_success',
                    translationParams: {
                        fileName: this.file.name,
                    },
                    type: 'success',
                    delay: 5000,
                });
            }
        });
    }

    acceptedFiles(): string {
        if (this.keepFileName && this.fileExtension) {
            return '.' + this.fileExtension;
        } else if (this.file.type === 'image') {
            return 'image/*';
        } else {
            return '!image/*';
        }
    }

    onImageLoadSuccess(): void {
        this.imageLoading.emit(false);
        const image = this.thumbnailImage.nativeElement as HTMLImageElement;
        const unknownDimension = (this.file.type === 'image') ? this.file.sizeX === 0 || this.file.sizeY === 0 : false;

        if (unknownDimension && image.naturalWidth !== 0 && image.naturalHeight !== 0) {
            this.appState.dispatch(new ApplyImageDimensionsAction(this.file.id, image.naturalWidth, image.naturalHeight));
        }
    }

    private onImageLoadError(event: ErrorEvent): void {
        this.imageLoading.emit(false);
        const toast = this.notification.show({
            message: 'message.image_load_error',
            translationParams: {
                filename: this.file.name,
            },
            type: 'default',
            delay: 5000,
            action: {
                label: 'common.retry_button',
                onClick: (): void => {
                    const img = event.target as HTMLImageElement;
                    img.setAttribute('src', this.generateImageUrl());
                },
            },
        });
        this.dismissErrorMessage = () => toast.dismiss();
    }

    private generateDownloadUrl(): string {
        // since this component is currently only used in the ContentFrame, we can use the nodeId stored in the editor state
        const nodeId = this.appState.now.editor.nodeId;
        return this.downloadUrl = this.resourceUrlBuilder.fileDownload(this.file.id, nodeId);
    }

    private generateImageUrl(): string {
        const image = this.file as ImageModel;
        // since this component is currently only used in the ContentFrame, we can use the nodeId stored in the editor state
        const nodeId = this.appState.now.editor.nodeId;
        if (image.sizeX < this.maxImageFullsizeDimensions && image.sizeX < this.maxImageFullsizeDimensions) {
            return this.imageUrl = this.resourceUrlBuilder.imageFullsize(this.file.id, nodeId, this.file.edate || this.file.cdate);
        } else {
            const factor = Math.min(this.maxImageFullsizeDimensions / image.sizeX, this.maxImageFullsizeDimensions / image.sizeY);
            const width = Math.round(factor * image.sizeX);
            const height = Math.round(factor * image.sizeY);
            const fileType = this.file.fileType;
            return this.imageUrl = this.resourceUrlBuilder.imageThumbnail(this.file.id, width, height, nodeId, this.file.edate || this.file.cdate, fileType);
        }
    }

    get creator(): User {
        return this.entityResolver.getUser((<any> this.file.creator) as number);
    }

    get editor(): User {
        return this.entityResolver.getUser((<any> this.file.editor) as number);
    }

    private calculateMaxImageDimensions(): number {
        return Math.min(2000, Math.max(screen.width, screen.height));
    }

    private hasEditPermission(permissionsMap: PermissionsMapCollection, isFile: boolean): boolean {
        const permissions = permissionsMap.permissions;
        if (isFile) {
            const rolePermissions = permissionsMap.rolePermissions;
            return (!!permissions && permissions.updateitems) || (!!rolePermissions && rolePermissions.file.updateitems);
        }
        return !!permissions && permissions.updateitems;
    }
}
