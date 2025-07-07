import { Injectable } from '@angular/core';
import { stripLeadingSlash } from '@editor-ui/app/common/utils/strip';
import {
    GcmsUiServices,
    ModalClosingReason,
    RepositoryBrowserOptions,
    TagEditorContext,
    TagEditorOptions,
    TagEditorResult,
    VariableTagEditorContext,
} from '@gentics/cms-integration-api-models';
import {
    AnyModelType,
    EditableTag,
    EditorControlStyle,
    File,
    Folder,
    Image,
    ModelType,
    Node,
    Page,
    Tag,
    TagType,
    Template,
} from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { ApiBase } from '@gentics/cms-rest-clients-angular';
import { IModalInstance, ModalService } from '@gentics/ui-core';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { EditorOverlayService } from '../../../editor-overlay/providers/editor-overlay.service';
import { RepositoryBrowserClient } from '../../../shared/providers/repository-browser-client/repository-browser-client.service';
import { UserAgentRef } from '../../../shared/providers/user-agent-ref';
import { ApplicationStateService, DecreaseOverlayCountAction, IncreaseOverlayCountAction, SetTagEditorOpenAction } from '../../../state';
import { TagEditorContextImpl } from '../../common/impl/tag-editor-context-impl';
import { TranslatorImpl } from '../../common/impl/translator-impl';
import { UploadWithPropertiesModalComponent } from '../../components/shared/upload-with-properties-modal/upload-with-properties-modal.component';
import { TagEditorModal } from '../../components/tag-editor-modal/tag-editor-modal.component';

/**
 * Captures information for creating a new `TagEditorContext`.
 *
 * This interface is used when creating a `TagEditorContext` for opening
 * the TagEditor in a custom place, i.e., not as an overlay when editing a page.
 */
export interface EditTagInfo {
    /** The tag that will be edited. */
    tag: Tag;

    /** The TagType, of which the tag is an instance. */
    tagType: TagType;

    /** The page, folder, image, or file to which the tag belongs. */
    tagOwner: Page<AnyModelType> | Folder<AnyModelType> | Image<AnyModelType> | File<AnyModelType> | Template<AnyModelType>;

    /** The node, from which the tagOwner has been opened. */
    node: Node<AnyModelType>;

    /** true if the tag may not be modified. */
    readOnly: boolean;

    /** If the tagOwner object comes from an IFrame, this must be set to true in order to apply polyfills to it in IE. */
    tagOwnerFromIFrame?: boolean;

    withDelete: boolean;
}

/**
 * This service acts as a bridge between the API provided to Aloha and the TagEditorHost.
 */
@Injectable()
export class TagEditorService {

    private tagEditorModal: IModalInstance<TagEditorModal>;

    constructor(
        private appState: ApplicationStateService,
        private editorOverlayService: EditorOverlayService,
        private entityResolver: EntityResolver,
        private repositoryBrowserClient: RepositoryBrowserClient,
        private translateService: TranslateService,
        private userAgentRef: UserAgentRef,
        private modals: ModalService,
        private apiBase: ApiBase,
        private client: GCMSRestClientService,
    ) {}

    /**
     * Opens a tag editor for the specified tag.
     * Based on the configuration of the TagType, either the GenticsTagEditor or
     * a custom tag editor is used.
     *
     * @param tag The tag to be edited - the property tag.tagType must be set.
     * @param tagType The tagtype of the tag
     * @param page The page in which the tag is being edited.
     * @param options The options for opening the Tag-Editor
     * @returns A promise, which when the user clicks OK, resolves and returns a copy of the edited tag
     * and when the user clicks Cancel, rejects.
     */
    async openTagEditor(tag: Tag, tagType: TagType, page: Page<AnyModelType>, options?: TagEditorOptions): Promise<TagEditorResult> {
        // Since the ContentFrame uses the currentNode object when opening a page,
        // we can assume that the entity has already been loaded.
        const node = this.entityResolver.getNode(this.appState.now.editor.nodeId);

        const tagEditorContext = this.createTagEditorContext({
            tag: tag,
            tagType: tagType,
            tagOwner: page,
            node: node,
            readOnly: false, // openTagEditor() is called when a page is in edit mode, so the user has edit permissions.
            withDelete: options?.withDelete ?? tagType.editorControlStyle === EditorControlStyle.CLICK,
            tagOwnerFromIFrame: true,
        });

        await Promise.all([
            this.appState.dispatch(new IncreaseOverlayCountAction()).toPromise(),
            this.appState.dispatch(new SetTagEditorOpenAction(true)).toPromise(),
        ]);

        let result: TagEditorResult;
        let error = null;

        this.tagEditorModal = await this.modals.fromComponent(TagEditorModal, {
            closeOnEscape: false,
            closeOnOverlayClick: false,
            width: '80%',
        }, {
            context: tagEditorContext,
        });

        try {
            result = await this.tagEditorModal.open();

            if (result?.tag) {
                delete result.tag.tagType;
            }
        } catch (err) {
            error = err;
        }

        this.tagEditorModal = null;
        await Promise.all([
            this.appState.dispatch(new DecreaseOverlayCountAction()).toPromise(),
            this.appState.dispatch(new SetTagEditorOpenAction(false)).toPromise(),
        ]);

        if (error) {
            throw error;
        }

        return result;
    }

    /**
     * Force Closes the opened tag editor
     */
    forceCloseTagEditor(): void {
        if (this.tagEditorModal) {
            this.tagEditorModal.instance.cancelFn(null, ModalClosingReason.API);
        }
    }

    /**
     * Creates a new `TagEditorContext` using the specified data.
     *
     * This method is used by `openTagEditor()` and needs to be called manually
     * only if the aforementioned method is not used to open the TagEditor,
     * e.g., if the TagEditor needs to be opened for editing an object property.
     *
     * @param editTagInfo The information needed to create the TagEditorContext.
     */
    createTagEditorContext(editTagInfo: EditTagInfo): TagEditorContext {
        let editableTag: EditableTag = {
            ...editTagInfo.tag,
            tagType: editTagInfo.tagType,
        };

        const gcmsUiServices: GcmsUiServices = {
            openRepositoryBrowser: (options: RepositoryBrowserOptions) =>
                this.repositoryBrowserClient.openRepositoryBrowser(options),
            openImageEditor: (options: { nodeId: number, imageId: number }) =>
                this.editorOverlayService.editImage({ nodeId: options.nodeId, itemId: options.imageId }),
            openUploadModal: (uploadType, destinationFolder, allowFolderSelection) => {
                return this.modals.fromComponent(
                    UploadWithPropertiesModalComponent,
                    { padding: true, width: '1000px' },
                    {
                        itemType: uploadType,
                        allowFolderSelection: allowFolderSelection ?? true,
                        destinationFolder,
                    },
                ).then(dialog => dialog.open());
            },
            restClient: this.client.getClient(),
            restRequestGET: (endpoint: string, params: any): Promise<object> =>
                this.apiBase.get(stripLeadingSlash(endpoint), params).toPromise(),
            restRequestPOST: (endpoint: string, data: object, params?: object): Promise<object> =>
                this.apiBase.post(stripLeadingSlash(endpoint), data, params).toPromise(),
            restRequestDELETE: (endpoint: string, params?: object): Promise<void | object> =>
                this.apiBase.delete(stripLeadingSlash(endpoint), params).toPromise(),
        };

        let tagOwner = editTagInfo.tagOwner;
        if (tagOwner.type === 'page') {
            tagOwner = this.createSafePageCopy(tagOwner);

            if (editTagInfo.tagOwnerFromIFrame && this.userAgentRef.isIE11) {
                editableTag = this.applyIE11Polyfills(editableTag);
                tagOwner = this.applyIE11Polyfills(tagOwner);
            }
        }
        const rawTagOwner = this.entityResolver.denormalizeEntity(tagOwner.type, tagOwner);
        const rawNode = this.entityResolver.denormalizeEntity('node', editTagInfo.node);

        const variableContext$: Observable<VariableTagEditorContext> = this.appState.select(state => ({
            uiLanguage: state.ui.language,
        }));
        const sid = this.appState.now.auth.sid;
        const translator = new TranslatorImpl(this.translateService);

        return TagEditorContextImpl.create(
            editableTag,
            editTagInfo.readOnly,
            rawTagOwner,
            rawNode,
            sid,
            translator,
            variableContext$,
            gcmsUiServices,
            editTagInfo.withDelete,
        );
    }

    /**
     * Creates a shallow copy of the specified page object without the
     * pageVariants and languageVariants arrays, because they may contain
     * cyclic references, which would cause deep cloning to fail.
     */
    private createSafePageCopy<T extends ModelType>(page: Page<T>): Page<T> {
        const safePage = {
            ...page,
        };
        delete safePage.pageVariants;
        delete safePage.languageVariants;
        return safePage;
    }

    /**
     * Applies necessary IE11 polyfills to the specified object.
     *
     * The reason we need this method is that the tag object was created in the
     * Aloha IFrame, which does not contain the polyfills available in the GCMS UI app.
     * Thus we need to apply them now.
     */
    private applyIE11Polyfills(obj: any): any {
        const json = JSON.stringify(obj);
        return JSON.parse(json);
    }

}
