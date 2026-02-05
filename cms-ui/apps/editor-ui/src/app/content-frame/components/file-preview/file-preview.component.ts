import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    Output
} from '@angular/core';
import { EditMode } from '@gentics/cms-integration-api-models';
import {
    File as FileModel,
    Image as ImageModel,
    PermissionsMapCollection,
    RotateParameters,
    User
} from '@gentics/cms-models';
import { ChangesOf } from '@gentics/ui-core';
import { from, Subscription } from 'rxjs';
import { publishReplay, refCount, switchMap } from 'rxjs/operators';
import { getFileExtension } from '../../../common/utils/get-file-extension';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { I18nNotification } from '../../../core/providers/i18n-notification/i18n-notification.service';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { PermissionService } from '../../../core/providers/permissions/permission.service';
import { ResourceUrlBuilder } from '../../../core/providers/resource-url-builder/resource-url-builder';
import { ApplicationStateService, FolderActionsService } from '../../../state';

interface ImageVariant {
    mediaQuery: string;
    url: string;
}

@Component({
    selector: 'gtx-file-preview',
    templateUrl: './file-preview.component.html',
    styleUrls: ['./file-preview.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class FilePreviewComponent implements OnChanges, OnDestroy {

    @Input()
    public file: FileModel | ImageModel;

    @Input()
    public nodeId: number;

    @Output()
    public fileChange = new EventEmitter<FileModel | ImageModel>();

    public imageVariants: ImageVariant[] = [];
    public imageUrl: string;
    public imageDimensions: { width: number; height: number } | null = null;

    public downloadUrl: string;
    public fileExtension: string;
    public keepFileName = true;

    public canEdit = false;
    public loading = false;

    private permSubscription: Subscription;
    private modifySubscription: Subscription;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private resourceUrlBuilder: ResourceUrlBuilder,
        private navigationService: NavigationService,
        private appState: ApplicationStateService,
        public permissions: PermissionService,
        private entityResolver: EntityResolver,
        private notification: I18nNotification,
        private folderActions: FolderActionsService
    ) {}

    ngOnChanges(changes: ChangesOf<this>): void {
        const fileChanged = changes.file;
        if (fileChanged) {
            this.handleFileUpdate();
        }
    }

    ngOnDestroy(): void {
        if (this.permSubscription != null) {
            this.permSubscription.unsubscribe();
        }
        if (this.modifySubscription != null) {
            this.modifySubscription.unsubscribe();
        }
    }

    private handleFileUpdate(): void {
        this.loadPermissions();

        if (this.file.type === 'image') {
            this.generateImageUrls();
            this.imageDimensions = { width: this.file.sizeX, height: this.file.sizeY };
        }

        this.fileExtension = getFileExtension(this.file.name);
        this.generateDownloadUrl();
    }

    private loadPermissions(): void {
        // Cancel loading of current permissions
        if (this.permSubscription != null) {
            this.permSubscription.unsubscribe();
        }

        this.canEdit = false;

        // Just in case
        if (this.file == null) {
            this.permSubscription = null;
            this.loading = false;
            this.changeDetector.markForCheck();
            return;
        }

        this.loading = true;
        this.changeDetector.markForCheck();

        this.permSubscription = this.permissions.getFolderPermissionMap(this.file.folderId).subscribe(perms => {
            this.canEdit = this.hasEditPermission(perms, this.file.type === 'file');
            this.loading = false;
            this.changeDetector.markForCheck();
        });
    }

    editImage(): void {
        const nodeId = this.appState.now.editor.nodeId;
        this.navigationService.modal(nodeId, 'image', this.file.id, EditMode.EDIT).navigate();
    }

    rotateImage(direction: 'cw' | 'ccw'): void {
        // Don't start another operation while it's already loading
        if (this.loading) {
            return;
        }

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

        this.modifySubscription = this.folderActions.rotateImage(rotateParameters).subscribe((loadedImage) => {
            this.fileChange.emit(loadedImage);

            this.file = loadedImage;
            this.handleFileUpdate();

            this.loading = false;
            this.changeDetector.markForCheck();
        });
    }

    replaceFile(files: File[]): void {
        if (!files || !files.length) {
            console.error('No files selected in FilePreview::replaceFile()');
            return;
        }

        // Already working, can't start another
        if (this.loading) {
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

        this.loading = true;
        this.changeDetector.markForCheck();

        this.modifySubscription = upload.pipe(
            switchMap(() => this.folderActions.getImage(this.file.id, { nodeId: this.nodeId })),
        ).subscribe({
            next: (loadedImage) => {
                this.fileChange.emit(loadedImage);

                this.file = loadedImage;
                this.handleFileUpdate();

                this.loading = false;
                this.changeDetector.markForCheck();

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
            },
            error: err => {
                this.loading = false;
                this.changeDetector.markForCheck();

                // TODO: Use error-handler
                this.notification.show({
                    message: 'message.file_uploads_error',
                    translationParams: {
                        _type: this.file.type,
                        files: this.file.name,
                    },
                    type: 'alert',
                    delay: 10000,
                });
            },
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

    public onImageLoadError(event: ErrorEvent): void {
        console.error(event);
        const toast = this.notification.show({
            message: 'message.image_load_error',
            translationParams: {
                filename: this.file.name,
            },
            type: 'default',
            delay: 5000,
        });
    }

    private generateDownloadUrl(): string {
        // since this component is currently only used in the ContentFrame, we can use the nodeId stored in the editor state
        const nodeId = this.appState.now.editor.nodeId;
        return this.downloadUrl = this.resourceUrlBuilder.fileDownload(this.file.id, nodeId);
    }

    private generateImageUrls(): void {
        const image = this.file as ImageModel;

        this.imageVariants = [];
        this.imageUrl = this.resourceUrlBuilder.imageFullsize(this.file.id, this.nodeId, this.file.edate || this.file.cdate);

        const widths = [460, 860, 1400, 1920];
        for (const resizeWidth of widths) {
            if (image.sizeX > resizeWidth) {
                this.imageVariants.push({
                    mediaQuery: `(max-width: ${resizeWidth}px)`,
                    url: this.resourceUrlBuilder.imageThumbnail(this.file.id, resizeWidth, 'auto', this.nodeId, this.file.edate || this.file.cdate),
                });
            }
        }
    }

    get creator(): User {
        return this.entityResolver.getUser((<any> this.file.creator) as number);
    }

    get editor(): User {
        return this.entityResolver.getUser((<any> this.file.editor) as number);
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
