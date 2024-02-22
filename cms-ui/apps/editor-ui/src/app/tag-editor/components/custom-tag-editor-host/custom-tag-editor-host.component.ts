import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import {
    CompleteTagEditor,
    CustomTagEditor,
    EditableTag,
    TagChangedFn,
    TagEditor,
    TagEditorContext,
    TagEditorError,
    TagEditorResult,
    WindowWithCustomTagEditor,
} from '@gentics/cms-models';
import { cloneDeep } from 'lodash-es';

/**
 * The CustomTagEditorHost is used when a TagType has an externalTagEditorUrl configured.
 * This component loads that URL inside an IFrame.
 */
@Component({
    selector: 'custom-tag-editor-host',
    templateUrl: './custom-tag-editor-host.component.html',
    styleUrls: ['./custom-tag-editor-host.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CustomTagEditorHostComponent implements CompleteTagEditor {

    /** The URL of the custom TagEditor. */
    customTagEditorUrl: string;

    /** The height of the IFrame as a CSS height string. */
    iFrameHeight = '48px';

    /** The Tag that is being edited. */
    tag: EditableTag;

    /** The TagEditorContext. */
    context: TagEditorContext;

    /** Set to true if an error message should be shown. */
    showError = false;

    /** The custom TagEditor inside the IFrame. */
    customTagEditor: TagEditor;

    /** The function for reporting changes when editing via editTagLive(). */
    onTagChangeFn: TagChangedFn;

    /** Indicates if the OK and Cancel buttons should be shown if the CustomTagEditor doesn't support `editTag()` */
    showButtons = false;

    /**
     * True if all properties are valid. This is only used when simulating non-live edit mode (i.e., `editTag()`).
     */
    allPropertiesValid: boolean;

    /** The resolution methods of the Promise returned by editTag. */
    private editTagResolve: (result: TagEditorResult) => void;
    private editTagReject: (reason: any) => void;

    constructor(private changeDetector: ChangeDetectorRef) {}

    editTag(tag: EditableTag, context: TagEditorContext): Promise<TagEditorResult> {
        this.onTagChangeFn = null;
        this.startEditingTag(tag, context);
        return new Promise<TagEditorResult>((resolve, reject) => {
            this.editTagResolve = resolve;
            this.editTagReject = reject;
        });
    }

    editTagLive(tag: EditableTag, context: TagEditorContext, onChangeFn: TagChangedFn): void {
        this.onTagChangeFn = onChangeFn;
        this.startEditingTag(tag, context);
    }

    /**
     * Event handler for the IFrame's `load` event.
     */
    onIFrameLoad(iFrame: HTMLIFrameElement): void {
        try {
            const contentWindow: WindowWithCustomTagEditor = iFrame.contentWindow as any;
            const customTagEditor = contentWindow.GcmsCustomTagEditor;
            if (customTagEditor) {
                this.initCustomTagEditor(customTagEditor);
            } else {
                this.showError = true;
                console.error('No CustomTagEditor found in loaded IFrame. ' +
                    'Please make sure that you have set the window.GcmsCustomTagEditor variable to the instance of the custom TagEditor.');
            }
            this.changeDetector.markForCheck();
        } catch (ex) {
            this.showError = true;
            console.error('Error loading custom TagEditor IFrame:');
            console.error(ex);
        }
    }

    onDeleteClick(): void {
        this.editTagResolve({
            doDelete: true,
            tag: this.tag,
        });
    }

    /** Event handler for the Cancel button, which is shown when simulating non-live edit mode (i.e., `editTag()`). */
    onCancelClick(): void {
        this.editTagReject(undefined);
    }

    /** Event handler for the OK button, which is shown when simulating non-live edit mode (i.e., `editTag()`). */
    onOkClick(): void {
        this.editTagResolve({
            doDelete: false,
            tag: this.tag,
        });
    }

    /** Event handler for the close button, which is shown if there is an error loading the custom TagEditor, except in live edit mode. */
    onCloseClick(): void {
        this.editTagReject(undefined);
    }

    /** Stores the tag and context and starts loading the custom tag editor. */
    private startEditingTag(tag: EditableTag, context: TagEditorContext): void {
        this.tag = tag;
        this.context = context;
        this.customTagEditorUrl = tag.tagType.externalEditorUrl;
        this.changeDetector.markForCheck();
    }

    private initCustomTagEditor(customTagEditor: CustomTagEditor): void {
        this.customTagEditor = customTagEditor;
        this.changeDetector.markForCheck();

        // If onTagChangeFn is set, we are in live edit mode.
        if (this.onTagChangeFn) {
            this.startCustomTagEditorEditLive(customTagEditor);
        } else {
            this.startCustomTagEditorEdit(customTagEditor);
        }
        this.changeDetector.markForCheck();

        this.checkIfMethodExists(customTagEditor, 'registerOnSizeChange');
        customTagEditor.registerOnSizeChange(newSize => {
            if (typeof newSize.height === 'number') {
                this.iFrameHeight = `${newSize.height}px`;
            } else {
                this.iFrameHeight = '';
            }
            this.changeDetector.markForCheck();
        });
    }

    /**
     * Starts non-live editing on the CustomTagEditor - `editTag()`.
     *
     * If the CustomTagEditor doesn't support `editTag()`, OK and Cancel
     * buttons are displayed below the IFrame to simulate the `editTag()` behavior.
     */
    private startCustomTagEditorEdit(customTagEditor: CustomTagEditor): void {
        if (customTagEditor.editTag) {
            this.showButtons = false;
            this.startCustomTagEditorEditNatively(customTagEditor);
        } else {
            this.showButtons = true;
            this.simulateCustomTagEditorEdit(customTagEditor);
        }
    }

    private startCustomTagEditorEditNatively(customTagEditor: CustomTagEditor): void {
        customTagEditor.editTag(this.tag, this.context)
            .then(editedTag => {
                // I tried a try catch block here to catch errors thrown by the GCNJS API in case
                // the custom tag editor has made an invalid change, but unfortunately those errors
                // cannot be caught that way. This probably requires a modification of the Aloha gcn-plugin.
                this.editTagResolve(editedTag);
            })
            .catch(reason => {
                if (this.checkIfError(reason)) {
                    console.error(reason);
                }
                this.editTagReject(reason);
            });
    }

    private simulateCustomTagEditorEdit(customTagEditor: CustomTagEditor): void {
        this.checkIfMethodExists(customTagEditor, 'editTagLive');
        const tagClone = cloneDeep(this.tag);
        const validator = this.context.validator.clone();
        this.allPropertiesValid = validator.validateAllTagProperties(this.tag.properties).allPropertiesValid;

        // Whenever the CustomTagEditor reports a change, update this.tag, so that
        // we can use it for resolving the promise, if the user clicks OK.
        customTagEditor.editTagLive(tagClone, this.context, (tagProperties) => {
            this.tag.properties = tagProperties;
            if (tagProperties) {
                this.allPropertiesValid = validator.validateAllTagProperties(tagProperties).allPropertiesValid;
            } else {
                this.allPropertiesValid =  false;
            }
            this.changeDetector.markForCheck();
        });
    }

    /**
     * Starts live editing on the CustomTagEditor - `editTagLive()`.
     */
    private startCustomTagEditorEditLive(customTagEditor: CustomTagEditor): void {
        this.checkIfMethodExists(customTagEditor, 'editTagLive');
        this.showButtons = false;
        const validator = this.context.validator.clone();

        customTagEditor.editTagLive(this.tag, this.context, (tagProperties) => {
            if (tagProperties) {
                // Only pass on the tagProperties if they are all valid,
                // otherwise pass on null.
                const validationResults = validator.validateAllTagProperties(tagProperties);
                if (!validationResults.allPropertiesValid) {
                    tagProperties = null;
                } else {
                    tagProperties = cloneDeep(tagProperties);
                }
            }
            this.onTagChangeFn(tagProperties);
        });
    }

    private checkIfMethodExists(customTagEditor: CustomTagEditor, methodName: string): void {
        if (typeof customTagEditor[methodName] !== 'function') {
            throw new TagEditorError(`window.GcmsCustomTagEditor.${methodName} is not a function. Please make sure that you have implemented the CustomTagEditor interface correctly.`);
        }
    }

    /**
     * Checks if the specified object is an Error object. Since the object comes from
     * the tag editor IFrame we cannot simply use instanceof
     * (see https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/instanceof#instanceof_and_multiple_context_(e.g._frames_or_windows) ).
     * Thus we just check if the prototype of the object contains the string 'Error'.
     */
    private checkIfError(obj: any): boolean {
        if (typeof obj === 'object') {
            const prototype = Object.getPrototypeOf(obj);
            if (prototype && typeof prototype.name === 'string') {
                return (prototype.name as string).indexOf('Error') !== -1;
            }
        }
        return false;
    }

}
