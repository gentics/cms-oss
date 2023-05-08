import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import {
    EditableTag,
    FormgeneratorListResponse,
    FormTagPartProperty,
    GtxFormWithUuid,
    TagEditorContext,
    TagEditorError,
    TagPart,
    TagPropertiesChangedFn,
    TagPartProperty,
    TagPropertyEditor,
    TagPropertyMap,
    TagPropertyType,
    ValidationResult,
} from '@gentics/cms-models';
import { isEqual } from 'lodash';
import { Observable, Subject } from 'rxjs';
import { distinctUntilChanged, map, takeUntil } from 'rxjs/operators';
import { FormgeneratorApiService } from '../../../providers/formgenerator-api/formgenerator-api.service';

/**
 * Used to insert forms created with ct-form-generator.
 */
@Component({
    selector: 'formlist-tag-property-editor',
    templateUrl: './formlist-tag-property-editor.component.html',
    styleUrls: ['./formlist-tag-property-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormlistTagPropertyEditor implements TagPropertyEditor, OnDestroy, OnInit {

    forms$: Observable<GtxFormWithUuid[]>;

    forms: GtxFormWithUuid[] = [];

    private destroy$ = new Subject<any>();

    /** The TagPart that the hosted TagPropertyEditor is responsible for. */
    tagPart: TagPart;

    /** The TagProperty that we are editing. */
    tagProperty: FormTagPartProperty;

    /** Whether the tag is opened in read-only mode. */
    readOnly: boolean;

    /** Used for displaying a validation error to the user. */
    displayedValidationResult: ValidationResult;

    /** Used to debounce the input/textarea changes. */
    private inputChange = new Subject<string>();
    private blur = new Subject<string>();

    /** The onChange function registered by the TagEditor. */
    private onChangeFn: TagPropertiesChangedFn;
    private lastValidationResult: ValidationResult;

    constructor(
        private formgeneratorApi: FormgeneratorApiService,
        private changeDetector: ChangeDetectorRef,
    ) { }

    ngOnInit(): void {
        this.forms$ = this.formgeneratorApi.getAllForms().pipe(
            map((formListReponse: FormgeneratorListResponse) => formListReponse.forms),
        );

        this.forms$.pipe(
            takeUntil(this.destroy$),
        ).subscribe(forms => {
            this.forms = forms;
        });

        const debouncer = this.inputChange.debounceTime(100);
        const blurOrDebouncedChange = Observable.merge(this.blur, debouncer).pipe(
            distinctUntilChanged(isEqual),
        );
        blurOrDebouncedChange.pipe(
            takeUntil(this.destroy$),
        ).subscribe(newValue => this.processChange(newValue));
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }

    initTagPropertyEditor(tagPart: TagPart, tag: EditableTag, tagProperty: TagPartProperty, context: TagEditorContext): void {
        this.tagPart = tagPart;
        this.readOnly = context.readOnly;
        this.updateTagProperty(tagProperty);
    }

    registerOnChange(fn: TagPropertiesChangedFn): void {
        this.onChangeFn = fn;
    }

    writeChangedValues(values: Partial<TagPropertyMap>): void {
        // We only care about changes to the TagProperty that this control is responsible for.
        const tagProp = values[this.tagPart.keyword];
        if (tagProp) {
            this.updateTagProperty(tagProp);
        }
    }

    onChange(newValue: string): void {
        if (typeof newValue === 'string') {
            this.inputChange.next(newValue);
        }
    }

    onBlur(newValue: string): void {
        if (typeof newValue === 'string') {
            this.blur.next(newValue);
            this.displayedValidationResult = this.lastValidationResult;
        }
    }

    /**
     * Used to update the currently edited TagProperty with external changes.
     */
    private updateTagProperty(newValue: TagPartProperty): void {
        if (newValue.type !== TagPropertyType.FORM) {
            throw new TagEditorError(`TagPropertyType ${newValue.type} not supported by FormlistTagPropertyEditor.`);
        }
        this.tagProperty = newValue ;
        this.changeDetector.markForCheck();
    }

    /**
     * Signals the change to the TagEditor and also validates the change if the
     * last validation was unsuccessful.
     */
    private processChange(newValue: string): void {
        if (this.onChangeFn) {
            // Signal only the changed tag properties.
            const changes: Partial<TagPropertyMap> = {};
            // TODO: This doesn't make ANY sense
            (this.tagProperty as any).stringValue = newValue;
            changes[this.tagPart.keyword] = this.tagProperty;
            const validationResults = this.onChangeFn(changes);
            this.lastValidationResult = validationResults[this.tagPart.keyword];
        }

        if (this.displayedValidationResult && !this.displayedValidationResult.success && this.lastValidationResult.success) {
            this.displayedValidationResult = this.lastValidationResult;
            this.changeDetector.markForCheck();
        }
    }

}
