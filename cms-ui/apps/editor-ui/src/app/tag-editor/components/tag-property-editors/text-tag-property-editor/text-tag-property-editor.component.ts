import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { TagEditorContext, TagEditorError, TagPropertiesChangedFn, TagPropertyEditor, ValidationResult } from '@gentics/cms-integration-api-models';
import {
    EditableTag,
    StringTagPartProperty,
    TagPart,
    TagPartProperty,
    TagPartType,
    TagPropertyMap,
    TagPropertyType,
} from '@gentics/cms-models';
import { createRegexValidator } from '@gentics/ui-core';
import { distinctUntilChanged, Subscription } from 'rxjs';

/**
 * Used to edit Text TagParts.
 */
@Component({
    selector: 'text-tag-property-editor',
    templateUrl: './text-tag-property-editor.component.html',
    styleUrls: ['./text-tag-property-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class TextTagPropertyEditor implements TagPropertyEditor, OnDestroy {

    /** The TagPart that the hosted TagPropertyEditor is responsible for. */
    tagPart: TagPart;

    /** Used to determine if we need to show a one line input or a multiline text area. */
    isTextShort: boolean;

    control: FormControl<string>;

    /** Used for displaying a validation error to the user. */
    validationResult: ValidationResult;

    /** Used to debounce the input/textarea changes. */
    private controlSub: Subscription;
    private subscriptions: Subscription[] = [];

    /** The onChange function registered by the TagEditor. */
    private onChangeFn: TagPropertiesChangedFn;

    constructor(
        private changeDetector: ChangeDetectorRef,
    ) { }

    ngOnDestroy(): void {
        this.subscriptions.forEach((s) => s.unsubscribe());
    }

    initTagPropertyEditor(tagPart: TagPart, tag: EditableTag, tagProperty: TagPartProperty, context: TagEditorContext): void {
        this.tagPart = tagPart;
        this.isTextShort = tagPart.typeId === TagPartType.TextShort;

        if (this.controlSub != null) {
            this.controlSub.unsubscribe();
        }

        if (tagProperty.type !== TagPropertyType.STRING && tagProperty.type !== TagPropertyType.RICHTEXT) {
            throw new TagEditorError(`TagPropertyType ${tagProperty.type} not supported by TextTagPropertyEditor.`);
        }

        this.control = new FormControl({
            value: tagProperty?.stringValue || '',
            disabled: context.readOnly,
        }, () => {
            if (this.validationResult != null && this.validationResult.isSet && !this.validationResult.success) {
                return { tagEditorValidation: this.validationResult.errorMessage };
            }
            return null;
        });
        if (tagPart.mandatory) {
            this.control.addValidators(Validators.required);
        }
        if (tagPart.regex?.expression) {
            this.control.addValidators(createRegexValidator(tagPart.regex.expression, [
                tagPart.regex.description,
            ]));
        }
        this.controlSub = this.control.valueChanges.pipe(
            distinctUntilChanged(),
        ).subscribe(() => {
            if (this.onChangeFn) {
                // Signal only the changed tag properties.
                const changes: Partial<TagPropertyMap> = {};
                changes[this.tagPart.keyword] = {
                    ...tagProperty,
                    stringValue: this.control.value,
                };
                const validationResults = this.onChangeFn(changes) || {};
                this.validationResult = validationResults[this.tagPart.keyword];
                this.control.updateValueAndValidity();
            }

            this.changeDetector.markForCheck();
        });
        this.subscriptions.push(this.controlSub);
        this.control.updateValueAndValidity();
    }

    registerOnChange(fn: TagPropertiesChangedFn): void {
        this.onChangeFn = fn;
    }

    writeChangedValues(values: Partial<TagPropertyMap>): void {
        // We only care about changes to the TagProperty that this control is responsible for.
        const tagProp = values[this.tagPart.keyword];
        if (tagProp) {
            this.control.setValue((tagProp as StringTagPartProperty).stringValue, { emitEvent: false });
        }
    }
}
