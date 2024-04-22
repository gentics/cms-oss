import {
    GcmsUiServices,
    TagEditorContext,
    TagEditorError,
    Translator,
    VariableTagEditorContext,
    TagValidator,
} from '@gentics/cms-integration-api-models';
import { EditableTag, File, Folder, Image, Node, Page, Raw, Template } from '@gentics/cms-models';
import { cloneDeep } from 'lodash-es';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { TagValidatorImpl } from '../../validation/tag-validator-impl';

// Why does this class even exist??? A regular obj with a cloneDeep call would do the same
/** Describes the current context, in which a TagEditor is operating. */
export class TagEditorContextImpl implements TagEditorContext {

    page?: Page<Raw>;
    folder?: Folder<Raw>;
    image?: Image<Raw>;
    file?: File<Raw>;
    node: Node<Raw>;
    template?: Template<Raw>;
    editedTag: EditableTag;
    readOnly: boolean;
    validator: TagValidator;
    sid: number;
    translator: Translator;
    variableContext: Observable<VariableTagEditorContext>;
    gcmsUiServices: GcmsUiServices;
    withDelete: boolean;

    // TODO: Shouldn't be so many params, but use a object instead.
    /**
     * Creates a new controller with a TagEditorContext with the specified objects.
     *
     * @param tag The tag that is being edited.
     * @param readOnly true, if the tag may not be edited, otherwise false.
     * @param tagOwner The page, folder, image, or file to which the tag belongs.
     * @param node The node, from which the tagOwner has been opened.
     * @param sid The current GCMS session ID.
     * @param translator The Translator that should be used by custom TagEditors and TagPropertyEditors for resolving i18n keys.
     * @param variableContext An Observable (ideally a BehaviorSubjet) that provides the VariableTagEditorContext.
     * @param gcmsUiServices Services for opening the repository browser and the image editor.
     * @param withDelete If the tag-editor should allow the user to delete the tag entirely.
     */
    static create(
        tag: EditableTag,
        readOnly: boolean,
        tagOwner: Page<Raw> | Folder<Raw> | Image<Raw> | File<Raw> | Template<Raw>,
        node: Node<Raw>,
        sid: number,
        translator: Translator,
        variableContext: Observable<VariableTagEditorContext>,
        gcmsUiServices: GcmsUiServices,
        withDelete: boolean,
    ): TagEditorContext {
        const context = new TagEditorContextImpl();
        context.editedTag = tag;
        context.readOnly = readOnly;
        context.node = node;
        context.sid = sid;
        context.translator = translator;
        context.validator = new TagValidatorImpl(tag.tagType);
        context.variableContext = variableContext;
        context.gcmsUiServices = gcmsUiServices;
        context.withDelete = withDelete;

        switch (tagOwner.type) {
            case 'page':
                context.page = tagOwner;
                break;
            case 'folder':
                context.folder = tagOwner;
                break;
            case 'image':
                context.image = tagOwner;
                break;
            case 'file':
                context.file = tagOwner;
                break;
            case 'template':
                context.template = tagOwner;
                break;

            default:
                // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
                throw new TagEditorError(`Unknown tag owner type ${tagOwner}`);
        }

        return context;
    }

    private constructor() {}

    clone(): TagEditorContext {
        const clone = new TagEditorContextImpl();
        clone.editedTag = cloneDeep(this.editedTag);
        clone.readOnly = this.readOnly;
        clone.node = cloneDeep(this.node);
        clone.validator = this.validator.clone();
        clone.sid = this.sid;
        clone.translator = this.translator;
        clone.page = cloneDeep(this.page);
        clone.folder = cloneDeep(this.folder);
        clone.image = cloneDeep(this.image);
        clone.file = cloneDeep(this.file);
        clone.template = cloneDeep(this.template);
        clone.gcmsUiServices = { ...this.gcmsUiServices };
        clone.withDelete = this.withDelete;

        clone.variableContext = this.variableContext.pipe(
            map(variableContext => cloneDeep(variableContext)),
        );

        return clone;
    }
}
