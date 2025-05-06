import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CmsFormElementBO, CmsFormElementInsertionInformation, CmsFormElementProperty } from '@gentics/cms-models';
import { GTX_FORM_EDITOR_ANIMATIONS } from '../../animations/form-editor.animations';
import { FormEditorService } from '../../providers';

@Component({
    selector: 'gtx-form-editor-element',
    templateUrl: './form-editor-element.component.html',
    styleUrls: ['./form-editor-element.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: GTX_FORM_EDITOR_ANIMATIONS,
})
export class FormEditorElementComponent implements OnChanges {

    @Input()
    public readonly: boolean;

    @Input()
    element: CmsFormElementBO;

    @Output()
    elementChange: EventEmitter<CmsFormElementBO> = new EventEmitter<CmsFormElementBO>();

    @Input()
    isPreview: boolean;

    /** Current UI language. */
    @Input()
    set activeUiLanguageCode(v: string) {
        this.formEditorService.activeUiLanguageCode = v;
    }
    get activeUiLanguageCode(): string {
        return this.formEditorService.activeUiLanguageCode;
    }

    /** Current content language. */
    @Input()
    set activeContentLanguageCode(v: string) {
        this.formEditorService.activeContentLanguageCode = v;
    }
    get activeContentLanguageCode(): string {
        return this.formEditorService.activeContentLanguageCode;
    }

    @Input()
    hideDropZones = true;

    /** Element exists within form editor and will try to render content properties, e. g. label. */
    @Input()
    isInEditor = false;

    @Output()
    remove = new EventEmitter<CmsFormElementBO>();

    @Output()
    openPropertiedEditor = new EventEmitter<boolean>();

    @Output()
    beforeFormElementInsert = new EventEmitter<CmsFormElementInsertionInformation>();

    /**
     * Emits when the properties error status changes
     */
    @Output()
    propertiesErrorChange = new EventEmitter<boolean>();

    /** Element is in Drag state. */
    isBeingDragged: boolean;
    /** Element is in Drop-preview state. */
    onDropPreview: boolean;
    /** Element's properties menu is visible. */
    propertiesEditorIsOpen: boolean;
    /** If TRUE, element will visually indicate that one of element properties has value set in another language than current. */
    isUntranslated: boolean;
    /** If TRUE, some requirement or validation constraint is unfulfilled in the current language. This will be reflected in the element. */
    containsPropertyError: boolean;

    propertiesContainError = false;

    private childElementsContainPropertiesError = false;

    constructor(
        private changeDetectorRef: ChangeDetectorRef,
        private formEditorService: FormEditorService,
    ) { }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.readonly) {
            if (this.readonly) {
                this.propertiesEditorIsOpen = false;
            }
        }
    }

    toggleActiveClicked(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        this.element.active = !this.element.active;
        this.elementChange.emit(this.element)
    }

    removeElementClicked(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        this.remove.emit(this.element);
    }

    containerClicked(event: MouseEvent): void {
        if (!this.isInEditor || this.readonly || !this.element.properties || this.element.properties.length === 0) {
            return;
        }

        this.propertiesEditorIsOpen = !this.propertiesEditorIsOpen;
        // notify parent
        this.openPropertiedEditor.emit(this.propertiesEditorIsOpen);
    }

    onTranslationErrorChange(v: boolean): void {
        this.isUntranslated = v;
        this.changeDetectorRef.detectChanges();
    }

    onRequiredOrValidationErrorStatus(v: boolean): void {
        this.propertiesContainError = v;
        this.updatePropertiesError();
        this.changeDetectorRef.detectChanges();
    }

    onFormElementInsert(insertionInformation: CmsFormElementInsertionInformation): void {
        this.beforeFormElementInsert.emit(insertionInformation);
    }

    public onElementsChange(elements: CmsFormElementBO[]): void {
        if (this.element) {
            this.element.elements = elements;
        }
        this.elementChange.emit(this.element);
    }

    public onPropertiesChange(properties: CmsFormElementProperty[]): void {
        if (this.element) {
            this.element.properties = properties;
        }
        this.elementChange.emit(this.element);
    }

    onPropertiesErrorChange(propertiesError: boolean): void {
        this.childElementsContainPropertiesError = propertiesError;
        this.updatePropertiesError();
    }

    private updatePropertiesError(): void {
        const containsPropertiesError = this.propertiesContainError || this.childElementsContainPropertiesError;
        this.containsPropertyError = containsPropertiesError;
        this.propertiesErrorChange.emit(containsPropertiesError);
    }

    // onMouseDown(event: MouseEvent): void {
    //     const allowedCssClasses = [
    //         'form-element-container-header',
    //         'form-element-name',
    //     ];
    //     const el = event.target as HTMLElement;
    //     this.dragIsAllowed = el && allowedCssClasses.some(c => el.classList.contains(c));
    // }

    onDragStart(event: DragEvent): void {
        this.isBeingDragged = true;
    }

    onDragEnd(): void {
        this.isBeingDragged = false;
    }

    disableDragEvent(event: DragEvent): void {
        event.preventDefault();
        event.stopPropagation();
    }

}
