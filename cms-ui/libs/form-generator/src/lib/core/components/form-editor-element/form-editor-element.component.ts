import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CmsFormElementBO, CmsFormElementInsertionInformation, CmsFormElementProperty, EditMode } from '@gentics/cms-models';
import { GTX_FORM_EDITOR_ANIMATIONS } from '../../animations/form-editor.animations';
import { FormEditorService } from '../../providers';

export interface FormEditorElementState {
    /** Element is in Drag state. */
    isBeingDragged: boolean;
    /** Element is in Drop-preview state. */
    onDropPreview: boolean;
    /** Element's properties menu is visible. */
    propertiesEditorOpen: boolean;
    /** Element exists within form and writable. */
    readOnly: boolean;
    /** Element exists within form editor and will try to render content properties, e. g. label. */
    isInEditor: boolean;
    /** If TRUE, element will show visual indicators of being interactive, e. g. has hover efect. */
    isInteractive: boolean;
    /** If TRUE, element will visually indicate that one of element properties has value set in another language than current. */
    isUntranslated: boolean;
    /** If TRUE, some requirement or validation constraint is unfulfilled in the current language. This will be reflected in the element. */
    containsPropertyError: boolean;
}

@Component({
    selector: 'gtx-form-editor-element',
    templateUrl: './form-editor-element.component.html',
    styleUrls: ['./form-editor-element.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: GTX_FORM_EDITOR_ANIMATIONS,
})
export class FormEditorElementComponent implements OnChanges {

    state: FormEditorElementState = {
        isBeingDragged: false,
        onDropPreview: false,
        propertiesEditorOpen: false,
        readOnly: false,
        isInEditor: false,
        isInteractive: false,
        isUntranslated: false,
        containsPropertyError: false,
    };

    public _formEditMode;

    @Input()
    set formEditMode(v: EditMode) {
        switch (v) {
            case 'preview':
                // close properties editor in case it is opened
                this.state.propertiesEditorOpen = false;
                this.state.readOnly = true;
                this.state.isInteractive = false;
                break;

            case 'edit':
                this.state.isInteractive = true;
                this.state.readOnly = false;
                break;

            default:
                break;
        }
        this._formEditMode = v;
    }
    @Input()
    set isInEditor(v: boolean) {
        this.state.isInEditor = v;
    }

    @Input()
    set isInteractive(v: boolean) {
        this.state.isInteractive = v;
    }

    @Input()
    set isBeingDragged(v: boolean) {
        this.state.isBeingDragged = v;
    }

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

    @Output()
    remove = new EventEmitter<CmsFormElementBO>();

    @Output()
    propertiesEditorOpen = new EventEmitter<boolean>();

    @Output()
    beforeFormElementInsert = new EventEmitter<CmsFormElementInsertionInformation>();

    /**
     * Emits when the properties error status changes
     */
    @Output()
    propertiesErrorChange = new EventEmitter<boolean>();

    propertiesContainError = false;
    private childElementsContainPropertiesError = false;

    constructor(
        private changeDetectorRef: ChangeDetectorRef,
        private formEditorService: FormEditorService,
    ) { }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.formEditMode && changes.formEditMode.previousValue !== changes.formEditMode.currentValue) {
            this.changeDetectorRef.detectChanges();
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
        if (this.state.readOnly || !this.element.properties || this.element.properties.length === 0) {
            return;
        }

        this.state.propertiesEditorOpen = !this.state.propertiesEditorOpen;
        // notify parent
        this.propertiesEditorOpen.emit(this.state.propertiesEditorOpen);
    }

    onTranslationErrorChange(v: boolean): void {
        this.state.isUntranslated = v;
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
        this.state.containsPropertyError = containsPropertiesError;
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
