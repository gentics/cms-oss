import { AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, QueryList, ViewChildren } from '@angular/core';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import {
    CompleteTagEditor,
    MultiValidationResult,
    TagChangedFn,
    TagEditorContext,
    TagEditorError,
    TagEditorResult,
    TagPropertyEditor,
    ValidationResult,
} from '@gentics/cms-integration-api-models';
import {
    EditableTag,
    TagPart,
    TagPropertyMap,
    findTagPart,
} from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { cloneDeep, isEqual } from 'lodash-es';
import { Subscription } from 'rxjs';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { TagPropertyEditorHostComponent } from '../tag-property-editor-host/tag-property-editor-host.component';

/**
 * The GenticsTagEditor is the default TagEditor and uses the Gentics TagPropertyEditors
 * for editing the TagProperties, but also allows each tag part to have a CustomTagPropertyEditor
 * configured, which will be loaded in an IFrame.
 */
@Component({
    selector: 'gentics-tag-editor',
    templateUrl: './gentics-tag-editor.component.html',
    styleUrls: ['./gentics-tag-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GenticsTagEditorComponent implements CompleteTagEditor, AfterViewInit, OnDestroy {

    @Input()
    public showTitle = true;

    /**
     * The original EditableTag object passed to the editTag() method.
     * This object is not intended to be modified.
     * TagPropertyEditors get a deep copy of this object.
     */
    originalTag: EditableTag;

    /**
     * The function for reporting changes when editing via editTagLive().
     */
    onTagChangeFn: TagChangedFn;

    /**
     * True if all properties are valid.
     * A mandatory property is valid if it is set and has a valid value.
     * A non-mandatory property is valid if it either has a valid value or if it is not set.
     */
    allPropertiesValid: boolean;

    /** The editable (and non hidden in editor) parts of the tag (used to create the TagPropertyEditorHosts). */
    editableTagParts: TagPart[];

    @ViewChildren(TagPropertyEditorHostComponent)
    propertyEditorHosts: QueryList<TagPropertyEditorHostComponent>;

    /**
     * Used to make sure that improperly written TagPropertyEditors don't
     * cause an infinite onTagPropertyChanged() loop.
     */
    private onTagPropertyChangeAllowed = true;

    /**
     * The current state of the tag properties.
     * TagPropertyEditors are only passed deep copies of this map (or its parts).
     */
    private currentTagState: TagPropertyMap;

    /**
     * The current TagEditorContext.
     * TagPropertyEditors get a deep copy of this object.
     */
    public context: TagEditorContext;

    /**
     * Contains all TagPropertyEditors used by this TagEditor.
     */
    private tagPropertyEditors: TagPropertyEditor[] = [];

    /**
     * Used to look up the latest validation results for each TagPart.keyword.
     */
    private latestValidationResults: Map<string, ValidationResult>;

    private subscriptions = new Subscription();

    private editResolve: (result: TagEditorResult) => void;
    private editReject: () => void;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private errorHandler: ErrorHandler,
        private modalService: ModalService,
        private i18n: I18nService,
    ) {}

    ngAfterViewInit(): void {
        this.subscriptions.add(
            this.propertyEditorHosts.changes.subscribe((newPropEditorHosts: QueryList<TagPropertyEditorHostComponent>) => {
                this.tagPropertyEditors = newPropEditorHosts.map(propEditorHost => propEditorHost.tagPropertyEditor);
                this.executeSafely(() => this.setUpPropertyEditors());
            }),
        );
        this.propertyEditorHosts.notifyOnChanges();
    }

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }

    editTag(tag: EditableTag, context: TagEditorContext): Promise<TagEditorResult> {
        this.onTagChangeFn = null;
        this.initTagEditor(tag, context);

        return new Promise<TagEditorResult>((resolveFn, rejectFn) => {
            this.editResolve = resolveFn;
            this.editReject = rejectFn;
        });
    }

    editTagLive(tag: EditableTag, context: TagEditorContext, onChangeFn: TagChangedFn): void {
        this.onTagChangeFn = onChangeFn;
        this.initTagEditor(tag, context);
    }

    onDeleteClick(): void {
        this.modalService.dialog({
            title: this.i18n.translate('modal.confirmation_tag_delete_singular_title'),
            body: this.i18n.translate('modal.delete_tag_confirm_singular', { name: this.originalTag.name }),
            buttons: [
                {
                    label: this.i18n.translate('common.cancel_button'),
                    type: 'secondary',
                    flat: true,
                    returnValue: false,
                    shouldReject: true,
                },
                {
                    label: this.i18n.translate('common.delete_button'),
                    type: 'alert',
                    returnValue: true,
                },
            ],
        })
            .then(modal => modal.open())
            .then(shouldContinue => {
                if (!shouldContinue) {
                    return;
                }

                this.editResolve({
                    doDelete: true,
                    tag: this.originalTag,
                });
            });

    }

    onCancelClick(): void {
        this.editReject();
    }

    onOkClick(): void {
        const editedTag = cloneDeep(this.originalTag);
        editedTag.properties = cloneDeep(this.currentTagState);
        this.editResolve({
            doDelete: false,
            tag: editedTag,
        });
    }

    private initTagEditor(tag: EditableTag, context: TagEditorContext): void {
        this.originalTag = tag;
        this.currentTagState = cloneDeep(tag.properties);
        this.context = context;
        this.editableTagParts = this.originalTag.tagType.parts
            .filter(tagPart => tagPart.editable && !tagPart.hideInEditor);
        this.changeDetector.markForCheck();
    }

    private setUpPropertyEditors(): void {
        if (this.tagPropertyEditors.length === 0) {
            return;
        }

        this.checkInitialTagPropertyValues();
        this.onTagPropertyChangeAllowed = false;
        for (let i = 0; i < this.tagPropertyEditors.length; ++i) {
            const editor = this.tagPropertyEditors[i];
            this.setUpPropertyEditor(editor, i);
        }
        this.onTagPropertyChangeAllowed = true;
        this.changeDetector.detectChanges();
    }

    private setUpPropertyEditor(editor: TagPropertyEditor, index: number): void {
        // If there is no editor component for the respective TagPartType it should not break the TagEditor.
        if (!editor) {
            return;
        }

        // Clone the originalTag and make sure that the tagProperty and the tagPart
        // that the TagPropertyEditor gets are part of that clone.
        const tag = cloneDeep(this.originalTag);
        const tagProperty = tag.properties[this.editableTagParts[index].keyword];
        const tagPart = findTagPart(tagProperty, tag.tagType);
        const context = this.context.clone();

        editor.initTagPropertyEditor(tagPart, tag, tagProperty, context);
        editor.registerOnChange(changes => this.onTagPropertyChange(changes, editor));
    }

    /**
     * Handles the TagPropertiesChangedFn calls made by the TagPropertyEditors.
     */
    private onTagPropertyChange(changes: Partial<TagPropertyMap>, src: TagPropertyEditor): MultiValidationResult {
        // Make sure that improperly written TagPropertyEditors cannot cause
        // an infinite onTagPropertyChange() loop.
        if (!this.onTagPropertyChangeAllowed) {
            throw new TagEditorError(
                `writeChangedValues() - onChange() loop detected. This means that two or more TagPropertyEditor \\
                components are causing each other to repeatedly call the TagPropertiesChangedFn or that \\
                the TagPropertiesChangedFn is called from the TagPropertyEditor.registerOnChange() method. \\
                DO NOT call the TagPropertiesChangedFn in the TagPropertyEditor.writeChangedValues() or the \\
                TagPropertyEditor.registerOnChange() method.`,
            );
        }
        this.onTagPropertyChangeAllowed = false;

        changes = cloneDeep(changes);
        const validationResults = this.validateChangesAndUpdateTagState(changes);
        this.executeSafely(() =>
            this.propagateChangesToPropertyEditors(changes, src),
        );

        this.allPropertiesValid = this.checkIfAllPropertiesAreValid();
        this.onTagPropertyChangeAllowed = true;
        this.changeDetector.markForCheck();

        if (this.onTagChangeFn && Object.keys(validationResults).length > 0) {
            // If the current state is invalid, we report null.
            const currentState = this.allPropertiesValid ? this.currentTagState : null;
            this.onTagChangeFn(cloneDeep(currentState));
        }

        return validationResults;
    }

    /**
     * Validates the specified changes, deleting invalid changes, and updates
     * the currentTagState with the valid changes and latestValidationResults with all validation results.
     *
     * @param changes The changes to be validated. Invalid changes will be deleted from this object.
     */
    private validateChangesAndUpdateTagState(changes: Partial<TagPropertyMap>): MultiValidationResult {
        const validationResults: MultiValidationResult = {};

        for (const key of Object.getOwnPropertyNames(changes)) {
            if (!this.originalTag.properties[key]) {
                console.error('TagPropertyEditor tried to modify a TagProperty, which does not exist: ' + key);
                continue;
            }

            const changedProp = changes[key];
            if (!findTagPart(changedProp, this.originalTag.tagType).editable) {
                throw new TagEditorError('TagPropertyEditor tried to change the non-editable TagProperty: ' + key);
            }

            // If there is no actual change, skip this property.
            // This can happen if the TagPropertyEditor reported the current value as a change,
            // e.g., because the an input field signaled a change due to loss of focus, even though the value
            // remained the same.
            if (isEqual(this.currentTagState[key], changedProp)) {
                delete changes[key];
                // Only skip through if the previous check was success, otherwise it can lead to an edge-case
                // when the previous version was invalid and the previous correct version is used the validation
                // will not triggered again.
                const lastVerifiedValidation = this.latestValidationResults.get(key);
                if (lastVerifiedValidation && lastVerifiedValidation.success) {
                    continue;
                }
            }

            const validationResult = this.context.validator.validateTagProperty(changedProp);
            validationResults[key] = validationResult;

            if (validationResult.success) {
                this.currentTagState[key] = changedProp;
            } else {
                delete changes[key];
            }
            this.latestValidationResults.set(key, validationResult);
        }

        return validationResults;
    }

    /**
     * Propagates deep clones of the specified changes to each TagPropertyEditor, except src.
     * If the changes object is empty, nothing is propagated.
     */
    private propagateChangesToPropertyEditors(changes: Partial<TagPropertyMap>, src: TagPropertyEditor): void {
        if (Object.keys(changes).length === 0) {
            return;
        }
        for (const editor of this.tagPropertyEditors) {
            if (editor && editor !== src) {
                editor.writeChangedValues(cloneDeep(changes));
            }
        }
    }

    /**
     * Returns true if all properties are valid.
     */
    private checkIfAllPropertiesAreValid(): boolean {
        for (const tagPart of this.editableTagParts) {
            const validationResult = this.latestValidationResults.get(tagPart.keyword);
            if (!validationResult.success) {
                return false;
            }
        }
        return true;
    }

    private checkInitialTagPropertyValues(): void {
        this.latestValidationResults = new Map();
        for (const tagPart of this.editableTagParts) {
            const validationResult = this.context.validator.validateTagProperty(this.currentTagState[tagPart.keyword]);
            this.latestValidationResults.set(tagPart.keyword, validationResult);
        }
        this.allPropertiesValid = this.checkIfAllPropertiesAreValid();
    }

    /**
     * Executes the specified function, catching all exceptions
     * thrown during its execution and passing them to the errorHandler.
     *
     * @returns true if the function was executed successfully, false if it threw an exception
     */
    private executeSafely(fn: () => void): boolean {
        try {
            fn();
            return true;
        } catch (error) {
            this.errorHandler.catch(error, { notification: true });
            return false;
        }
    }

}
