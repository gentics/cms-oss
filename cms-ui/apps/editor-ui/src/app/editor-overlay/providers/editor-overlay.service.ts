import { Injectable, Type } from '@angular/core';
import { CropResizeParameters, File, Form, Image, InheritableItem, Page, Raw } from '@gentics/cms-models';
import { IModalDialog, IModalInstance, IModalOptions, ModalService } from '@gentics/ui-core';
import { Subject } from 'rxjs';
import { AssignPageModal } from '../../core/components/assign-page-modal/assign-page-modal.component';
import { PublishQueueModal } from '../../core/components/publish-queue-modal/publish-queue-modal.component';
import { EntityResolver } from '../../core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '../../core/providers/error-handler/error-handler.service';
import { ApplicationStateService, FolderActionsService, NodeSettingsActionsService } from '../../state';
import { ImageEditorModalComponent } from '../components/image-editor-modal/image-editor-modal.component';

@Injectable()
export class EditorOverlayService {

    editorOverlayOnOpen$ = new Subject<IModalInstance<any>>();
    editorOverlayOnClose$ = new Subject<IModalInstance<any>>();

    constructor(
        private appState: ApplicationStateService,
        private entityResolver: EntityResolver,
        private errorHandler: ErrorHandler,
        private folderActions: FolderActionsService,
        private modalService: ModalService,
        private nodeSettingsActions: NodeSettingsActionsService,
    ) {}

    /**
     * Opens the image editor modal for the image specified in the options.
     *
     * @returns A promise, which resolves to the edited image object or to void if the
     * user cancels the dialog or if an error occurs.
     */
    editImage(options: { nodeId: number, itemId: number }): Promise<Image<Raw> | void> {
        const round = (val: number) => Math.round(val);
        let image: Image;

        return this.getOrLoadItem('image', options.itemId, options.nodeId)
            .then(item => {
                image = item;
                return this.openEditor(ImageEditorModalComponent,
                    {
                        nodeId: options.nodeId,
                        image: image,
                        initialFocalPoints: {
                            focalPointX: image.fpX,
                            focalPointY: image.fpY,
                        },
                    },
                );
            })
            .then(modal => modal.open())
            .then(result => {
                const params = result.params;
                const fileExt = image.name.substr(image.name.lastIndexOf('.') + 1).toLowerCase();
                const format: 'png' | 'jpg' = fileExt === 'jpg' || fileExt === 'jpeg' ? 'jpg' : 'png';

                const apiParams: CropResizeParameters = {
                    image: {
                        id: image.id,
                    },
                    copyFile: result.asCopy,
                    cropWidth: round(params.cropRect.width),
                    cropHeight: round(params.cropRect.height),
                    cropStartX: round(params.cropRect.startX),
                    cropStartY: round(params.cropRect.startY),
                    width: params.width,
                    height: params.height,
                    fpX: params.focalPointX,
                    fpY: params.focalPointY,
                    mode: 'cropandresize',
                    resizeMode: 'force',
                    targetFormat: format,
                };
                return this.folderActions.cropAndResizeImage(image, apiParams);
            })
            // We get here if the loading of the image fails, cropAndResizeImage() handles its errors internally.
            .catch(this.errorHandler.catch);
    }

    /**
     * Opens the publish queue modal.
     */
    displayPublishQueue(): void {
        let modalInstance: IModalInstance<PublishQueueModal>;
        modalInstance = undefined;

        this.modalService.fromComponent(PublishQueueModal, {
            onOpen: () => { this.editorOverlayOnOpen$.next(modalInstance); },
            onClose: () => { this.editorOverlayOnClose$.next(modalInstance); },
        })
            .then(modal => modalInstance = modal)
            .then<Page[]>(modal => modal.open())
            .then(pages => {
                if (pages !== null) {
                    return this.modalService.fromComponent(AssignPageModal, {}, {pages})
                        .then(modal => modal.open());
                }
            })
            .catch(this.errorHandler.catch);
    }

    /**
     * Wraps the ModalService fromComponent method with default arguments
     */
    openEditor<T extends IModalDialog>(
        component: Type<T>,
        locals?: any,
        options?: IModalOptions,
    ): Promise<IModalInstance<T>> {

        let modalInstance: IModalInstance<T>;

        let defaultOptions = {
            onOpen: () => { this.editorOverlayOnOpen$.next(modalInstance); },
            onClose: () => { this.editorOverlayOnClose$.next(modalInstance); },
            width: '100%',
            closeOnOverlayClick: false,
            closeOnEscape: false,
        };

        let modal = this.modalService.fromComponent(component,
            { ...defaultOptions, ...options },
            { ...locals },
        );
        modal.then(modal => modalInstance = modal);
        return modal;
    }

    /**
     * Loads the specified item and also makes sure that the specified node's settings are loaded.
     */
    private getOrLoadItem(itemType: 'page', itemId: number, nodeId: number): Promise<Page>;
    private getOrLoadItem(itemType: 'image', itemId: number, nodeId: number): Promise<Image>;
    private getOrLoadItem(itemType: 'file', itemId: number, nodeId: number): Promise<File>;
    private getOrLoadItem(itemType: 'form', itemId: number, nodeId: number): Promise<Form>;
    private getOrLoadItem(itemType: 'page' | 'image' | 'file' | 'form', itemId: number, nodeId: number): Promise<InheritableItem> {
        let ret: Promise<InheritableItem>;

        let item = this.entityResolver.getEntity(itemType, itemId);
        if (item) {
            ret = Promise.resolve(item);
        } else {
            ret = this.folderActions.getItem(itemId, itemType, { nodeId: nodeId }) as Promise<InheritableItem>;
        }

        // Make sure, that the correct nodeId's settings are loaded
        if (typeof this.appState.now.nodeSettings.node[nodeId] === 'undefined') {
            ret = ret.then(loadedItem => {
                return this.nodeSettingsActions.loadNodeSettings(nodeId)
                    .then(() => loadedItem);
            });
        }
        return ret;
    }

}
