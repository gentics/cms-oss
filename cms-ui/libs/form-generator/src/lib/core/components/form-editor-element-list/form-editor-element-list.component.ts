import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    Output,
    QueryList,
    ViewChildren,
} from '@angular/core';
import {
    CmsFormElementBO,
    CmsFormElementInsertionInformation,
    CmsFormElementInsertionType,
    FORM_ELEMENT_MIME_TYPE_TYPE,
    FormElementDropInformation,
} from '@gentics/cms-models';
import { cloneDeep as _cloneDeep } from 'lodash-es';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { newUUID } from '../../../common';
import { GTX_FORM_EDITOR_ANIMATIONS } from '../../animations/form-editor.animations';
import { FormEditorService } from '../../providers';
import { FormEditorElementComponent } from '../form-editor-element/form-editor-element.component';

@Component({
    selector: 'gtx-form-editor-element-list',
    templateUrl: './form-editor-element-list.component.html',
    styleUrls: ['./form-editor-element-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: GTX_FORM_EDITOR_ANIMATIONS,
})
export class FormEditorElementListComponent
    implements AfterViewInit, OnDestroy {

    /** Unique ID. If not ROOT, it is globaldId of container element. */
    @Input()
    containerId = 'ROOT';

    @Input()
    elements: CmsFormElementBO[];

    @Input()
    readonly: boolean;

    @Output()
    elementsChange = new EventEmitter<CmsFormElementBO[]>();


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

    /**
     * a list of elements that cannot be dropped in drop zones that are direct children of this element list
     */
    @Input()
    cannotReceive: string[] = [];

    // @Output()
    // formModified = new EventEmitter<Form<Raw | Normalized>>();

    @Output()
    beforeFormElementInsert = new EventEmitter<CmsFormElementInsertionInformation>();

    /**
     * Emits when the properties error status changes
     */
    @Output()
    propertiesErrorChange = new EventEmitter<boolean>();

    // @ViewChild('formBackground', { static: false })
    // formBackground: ElementRef<HTMLElement>;

    // menuPositionX = 0;
    // menuPositionY = 0;

    /** Index indicating whether user has opened element menu at top or bottm. */
    btnAdderHoverIndex: number;

    /** FOR TESTING: if TRUE do not dnmaically render input preview. */
    isPreview = true;

    /** Map of open element's properties menu is visible. */
    propertiesEditorOpenMap: Record<number, boolean> = {};

    private destroyed$ = new Subject<void>();

    @ViewChildren(FormEditorElementComponent)
    formElementComponents!: QueryList<FormEditorElementComponent>;

    constructor(
        private formEditorService: FormEditorService,
        private changeDetectorRef: ChangeDetectorRef,
    ) {}

    ngAfterViewInit(): void {
        this.formElementComponents.changes
            .pipe(takeUntil(this.destroyed$))
            .subscribe(() => {
                this.updatePropertiesError();
            });
        this.updatePropertiesError();
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    identify(index: number, element: CmsFormElementBO): string {
        return `${element.globalId}`;
    }

    onElementRemove(index: number): void {
        this.removeElementAtIndex(index);
        this.onElementsChange();
    }

    // TODO: redo functionality for MENU
    onElementDragStart(
        eventData: { event: DragEvent; element: CmsFormElementBO },
        index?: number,
    ): void {
        eventData.event.stopPropagation();
        if (this.readonly || !eventData || !eventData.element) {
            return;
        }
        eventData.event.dataTransfer.setData(
            `${FORM_ELEMENT_MIME_TYPE_TYPE}/${eventData.element.type}`,
            JSON.stringify({
                element: eventData.element,
                formId: '',
            }),
        );
        // items could also be dropped outside of the Editor UI to create a JSON copy. For this, we would have to add fallback types (without form id).
        // eventData.event.dataTransfer.setData('application/json', JSON.stringify(eventData.element));
        // eventData.event.dataTransfer.setData('text/plain', JSON.stringify(eventData.element));
        eventData.event.dataTransfer.effectAllowed = 'copyMove';
    }

    onElementDropped(
        dropInformation: FormElementDropInformation,
        index: number,
        aboveFirst: boolean,
    ): void {
        let wasInSameParentContainerAtLowerIndex = false;
        for (let i = 0; i < this.elements.length && i <= index; i++) {
            const element: CmsFormElementBO = this.elements[i];
            if (element.name === dropInformation.element.name) {
                wasInSameParentContainerAtLowerIndex = true;
                break;
            }
        }

        this.beforeFormElementInsert.emit({
            element: dropInformation.element,
            insertionType: CmsFormElementInsertionType.MOVE,
        });
        const isNewElement =
            dropInformation.element && !dropInformation.element.globalId;

        if (isNewElement) {
            this.addElementFromMenu(dropInformation.element, index, aboveFirst);
        } else {
            if (wasInSameParentContainerAtLowerIndex) {
                index -= 1;
            }
            this.addElement(dropInformation.element, index, aboveFirst);
        }

        this.onElementsChange();
        this.dragCleanUp(undefined);
    }

    private dragCleanUp(event: DragEvent): void {
        if (event) {
            event.dataTransfer.clearData();
        }
        this.changeDetectorRef.markForCheck();
    }

    public addElementFromMenu(element: CmsFormElementBO, index?: number, aboveFirstElement: boolean = false): void {
        if (element && this.cannotReceive && this.cannotReceive.includes(element.type)) {
            return;
        }
        const newElement: CmsFormElementBO = _cloneDeep(element);

        // add unique id
        newElement.globalId = newUUID();
        // suggest name
        newElement.name = `${newElement.type}_${newElement.globalId.replace(
            /-/g,
            '_',
        )}`;

        this.addElement(newElement, index, aboveFirstElement);
    }

    private addElement(
        element: CmsFormElementBO,
        index?: number,
        aboveFirstElement: boolean = false,
    ): void {
        if (!element) {
            return;
        }
        if (Number.isInteger(index)) {
            const realIndex = aboveFirstElement ? 0 : index + 1;
            this.elements.splice(realIndex, 0, element);
        } else {
            this.elements.push(element);
        }
    }

    private removeElementAtIndex(index: number): void {
        this.elements.splice(index, 1);
    }

    private onElementsChange(): void {
        this.elementsChange.emit(this.elements);
    }

    public openPropertiesEditor(index: number, isOpen: boolean): void {
        this.propertiesEditorOpenMap[index] = isOpen;
    }

    onFormElementInsert(
        insertionInformation: CmsFormElementInsertionInformation,
    ): void {
        this.beforeFormElementInsert.emit(insertionInformation);
    }

    onPropertiesErrorChange(propertiesError: boolean): void {
        this.updatePropertiesError();
    }

    private updatePropertiesError(): void {
        let containsPropertiesError = false;

        if (this.formElementComponents) {
            containsPropertiesError = this.formElementComponents.reduce(
                (
                    containsPropertiesError: boolean,
                    formElementComponent: FormEditorElementComponent,
                ) => {
                    return formElementComponent &&
                        formElementComponent &&
                        formElementComponent.containsPropertyError
                        ? true
                        : containsPropertiesError;
                },
                false,
            );
        }

        this.propertiesErrorChange.emit(containsPropertiesError);
    }

    public onElementChange(element: CmsFormElementBO, index: number): void {
        if (this.elements) {
            this.elements[index] = element;
        }
        this.elementsChange.emit(this.elements);
    }
}
