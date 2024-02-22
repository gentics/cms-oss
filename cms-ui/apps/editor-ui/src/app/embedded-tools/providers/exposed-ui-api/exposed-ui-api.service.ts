import { Injectable } from '@angular/core';
import {
    EditMode,
    FileResponse,
    Folder,
    FolderItemType,
    FolderResponse,
    Form,
    FormResponse,
    ImageResponse,
    ItemInNode,
    NodeFeature,
    Page,
    PageResponse,
    TagInContainer,
} from '@gentics/cms-models';
import { IDialogConfig, INotificationOptions, ModalService, NotificationService } from '@gentics/ui-core';
import { map } from 'rxjs/operators';
import { ToolBreadcrumb } from '../../../../../embedded-tools-api/exposed-gcmsui-api';
import { EditorTab, ITEM_PROPERTIES_TAB, PropertiesTab } from '../../../common/models';
import { PublishQueueModal } from '../../../core/components/publish-queue-modal/publish-queue-modal.component';
import { WastebinModal } from '../../../core/components/wastebin-modal/wastebin-modal.component';
import { Api } from '../../../core/providers/api/api.service';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { MessageService } from '../../../core/providers/message/message.service';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { SendMessageModal } from '../../../shared/components/send-message-modal/send-message-modal.component';
import { RepositoryBrowserClient } from '../../../shared/providers/repository-browser-client/repository-browser-client.service';
import { ApplicationStateService, FolderActionsService } from '../../../state';
import { ExposableGCMSUIAPI, NotificationOptions, RepositoryBrowserOptions } from '../tool-api-channel/exposable-gcmsui-api';

/**
 * All methods declared on this class are callable from tool iframes / tabs.
 */
@Injectable()
export class ExposedUIAPI implements ExposableGCMSUIAPI {

    /** Create a copy of an instance with all services injected. */
    static clone(instance: ExposedUIAPI): ExposedUIAPI {
        const prototype: any = Object.getPrototypeOf(instance);
        const copy: any = Object.create(instance);

        for (const key of Object.getOwnPropertyNames(prototype)) {
            if (key !== 'constructor' && typeof prototype[key] === 'function') {
                copy[key] = prototype[key];
            }
        }

        for (const key of Object.getOwnPropertyNames(instance)) {
            copy[key] = (instance as any)[key];
        }

        return copy;
    }

    constructor(
        private api: Api,
        private entityResolver: EntityResolver,
        private folderActions: FolderActionsService,
        private modalService: ModalService,
        private messageService: MessageService,
        private navigation: NavigationService,
        private notification: NotificationService,
        private repoBrowserClient: RepositoryBrowserClient,
        private state: ApplicationStateService,
    ) { }

    navigateToFolder(folderId: number, nodeId?: number): Promise<boolean> {
        nodeId = nodeId || this.state.now.folder.activeNode;
        return this.navigation.list(nodeId, folderId).navigate();
    }

    refreshCurrentFolder(itemType?: 'folder' | 'form' | 'page' | 'file' | 'image'): void {
        if (itemType) {
            this.folderActions.refreshList(itemType);
        } else {
            this.folderActions.refreshList('folder');
            this.folderActions.refreshList('page');
            this.folderActions.refreshList('file');
            this.folderActions.refreshList('image');
            if (this.nodeFeatureIsActive(NodeFeature.FORMS)) {
                this.folderActions.refreshList('form');
            }
        }
    }

    nodeProperties(nodeId: number): Promise<boolean> {
        const node = this.entityResolver.getNode(nodeId);
        return this.navigation.instruction({
            list: {
                nodeId,
                folderId: node.folderId,
            },
            detail: {
                editMode: EditMode.EDIT_PROPERTIES,
                itemId: nodeId,
                itemType: 'node',
                nodeId,
                options: {
                    openTab: 'properties',
                },

            },
        }).navigate();
    }

    private internalNavigate(
        editMode: EditMode,
        itemType: FolderItemType | 'node' | 'channel',
        itemId: number,
        nodeId: number,
        openTab?: EditorTab,
        propertiesTab?: PropertiesTab,
    ): Promise<boolean> {

        return this.api.folders.getItem(itemId, itemType).pipe(
            map(response =>
                (response as FolderResponse).folder ||
                (response as PageResponse).page ||
                (response as FileResponse).file ||
                (response as ImageResponse).image ||
                (response as unknown as FormResponse).item,
            ),
        )
            .toPromise()
            .then(item => {
                nodeId = nodeId || (item as Folder).nodeId || item.inheritedFromId;
                let folderId: number;
                switch (itemType) {
                    case 'folder':
                        folderId = (item as Folder).motherId || item.id;
                        break;
                    case 'page':
                        folderId = (item as Page).folderId;
                        break;
                    case 'form':
                        folderId = (item as Form).folderId;
                        break;
                    default:
                        break;
                }

                return this.navigation.instruction({
                    list: {
                        nodeId,
                        folderId,
                    },
                    detail: {
                        editMode,
                        itemType,
                        itemId,
                        nodeId,
                        options: { openTab, propertiesTab },
                    },
                }).navigate();
            });
    }

    folderProperties(folderId: number, nodeId?: number): Promise<boolean> {
        return this.internalNavigate(EditMode.EDIT_PROPERTIES, 'folder', folderId, nodeId, 'properties', ITEM_PROPERTIES_TAB);
    }

    folderObjectProperties(folderId: number, nodeId?: number): Promise<boolean> {
        return this.internalNavigate(EditMode.EDIT_PROPERTIES, 'folder', folderId, nodeId, 'properties', ITEM_PROPERTIES_TAB);
    }

    previewPage(pageId: number, nodeId?: number): Promise<boolean> {
        return this.internalNavigate(EditMode.PREVIEW, 'page', pageId, nodeId);
    }

    pageProperties(pageId: number, nodeId?: number): Promise<boolean> {
        return this.internalNavigate(EditMode.EDIT_PROPERTIES, 'page', pageId, nodeId, 'properties', ITEM_PROPERTIES_TAB);
    }

    pageObjectProperties(pageId: number, nodeId?: number): Promise<boolean> {
        return this.internalNavigate(EditMode.EDIT_PROPERTIES, 'page', pageId, nodeId, 'properties');
    }

    editPage(pageId: number, nodeId?: number): Promise<boolean> {
        return this.internalNavigate(EditMode.EDIT, 'page', pageId, nodeId);
    }

    previewForm(formId: number, nodeId?: number): Promise<boolean> {
        if (this.nodeFeatureIsActive(NodeFeature.FORMS)) {
            return this.internalNavigate(EditMode.PREVIEW, 'form', formId, nodeId);
        } else {
            throw new Error(`Cannot previewForm of Form with
                ID ${formId}, because Node
                with ID ${nodeId} feature "${NodeFeature.FORMS}" is not active.`);
        }
    }

    formProperties(formId: number, nodeId?: number): Promise<boolean> {
        if (this.nodeFeatureIsActive(NodeFeature.FORMS)) {
            return this.internalNavigate(EditMode.EDIT_PROPERTIES, 'form', formId, nodeId, 'properties', ITEM_PROPERTIES_TAB);
        } else {
            throw new Error(`Cannot formProperties of Form with
                ID ${formId}, because Node
                with ID ${nodeId} feature "${NodeFeature.FORMS}" is not active.`);
        }
    }

    editForm(formId: number, nodeId?: number): Promise<boolean> {
        if (this.nodeFeatureIsActive(NodeFeature.FORMS)) {
            return this.internalNavigate(EditMode.EDIT, 'form', formId, nodeId);
        } else {
            throw new Error(`Cannot editForm of Form with
                ID ${formId}, because Node
                with ID ${nodeId} feature "${NodeFeature.FORMS}" is not active.`);
        }
    }

    previewFile(fileId: number, nodeId?: number): Promise<boolean> {
        return this.internalNavigate(EditMode.EDIT_PROPERTIES, 'file', fileId, nodeId, 'preview');
    }

    fileProperties(fileId: number, nodeId?: number): Promise<boolean> {
        return this.internalNavigate(EditMode.EDIT_PROPERTIES, 'file', fileId, nodeId, 'properties', ITEM_PROPERTIES_TAB);
    }

    fileObjectProperties(fileId: number, nodeId?: number): Promise<boolean> {
        return this.internalNavigate(EditMode.EDIT_PROPERTIES, 'file', fileId, nodeId, 'properties', ITEM_PROPERTIES_TAB);
    }

    previewImage(imageId: number, nodeId?: number): Promise<boolean> {
        return this.internalNavigate(EditMode.EDIT_PROPERTIES, 'image', imageId, nodeId, 'preview');
    }

    imageProperties(imageId: number, nodeId?: number): Promise<boolean> {
        return this.internalNavigate(EditMode.EDIT_PROPERTIES, 'image', imageId, nodeId, 'properties', ITEM_PROPERTIES_TAB);
    }

    imageObjectProperties(imageId: number, nodeId?: number): Promise<boolean> {
        return this.internalNavigate(EditMode.EDIT_PROPERTIES, 'image', imageId, nodeId, 'properties', ITEM_PROPERTIES_TAB);
    }

    editImage(imageId: number, nodeId?: number): Promise<boolean> {
        return this.internalNavigate(EditMode.EDIT, 'image', imageId, nodeId);
    }

    openPublishQueue(): Promise<void> {
        return this.modalService.fromComponent(PublishQueueModal, { width: '1000px' })
            .then(modal => modal.open())
            .then(() => {});
    }

    openWastebin(nodeId?: number): Promise<void> {
        nodeId = nodeId || this.state.now.folder.activeNode;
        return this.modalService.fromComponent(WastebinModal, { width: '1000px' }, { nodeId })
            .then(modal => modal.open())
            .then(() => {});
    }

    openMessageComposer(): void {
        this.modalService.fromComponent(SendMessageModal)
            .then(modal => modal.open())
            .then(() => {});
    }

    openMessageInbox(): void {
        this.messageService.openInbox();
    }

    openRepositoryBrowser(options: RepositoryBrowserOptions): Promise<(ItemInNode | TagInContainer)[]> {
        return this.repoBrowserClient.openRepositoryBrowser(options)
            .then(result => {
                return Array.isArray(result) ? result : [result];
            });
    }

    showDialog(config: IDialogConfig): any {
        return this.modalService
            .dialog(config, { width: '1000px' })
            .then(modal => modal.open());
    }

    showNotification(options: NotificationOptions): Promise<string | undefined> {
        return new Promise(resolve => {
            let timeout: ReturnType<typeof setTimeout>;
            if (options.delay === 0) {
                timeout = null;
            } else {
                timeout = setTimeout(() => resolve(undefined), options.delay || 3000);
            }

            // eslint-disable-next-line prefer-const
            let toast: { dismiss(): void };

            const optionsToUse: INotificationOptions = { ...options };
            if (options.action) {
                const result = 'result' in options.action ? options.action.result : options.action.label;

                optionsToUse.action = {
                    label: options.action.label,
                    onClick(): void {
                        if (timeout) {
                            clearTimeout(timeout);
                            if (options.action.dismiss !== false) {
                                toast.dismiss();
                            }
                            resolve(result);
                        }
                    },
                };
            }

            toast = this.notification.show(optionsToUse);
        });
    }

    // Call should do different things depending on which tool called => Overwrite in a clone
    close(): void { }

    // Call should do different things depending on which tool called => Overwrite in a clone
    navigated(path: string): void { }

    // Call should do different things depending on which tool called => Overwrite in a clone
    provideBreadcrumbs(breadcrumbs: ToolBreadcrumb[]): void { }

    private nodeFeatureIsActive(nodeFeature: NodeFeature): boolean {
        const activeNodeId = this.state.now.folder.activeNode;
        const activeNodeFeatures: NodeFeature[] = this.state.now.features.nodeFeatures[activeNodeId];
        return Array.isArray(activeNodeFeatures) && activeNodeFeatures.includes(nodeFeature);
    }
}

