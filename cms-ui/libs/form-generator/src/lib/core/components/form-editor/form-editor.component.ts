import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild,
} from '@angular/core';
import {
    CmsFormElementBO,
    CmsFormElementInsertionInformation,
    CmsFormElementInsertionType,
    CmsFormType,
    EditMode,
    Form,
    FormBO,
    Normalized,
    Raw,
} from '@gentics/cms-models';
import { cloneDeep as _cloneDeep } from'lodash-es'
import { Subject } from 'rxjs';
import { FormEditorConfiguration } from '../../../common/models/form-editor-configuration';
import { GTX_FORM_EDITOR_ANIMATIONS } from '../../animations/form-editor.animations';
import { FormEditorConfigurationService, FormEditorService } from '../../providers';
import { FormEditorMappingService } from '../../providers/form-editor-mapping/form-editor-mapping.service';
import { FormEditorElementListComponent } from '../form-editor-element-list/form-editor-element-list.component';

@Component({
    selector: 'gtx-form-editor',
    templateUrl: './form-editor.component.html',
    styleUrls: ['./form-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: GTX_FORM_EDITOR_ANIMATIONS,
})
export class FormEditorComponent implements OnInit, OnDestroy {

    private mappedForm: FormBO<Raw | Normalized>;

    private configurationFetched = false;

    get form(): Form<Raw | Normalized> {
        return this.formEditorMappingService.mapFormBOToForm(this.mappedForm);
    }

    @Input() set form(value: Form) {
        this.formEditorConfigurationService.getConfiguration$(
            value.data.type ? value.data.type : CmsFormType.GENERIC,
        ).subscribe((configuration: FormEditorConfiguration) => {
            this.configurationFetched = true;
            this.mappedForm = this.formEditorMappingService.mapFormToFormBO(value, configuration);
            this.onFormUpdated();
            this.changeDetectorRef.markForCheck();
            this.setFormEditorMenu();
        });
    }

    get formElements(): CmsFormElementBO[] {
        if (this.mappedForm && this.mappedForm.data) {
            return this.mappedForm.data.elements;
        } else {
            return undefined;
        }
    }

    get formType(): CmsFormType {
        if (this.mappedForm && this.mappedForm.data) {
            return this.mappedForm.data.type;
        } else {
            return undefined;
        }
    }

    @Input()
    isDisabled = false;

    @Input()
    set formEditMode(v: EditMode) {
        this.internalFormEditMode = v;
        if (v === 'preview') {
            this.menuVisible = false;
        } else if (v === 'edit') {
            if (this.configurationFetched) {
                this.menuVisible = true;
            }
        }
    }

    get formEditMode(): EditMode {
        return this.internalFormEditMode;
    }
    private internalFormEditMode: EditMode = EditMode.PREVIEW;

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

    @Output()
    formModified = new EventEmitter<Form<Raw | Normalized>>();

    /**
     * Emits when the properties error status changes
     */
    @Output()
    validityChange = new EventEmitter<boolean>();


    @ViewChild('formBackground', { static: false })
    formBackground: ElementRef<HTMLElement>;

    @ViewChild('formEditorElementListComponent', { static: false })
    formEditorElementList: FormEditorElementListComponent;

    menuVisible = false;

    /** If any element is currrently being dragged by user. */
    elementBeingDragged: boolean;

    /** Current item's index user is dragging over. */
    dragIndex: number;

    /** Index of an element's former position after being dragged to antoher position. */
    replaceElementIndex: number;

    /** If drag index is 0 and user drags over first element in list. */
    aboveFirstElement: boolean;

    /** Element's index a user's mouse is hovering. */
    formElementHoverIndex: number;

    /** Index indicating whether user has opened element menu at top or bottm. */
    btnAdderHoverIndex: number;

    /** FOR TESTING: if TRUE do not dnmaically render input preview. */
    isPreview = true;

    /** If an "adder" button is clicked at a form element, this is the element's index. */
    adderPositionIndex: number;

    /** Map of open element's properties menu is visible. */
    propertiesEditorOpenMap: boolean[] = [];

    /** To compare if changed. */
    private formMemory: string;

    private destroyed$ = new Subject<void>();

    private configuration: FormEditorConfiguration;

    private updateFormInputWithNewConfiguration: (configuration: FormEditorConfiguration) => void;

    dragEnterDepth = 0;

    constructor(
        private formEditorConfigurationService: FormEditorConfigurationService,
        private formEditorMappingService: FormEditorMappingService,
        private formEditorService: FormEditorService,
        private changeDetectorRef: ChangeDetectorRef,
    ) { }

    ngOnInit(): void { }

    setFormEditorMenu(): void {
        if (this.formEditorIsReadOnly()) {
            return;
        }
        this.menuVisible = true;
        this.changeDetectorRef.markForCheck();
    }

    private onFormUpdated(): void {
        this.mappedForm = _cloneDeep(this.mappedForm);
        this.formMemory = JSON.stringify(this.mappedForm);
        this.formEditorService.formLanguages = this.mappedForm.languages;
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    identify(index: number, element: CmsFormElementBO): string {
        return element.globalId;
    }

    propertiesEditorOpen(): boolean {
        return this.propertiesEditorOpenMap.some(e => e);
    }

    formEditorIsReadOnly(): boolean {
        return this.formEditMode !== 'edit';
    }

    public onElementsChange(elements: CmsFormElementBO[]): void {
        if (this.mappedForm && this.mappedForm.data) {
            this.mappedForm.data.elements = elements;
        }
        if (this.formMemory !== JSON.stringify(this.mappedForm)) {
            this.formModified.emit(this.form);
        }
    }

    onFormElementInsert(insertionInformation: CmsFormElementInsertionInformation): void {
        if (insertionInformation.insertionType === CmsFormElementInsertionType.MOVE) {
            this.removeElementByName(this.mappedForm.data.elements, insertionInformation.element.name)
        }
    }

    /**
     * Finds an element with a matching name in the whole form and removes it.
     * This has to be done, in order to safely remove the dragged element at the original position before adding it at its target position.
     * If we were to use the dragend event, we could never be sure whether it will be triggered on the original or new element due to
     * the way Angular tracks identity (this can be a problem when moving an element within a list).
     *
     * Furthermore, we do not want an element to be removed when it is dragged outside of the form editor and
     * we do want to remove elements with the same name as elements dragged into the form editor (e.g. from another browser). This approach
     * allows us to do that.
     */
    private removeElementByName(elements: CmsFormElementBO[], name: string): void {
        for (let i = 0; i < elements.length; i++) {
            const element: CmsFormElementBO = elements[i];
            if (element.name === name) {
                elements.splice(i, 1);
                this.changeDetectorRef.detectChanges();
                break;
            } else if (element.elements && element.elements.length > 0) {
                this.removeElementByName(element.elements, name);
            }
        }
    }

    onMenuSelect(element: CmsFormElementBO): void {
        if (!element) {
            return;
        }
        this.formEditorElementList.addElementFromMenu(element);
        this.formModified.emit(this.form);
        this.changeDetectorRef.markForCheck();
    }

    onPropertiesErrorChange(propertiesError: boolean): void {
        this.validityChange.emit(!propertiesError);
    }


    /**
     * In order to hide drop zones in case they must not accept the currently dragged element, the item type has to be checked here.
     * This is not done as of yet, to avoid having two different places checking the type (possibly differently).
     */

    onDragEnter(event: DragEvent): void {
        this.dragEnterDepth++;
        event.preventDefault();
        event.stopPropagation();
    }

    onDragOver(event: DragEvent): void {
        event.preventDefault();
        event.stopPropagation();
    }

    onDragLeave(event: DragEvent): void {
        this.dragEnterDepth--;
        event.preventDefault();
        event.stopPropagation();
    }

    onDrop(event: DragEvent): void {
        this.dragEnterDepth = 0;
        event.preventDefault();
        event.stopPropagation();
    }
}
