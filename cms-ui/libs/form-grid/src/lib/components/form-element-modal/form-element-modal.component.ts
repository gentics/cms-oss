import {
    AfterViewInit,
    ChangeDetectorRef,
    Component,
    ComponentFactoryResolver,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChanges,
    ViewChild,
    ViewContainerRef,
    ViewRef,
} from '@angular/core';
import { IModalDialog } from '@gentics/ui-core';
import { alignmentOptions } from '../../constants/alignment-options';
import { columnOptions } from '../../constants/column-options';
import { Element, ISchemaFieldProperties } from '@gentics/cms-models';
import { BooleanTypeComponent } from '../customComponent/custom-booleanType.component';
import { DropdownTypeComponent } from '../customComponent/custom-dropdownType.component';
import { StringTypeComponent } from '../customComponent/custom-stringType.component';

@Component({
    selector: 'andp-form-element-modal',
    templateUrl: './form-element-modal.component.html',
    styleUrls: ['./form-element-modal.component.scss'],
})
export class FormElementModalComponent implements IModalDialog, OnInit, AfterViewInit, OnChanges {
    @Input() element: Partial<Element> = {};
    @Input() schema: ISchemaFieldProperties;
    @Input() embedded = false;
    @Input() mode: 'modal' | 'panel' = 'modal';
    @Input() panelTitle = '';
    @Input() saveLabel = 'Speichern';
    @Input() cancelLabel = 'Abbrechen';
    @Output() save = new EventEmitter<Partial<Element>>();
    @Output() cancelEmbedded = new EventEmitter<void>();
    formGridElementOptions: any[];
    options = columnOptions;
    alignment = alignmentOptions;
    mesh: MeshService = null;
    fileExtensionsAllowedSelections = [];
    locales = [];
    isAggregateListDialogFormTable = false;
    hasUnits = false;
    hasDynamicEditOptions = false;

    @ViewChild('dynamicComponentContainer', { read: ViewContainerRef, static: true }) dynamicComponentContainer: ViewContainerRef;

    constructor(private cd: ChangeDetectorRef, mesh: MeshService, private resolver: ComponentFactoryResolver) {
        this.mesh = mesh;
        this.locales = this.mesh.getLocales();
        this.formGridElementOptions = this.mesh.getFormgridOptions();
    }

    ngOnInit(): void {
    // Ensure formGridOptions and overlayOptions exist
        if (!this.element.formGridOptions) {
            this.element.formGridOptions = {} as any;
        }
        if (!this.element.formGridOptions.overlayOptions) {
            this.element.formGridOptions.overlayOptions = {
                isShowOverlay: false,
                texts: {
                    textPre: {},
                    textPost: {},
                },
            } as any;
        }

        if (this.schema) {
            if (this.schema.unit) {
                this.hasUnits = true;
            }
            if (this.schema.type && this.schema.type == 'aggregate' && this.element.editModeDialogFormID && this.element.elements) {
                this.isAggregateListDialogFormTable = true;
            }
        }

        if (
            !this.mesh?.features?.feature_upload_constraints
            || !this.mesh.features.feature_upload_constraints.fileExtensionsAllowed
        ) {
            return;
        }
        const fileExtensionsAllowed = this.mesh.features.feature_upload_constraints.fileExtensionsAllowed;
        this.fileExtensionsAllowedSelections = fileExtensionsAllowed.reduce((accumulator, allowedFileExtension) => {
            let enabled = true;
            if (this.element.formGridOptions && this.element.formGridOptions.fileExtensionsNotAllowed) {
                enabled = this.element.formGridOptions.fileExtensionsNotAllowed.indexOf(allowedFileExtension) === -1;
            }
            accumulator.push({
                extension: allowedFileExtension,
                enabled,
            });
            return accumulator;
        }, []);
    }

    ngAfterViewInit() {
        this.syncOverlayEditorContent();
        (window as any).Aloha.ready(function () {
            (window as any).Aloha.jQuery('.aloha-editable').aloha();
        });
        this.loadDynamicComponents();
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (!changes.element && !changes.schema) {
            return;
        }

        if (!this.element.formGridOptions) {
            this.element.formGridOptions = {} as any;
        }
        if (!this.element.formGridOptions.overlayOptions) {
            this.element.formGridOptions.overlayOptions = {
                isShowOverlay: false,
                texts: {
                    textPre: {},
                    textPost: {},
                },
            } as any;
        }

        // Wait until the view reflects the new input bindings, then rebuild dynamic content.
        setTimeout(() => {
            this.syncOverlayEditorContent();
            this.loadDynamicComponents();
            this.cd.detectChanges();
        });
    }

    private syncOverlayEditorContent(): void {
        const overlayTexts = this.element?.formGridOptions?.overlayOptions?.texts;
        if (!overlayTexts) {
            return;
        }

        Object.keys(overlayTexts).forEach((overlayOptionTextKey) => {
            this.locales.forEach((local) => {
                const id = `overlayOptions_${overlayOptionTextKey}-${local}`;
                const valueFromFormGridOptionsI18n = overlayTexts[overlayOptionTextKey]?.[local] || '';
                const target = document.getElementById(id);
                if (target) {
                    target.innerHTML = valueFromFormGridOptionsI18n;
                }
            });
        });
    }

    loadDynamicComponents() {
        this.dynamicComponentContainer.clear();
        this.hasDynamicEditOptions = false;

        for (const formGridElementOption of this.formGridElementOptions) {
            if (this.schema && this.schema.type === formGridElementOption.name) {
                for (const option of formGridElementOption.options) {
                    const component = this.getComponentForType(option.type);
                    if (component) {
                        this.hasDynamicEditOptions = true;
                        const factory = this.resolver.resolveComponentFactory(component);
                        const componentRef = this.dynamicComponentContainer.createComponent(factory);
                        (componentRef.instance as any).element = this.element;
                        (componentRef.instance as any).options = option;
                        (componentRef.instance as any).schema = this.schema;
                        (componentRef.instance as any).mesh = this.mesh;
                    }
                }
            }
        }
    }

    closeFn: (val: Partial<Element>) => void = () => {};
    cancelFn: (val?: any) => void = () => {};

    registerCloseFn(close: (val: any) => void): void {
        if (this.isModalMode) {
            this.closeFn = close;
        }
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        if (this.isModalMode) {
            this.cancelFn = cancel;
        }
    }

    get isPanelMode(): boolean {
        return this.mode === 'panel';
    }

    get isModalMode(): boolean {
        return this.mode === 'modal' && !this.embedded;
    }

    onCancelClick(): void {
        this.cleanUpAloha();
        if (this.embedded || this.isPanelMode) {
            this.cancelEmbedded.emit();
        } else {
            this.cancelFn();
        }
    }

    edit() {
        const fileExtensionsNotAllowed = this.fileExtensionsAllowedSelections.reduce((accumulator, fileExtensionsAllowedSelection) => {
            if (!fileExtensionsAllowedSelection.enabled) {
                accumulator.push(fileExtensionsAllowedSelection.extension);
            }
            return accumulator;
        }, []);
        if (!this.element.formGridOptions) {
            this.element.formGridOptions = {} as any;
        }
        this.element.formGridOptions.fileExtensionsNotAllowed = fileExtensionsNotAllowed;
        this.cleanUpAloha();

        if (this.embedded || this.isPanelMode) {
            this.save.emit(this.element);
            return;
        }

        this.closeFn(this.element);
    }

    cleanUpAloha() {
        (window as any).Aloha.ready(function () {
            (window as any).Aloha.jQuery('.aloha-editable').mahalo();
        });
    }

    cancel() {
        this.onCancelClick();
    }

    get hasUploadConstraintOptions(): boolean {
        return this.mesh.hasFeature('feature_upload_constraints') && this?.schema?.type === 'binary';
    }

    get hasOverlayOptions(): boolean {
        return this.mesh.hasFeature('feature_form_elements_with_editing_overlay')
          && (this.schema?.type === 'string'
            || this.schema?.type === 'number'
            || this.schema?.type === 'property');
    }

    get hasAnyEditOptions(): boolean {
        return this.hasDynamicEditOptions || this.hasUploadConstraintOptions || this.hasOverlayOptions;
    }

    getComponentForType(type: string) {
        console.log('getComponentForType called with type:', type);
        switch (type.toLowerCase()) {
            case 'boolean':
                return BooleanTypeComponent;
            case 'string':
                return StringTypeComponent;
            case 'dropdown':
                return DropdownTypeComponent;
            default:
                console.log('No matching component found for type:', type);
                return null;
        }
    }

}
