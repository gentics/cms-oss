import { Observable } from 'rxjs';

import { File, Folder, Form, Image, Node, Page, Raw, Template } from '../models';
import { ItemInNode, RepositoryBrowserOptions, TagInContainer } from '../repository-browser';
import { EditableTag } from './editable-tag';
import { TagValidator } from './tag-validator';

/** Describes the current context, in which a TagEditor is operating. */
export interface TagEditorContext {

    /** The page, to which the tag belongs (set when editing a content tag or an object property of a page). */
    page?: Page<Raw>;

    /** The folder, to which the tag belongs (set when editing an object property of a folder). */
    folder?: Folder<Raw>;

    /** The form, to which the tag belongs (set when editing an object property of a form). */
    form?: Form<Raw>;

    /** The image, to which the tag belongs (set when editing an object property of an image). */
    image?: Image<Raw>;

    /** The file, to which the tag belongs (set when editing an object property of a file). */
    file?: File<Raw>;

    /** The template, to which the tag belongs to (set when editing an object property or template tag of a template). */
    template?: Template<Raw>;

    /** The node from which the page/folder/image/file, to which the tag belongs, has been opened. */
    node: Node<Raw>;

    /** The Tag being edited. */
    editedTag: EditableTag;

    /** If true, the tag may not be modified - its data should be displayed only. */
    readOnly: boolean;

    /** The TagValidator that can be used to validate the values of the TagProperties. */
    validator: TagValidator;

    /**
     * The ID of the current GCMS REST API session.
     * This is provided in case a custom TagEditor or TagPropertyEditor needs to use the REST API.
     */
    sid: number;

    /**
     * Used in custom TagEditors and custom TagPropertyEditors to obtain translations of
     * i18n keys that come from the GCMS UI.
     */
    translator: Translator;

    /** The parts of the context, which may change while the tag editor is displayed. */
    variableContext: Observable<VariableTagEditorContext>;

    /** Additional services provided by the GCMS UI. */
    gcmsUiServices: GcmsUiServices;

    /**
     * Creates a clone of this TagEditorContext.
     *
     * All properties will be deep copies, except for sid and translator (which are immutable) and variableContext,
     * which will be an observable that is connected to the original observable,
     * such that the cloned context's obervable emits whenever the observable
     * of the original context emits.
     */
    clone(): TagEditorContext;

}

/** Contains the parts of the context, which may change while the tag editor is displayed. */
export interface VariableTagEditorContext {

    /** The current UI language. */
    uiLanguage: string;

}

/**
 * Additional services provided by the GCMS UI.
 * For example, services for opening the repository browser and the image editor.
 */
export interface GcmsUiServices {

    /** Method for opening the Repository Browser. */
    openRepositoryBrowser<R = ItemInNode | TagInContainer>(options: RepositoryBrowserOptions): Promise<R | R[]>;

    /** Method for opening the Image Editor. */
    openImageEditor(options: { nodeId: number, imageId: number }): Promise<Image | void>;

}

/**
 * This should be used in custom TagEditors and custom TagPropertyEditors
 * to obtain translations for i18n keys that come from the GCMS UI.
 */
export interface Translator {

    /**
     * Gets the translated value(s) of the specified i18n key(s) for the currently active UI language.
     * This method works like TranslateService.get() of ngx-translate (https://github.com/ngx-translate/core#methods ).
     *
     * @returns An Observable with the translated value(s). This observable will emit whenever the
     * current languages changes.
     */
    get(key: string | Array<string>, interpolateParams?: object): Observable<string | object>;

    /**
     * Gets the translated value(s) of the specified i18n key(s) for the currently active UI language.
     * This method works like TranslateService.instant() of ngx-translate (https://github.com/ngx-translate/core#methods ).
     *
     * This method only returns the translation for the currently selected language. It is recommended to use
     * the get() method instead, which will return an observable that reacts to language changes.
     */
    instant(key: string | Array<string>, interpolateParams?: object): string | object;

}
