import { Injectable } from '@angular/core';
import {
    AnyModelType,
    EditableTag,
    File,
    Folder,
    GcmsUiServices,
    Image,
    ModelType,
    Node,
    Page,
    RepositoryBrowserOptions,
    Tag,
    TagEditorContext,
    TagEditorResult,
    TagType,
    Template,
    VariableTagEditorContext,
} from '@gentics/cms-models';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { EditorOverlayService } from '../../../editor-overlay/providers/editor-overlay.service';
import { RepositoryBrowserClient } from '../../../shared/providers/repository-browser-client/repository-browser-client.service';
import { UserAgentRef } from '../../../shared/providers/user-agent-ref';
import { ApplicationStateService, DecreaseOverlayCountAction, IncreaseOverlayCountAction, SetTagEditorOpenAction } from '../../../state';
import { TagEditorContextImpl } from '../../common/impl/tag-editor-context-impl';
import { TranslatorImpl } from '../../common/impl/translator-impl';
import { TagEditorOverlayHostComponent } from '../../components/tag-editor-overlay-host/tag-editor-overlay-host.component';

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

    /**  The page, folder, image, or file to which the tag belongs. */
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

    private tagEditorOverlayHost: TagEditorOverlayHostComponent = null;

    constructor(
        private appState: ApplicationStateService,
        private editorOverlayService: EditorOverlayService,
        private entityResolver: EntityResolver,
        private repositoryBrowserClient: RepositoryBrowserClient,
        private translateService: TranslateService,
        private userAgentRef: UserAgentRef,
    ) {}

    /**
     * Opens a tag editor for the specified tag.
     * Based on the configuration of the TagType, either the GenticsTagEditor or
     * a custom tag editor is used.
     *
     * @param tag The tag to be edited - the property tag.tagType must be set.
     * @param context The current context.
     * @param page The page in which the tag is being edited.
     * @param withDelete If a delete option for the Tag should be provided.
     * @returns A promise, which when the user clicks OK, resolves and returns a copy of the edited tag
     * and when the user clicks Cancel, rejects.
     */
    async openTagEditor(tag: Tag, tagType: TagType, page: Page<AnyModelType>, withDelete?: boolean): Promise<TagEditorResult> {
        // Since the ContentFrame uses the currentNode object when opening a page,
        // we can assume that the entity has already been loaded.
        const node = this.entityResolver.getNode(this.appState.now.editor.nodeId);

        const tagEditorContext = this.createTagEditorContext({
            tag: tag,
            tagType: tagType,
            tagOwner: page,
            node: node,
            readOnly: false, // openTagEditor() is called when a page is in edit mode, so the user has edit permissions.
            withDelete,
            tagOwnerFromIFrame: true,
        });

        await Promise.all([
            this.appState.dispatch(new IncreaseOverlayCountAction()).toPromise(),
            this.appState.dispatch(new SetTagEditorOpenAction(true)).toPromise(),
        ]);
        try {
            const result = await this.tagEditorOverlayHost.openTagEditor(tagEditorContext.editedTag, tagEditorContext)
            if (result.tag) {
                delete result.tag.tagType;
            }
            return result;
        } finally {
            await Promise.all([
                this.appState.dispatch(new DecreaseOverlayCountAction()).toPromise(),
                this.appState.dispatch(new SetTagEditorOpenAction(false)).toPromise(),
            ]);
        }
    }

    /**
     * Force Closes the opened tag editor
     */
    forceCloseTagEditor(): void {
        this.tagEditorOverlayHost.forceCloseTagEditor();
    }

    /**
     * Registers the specified TagEditorOverlayHostComponent with the TagPropertyEditorService.
     * This is needed, such that this service knows, which component it can tell to open the TagEditor.
     * This method should only be used by TagEditorOverlayHostComponent.
     */
    registerTagEditorOverlayHost(tagEditorOverlayHost: TagEditorOverlayHostComponent): void {
        this.tagEditorOverlayHost = tagEditorOverlayHost;
    }

    /**
     * Unregisters the specified TagEditorOverlayHostComponent.
     * This method should only be used by TagEditorOverlayHostComponent.
     */
    unregisterTagEditorOverlayHost(tagEditorOverlayHost: TagEditorOverlayHostComponent): void {
        if (this.tagEditorOverlayHost === tagEditorOverlayHost) {
            this.tagEditorOverlayHost = null;
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
