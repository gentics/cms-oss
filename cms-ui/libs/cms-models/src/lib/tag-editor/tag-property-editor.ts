import {TagPart, TagPartProperty, TagPropertyMap} from '../models';
import {CustomEditor} from './custom-editor';
import {EditableTag} from './editable-tag';
import {TagEditorContext} from './tag-editor-context';
import {MultiValidationResult} from './tag-property-validator';

/**
 * The `onChange` function that a `TagPropertyEditor` needs to call when the user changes the `TagProperty` assigned to the editor.
 *
 * In response to user input a `TagPropertyEditor` may also change other TagProperties.
 * All changed TagProperties have to be passed to the onChange function.
 *
 * onChange returns the validation results for the changed TagProperties.
 *
 * onChange must not be called from within `TagPropertyEditor.writeValues()`.
 */
export type TagPropertiesChangedFn = (changes: Partial<TagPropertyMap>) => MultiValidationResult;

/**
 * Base interface for a `TagPropertyEditor` (i.e., a component for editing a single `TagProperty`).
 *
 * The initialization of a `TagPropertyEditor` occurs as follows:
 * 1. `ngOnInit()`
 * 2. `initTagPropertyEditor()`
 * 3. `registerOnChange()`
 *
 * Whenever the user changes the `TagProperty` assigned to this editor (which may cause the editor to change
 * other TagProperties as well), the `TagPropertyEditor` has to call the `TagPropertiesChangedFn` to inform the
 * TagEditor about the changes. The TagEditor will then validate the changes and make the onChange function
 * return the validation results.
 * During the execution of the `onChange` function the TagEditor copies all valid changes to a partial
 * TagPropertyMap and communicates them to all other TagPropertyEditors through their `writeChangedValues()` method.
 * The source `TagPropertyEditor` does not get a `writeChangedValues()` call.
 *
 * The decision for the appropriate times to call the `TagPropertiesChangedFn` is up to each `TagPropertyEditor`,
 * but the `TagPropertiesChangedFn` must be called at the latest when the `TagPropertyEditor` component loses focus.
 *
 * The `TagPropertyEditor` may perform tag validation by itself as well. This must however, not influence the calls
 * to the `TagPropertiesChangedFn`, i.e. user input values that fail validation must also be passed to it, so that
 * the `TagEditor` knows that the currently displayed value is invalid.
 *
 * All objects passed to any of the methods of the `TagPropertyEditor` interface are deep copies created by the `TagEditor`,
 * so they may be safely modified by the `TagPropertyEditor`.
 */
export interface TagPropertyEditor {

    /**
     * Initializes the TagPropertyEditor.
     *
     * This method should perform necessary initialization and store the objects for later reference.
     * At this point the TagPropertyEditor needs to store and process all TagProperties that it is interested in.
     *
     * Since the tag object passed to this method contains all TagProperties as they were received from the CMS,
     * some of the TagProperties may contain values, which would be considered invalid by a TagPropertyValidator
     * (e.g., mandatory properties, which are not set).
     *
     * @param tagPart The part of the tag that this TagPropertyEditor is responsible for.
     * @param tag The Tag that is being edited.
     * @param context The current TagEditorContext.
     */
    initTagPropertyEditor(tagPart: TagPart, tag: EditableTag, tagProperty: TagPartProperty, context: TagEditorContext): void;

    /**
     * Registers the callback that needs to be called whenever the TagPropertyEditor changes any of the TagProperties.
     */
    registerOnChange(fn: TagPropertiesChangedFn): void;

    /**
     * This method is called whenever another TagPropertyEditor changes some TagProperty (by calling the TagPropertiesChangedFn),
     * such that each TagPropertyEditor can see the changes and react if needed. All changes passed to this method have passed
     * validation by a TagPropertyValidator (invalid changes are filtered out by the TagEditor).
     *
     * Important: The TagPropertiesChangedFn must not be called from within the writeValues() method,
     * since two TagPropertyEditors doing this could cause an infinite writeValues() - onChange() - writeValues() - onChange() loop.
     *
     * @param values A map of all TagProperties that were changed and are valid.
     */
    writeChangedValues(values: Partial<TagPropertyMap>): void;

}

/**
 * Base interface for a custom `TagPropertyEditor` (a `TagProperty` editor that is loaded inside an IFrame within the Gentics Tag Editor).
 *
 * By the time the `load` event of the IFrame window fires, the global `window.GcmsCustomTagPropertyEditor` variable
 * has to be set to the instance of the `CustomTagPropertyEditor`.
 *
 * The IFrame element has the `data-gcms-ui-styles` set to a URL from which the basic styles
 * of the GCMS UI can be loaded. The URL can be accessed via `window.frameElement.dataset.gcmsUiStyles`.
 * Loading these styles is optional.
 *
 * See {@link TagPropertyEditor} for the workflow of a TagPropertyEditor.
 */
export interface CustomTagPropertyEditor extends TagPropertyEditor, CustomEditor { }

/**
 * The interface that the window that is hosted inside the IFrame has to implement.
 * By the time the `load` event fires, the global `window.GcmsCustomTagPropertyEditor` variable
 * has to be set to the instance of the `CustomTagPropertyEditor`.
 */
export interface WindowWithCustomTagPropertyEditor {
    /**
     * The `CustomTagPropertyEditor` provided by this window. This must have been set
     * by the time the `load` event fires.
     */
    GcmsCustomTagPropertyEditor: CustomTagPropertyEditor;
}
