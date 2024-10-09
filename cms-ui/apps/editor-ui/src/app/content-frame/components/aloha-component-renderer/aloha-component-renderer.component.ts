import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ComponentRef,
    EnvironmentInjector,
    EventEmitter,
    Injector,
    Input,
    OnChanges,
    OnDestroy,
    Output,
    SimpleChanges,
    Type,
    ViewChild,
    ViewContainerRef
} from '@angular/core';
import { ControlValueAccessor } from '@angular/forms';
import { AlohaComponent, AlohaCoreComponentNames } from '@gentics/aloha-models';
import { generateFormProvider } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { AlohaAttributeButtonRendererComponent } from '../aloha-attribute-button-renderer/aloha-attribute-button-renderer.component';
import { AlohaAttributeToggleButtonRendererComponent } from '../aloha-attribute-toggle-button-renderer/aloha-attribute-toggle-button-renderer.component';
import { AlohaButtonRendererComponent } from '../aloha-button-renderer/aloha-button-renderer.component';
import { AlohaCheckboxRendererComponent } from '../aloha-checkbox-renderer/aloha-checkbox-renderer.component';
import { AlohaColorPickerRendererComponent } from '../aloha-color-picker-renderer/aloha-color-picker-renderer.component';
import { AlohaContextButtonRendererComponent } from '../aloha-context-button-renderer/aloha-context-button-renderer.component';
import { AlohaContextToggleButtonRendererComponent } from '../aloha-context-toggle-button-renderer/aloha-context-toggle-button-renderer.component';
import { AlohaDateTimePickerRendererComponent } from '../aloha-date-time-picker-renderer/aloha-date-time-picker-renderer.component';
import { AlohaIFrameRendererComponent } from '../aloha-iframe-renderer/aloha-iframe-renderer.component';
import { AlohaInputRendererComponent } from '../aloha-input-renderer/aloha-input-renderer.component';
import { AlohaLinkTargetRendererComponent } from '../aloha-link-target-renderer/aloha-link-target-renderer.component';
import { AlohaSelectMenuRendererComponent } from '../aloha-select-menu-renderer/aloha-select-menu-renderer.component';
import { AlohaSelectRendererComponent } from '../aloha-select-renderer/aloha-select-renderer.component';
import { AlohaSplitButtonRendererComponent } from '../aloha-split-button-renderer/aloha-split-button-renderer.component';
import { AlohaSymbolGridRendererComponent } from '../aloha-symbol-grid-renderer/aloha-symbol-grid-renderer.component';
import { AlohaSymbolSearchGridRendererComponent } from '../aloha-symbol-search-grid-renderer/aloha-symbol-search-grid-renderer.component';
import { AlohaTableSizeSelectRendererComponent } from '../aloha-table-size-select-renderer/aloha-table-size-select-renderer.component';
import { AlohaToggleButtonRendererComponent } from '../aloha-toggle-button-renderer/aloha-toggle-button-renderer.component';
import { AlohaToggleSplitButtonRendererComponent } from '../aloha-toggle-split-button-renderer/aloha-toggle-split-button-renderer.component';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';

const RENDER_COMPONENTS: Record<string, Type<BaseAlohaRendererComponent<any, any>>> = {
    [AlohaCoreComponentNames.ATTRIBUTE_BUTTON]: AlohaAttributeButtonRendererComponent,
    [AlohaCoreComponentNames.ATTRIBUTE_TOGGLE_BUTTON]: AlohaAttributeToggleButtonRendererComponent,
    [AlohaCoreComponentNames.BUTTON]: AlohaButtonRendererComponent,
    [AlohaCoreComponentNames.CHECKBOX]: AlohaCheckboxRendererComponent,
    [AlohaCoreComponentNames.COLOR_PICKER]: AlohaColorPickerRendererComponent,
    [AlohaCoreComponentNames.CONTEXT_BUTTON]: AlohaContextButtonRendererComponent,
    [AlohaCoreComponentNames.CONTEXT_TOGGLE_BUTTON]: AlohaContextToggleButtonRendererComponent,
    [AlohaCoreComponentNames.DATE_TIME_PICKER]: AlohaDateTimePickerRendererComponent,
    [AlohaCoreComponentNames.IFRAME]: AlohaIFrameRendererComponent,
    [AlohaCoreComponentNames.INPUT]: AlohaInputRendererComponent,
    [AlohaCoreComponentNames.LINK_TARGET]: AlohaLinkTargetRendererComponent,
    [AlohaCoreComponentNames.SELECT]: AlohaSelectRendererComponent,
    [AlohaCoreComponentNames.SELECT_MENU]: AlohaSelectMenuRendererComponent,
    [AlohaCoreComponentNames.SPLIT_BUTTON]: AlohaSplitButtonRendererComponent,
    [AlohaCoreComponentNames.SYMBOL_GRID]: AlohaSymbolGridRendererComponent,
    [AlohaCoreComponentNames.SYMBOL_SEARCH_GRID]: AlohaSymbolSearchGridRendererComponent,
    [AlohaCoreComponentNames.TABLE_SIZE_SELECT]: AlohaTableSizeSelectRendererComponent,
    [AlohaCoreComponentNames.TOGGLE_BUTTON]: AlohaToggleButtonRendererComponent,
    [AlohaCoreComponentNames.TOGGLE_SPLIT_BUTTON]: AlohaToggleSplitButtonRendererComponent,
} as any;

@Component({
    selector: 'gtx-aloha-component-renderer',
    templateUrl: './aloha-component-renderer.component.html',
    styleUrls: ['./aloha-component-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaComponentRendererComponent)],
})
export class AlohaComponentRendererComponent implements ControlValueAccessor, AfterViewInit, OnChanges, OnDestroy {

    @Input()
    public slot?: string;

    @Input()
    public renderContext: string;

    @Input()
    public component?: AlohaComponent;

    @Input()
    public type?: string;

    @Input()
    public settings?: Record<string, any>;

    @Input()
    public disabled = false;

    /**
     * @see BaseAlohaRendererComponent.requiresConfirm
     */
    @Output()
    public requiresConfirm = new EventEmitter<boolean>();

    /**
     * @see BaseAlohaRendererComponent.manualConfirm
     */
    @Output()
    public manualConfirm = new EventEmitter<void>();

    /**
     * Special output for dropdowns, since these have to be manually resized when the content size changes.
     */
    @Output()
    public hideHeader = new EventEmitter<boolean>();

    @ViewChild('ref', { read: ViewContainerRef, static: true })
    public containerRef: ViewContainerRef;

    protected isInitalized = false;
    protected createInstanceType: string = null;
    protected instanceRef: ComponentRef<BaseAlohaRendererComponent<any, any>>;

    private cvaValueReceived = false;
    /** Internal values for control-value accessor impl */
    private cvaChange: (value: any) => void;
    private cvaTouch: () => void;
    private cvaValue: any;
    private cvaDisabled = false;

    protected subscriptions: Subscription[] = [];

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected injector: Injector,
        protected envInjector: EnvironmentInjector,
    ) {}

    ngOnChanges(changes: SimpleChanges): void {
        if ((changes.type || changes.component)) {
            // If the type changes, we have to re-create the instance
            if ((this.createInstanceType !== (this.type || this.component?.type))) {
                this.setupInstance();
            } else {
                this.forwardInputs();
            }
        } else if (changes.slot || changes.settings || changes.disabled) {
            this.forwardInputs();
        }
    }

    ngAfterViewInit(): void {
        this.isInitalized = true;
        this.setupInstance();

        // Otherwiese it's not rendered correctly initially
        setTimeout(() => {
            this.changeDetector.markForCheck();
            if (this.instanceRef) {
                this.instanceRef.changeDetectorRef.markForCheck();
            }
        });
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
        if (this.instanceRef) {
            this.instanceRef.destroy();
            this.instanceRef = null;
        }
    }

    protected setupInstance(): void {
        // cannot create a component yet
        if (!this.isInitalized) {
            return;
        }

        if (this.instanceRef) {
            this.instanceRef.destroy();
            this.instanceRef = null;
        }

        this.createInstanceType = this.type || this.component?.type;
        const componentType = RENDER_COMPONENTS[this.createInstanceType];
        if (componentType == null) {
            this.createInstanceType = null;
            return;
        }

        this.instanceRef = this.containerRef.createComponent(componentType, {
            injector: this.injector,
            environmentInjector: this.envInjector,
        });
        this.forwardInputs();
        this.setupEventForward();

        // Forward CVA
        this.instanceRef.instance.registerOnChange(this.cvaChange);
        this.instanceRef.instance.registerOnTouched(this.cvaTouch);
        this.instanceRef.instance.setDisabledState(this.cvaDisabled);
        if (this.cvaValueReceived) {
            this.instanceRef.instance.writeValue(this.cvaValue);
        }
        this.cvaValueReceived = false;

        this.instanceRef.changeDetectorRef.markForCheck();
        this.changeDetector.markForCheck();
    }

    protected forwardInputs(): void {
        if (!this.instanceRef?.instance) {
            return;
        }
        this.instanceRef.instance.slot = this.slot;
        this.instanceRef.instance.renderContext = this.renderContext ?? this.settings?.renderContext;
        this.instanceRef.instance.settings = this.component || this.settings;
        this.instanceRef.instance.disabled = this.disabled;

        this.instanceRef.changeDetectorRef.markForCheck();
        this.changeDetector.markForCheck();
    }

    protected setupEventForward(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
        this.subscriptions = [];

        this.subscriptions.push(this.instanceRef.instance.requiresConfirm.subscribe(confirm => {
            this.requiresConfirm.emit(confirm);
        }));
        this.subscriptions.push(this.instanceRef.instance.manualConfirm.subscribe(() => {
            this.manualConfirm.emit();
        }));

        if (this.createInstanceType === AlohaCoreComponentNames.SELECT_MENU) {
            (this.instanceRef.instance as AlohaSelectMenuRendererComponent).multiStepActivation.subscribe(stepId => {
                this.hideHeader.emit(stepId != null);
            });
        }
    }

    writeValue(value: any): void {
        this.cvaValueReceived = true;
        this.cvaValue = value;

        if (!this.instanceRef?.instance) {
            return;
        }

        this.instanceRef.instance.writeValue(value);
    }

    registerOnChange(fn: any): void {
        this.cvaChange = fn;
    }

    registerOnTouched(fn: any): void {
        this.cvaTouch = fn;
    }

    setDisabledState?(isDisabled: boolean): void {
        this.cvaDisabled = isDisabled;
        if (!this.instanceRef?.instance) {
            return;
        }
        this.instanceRef.instance.setDisabledState(isDisabled);
    }
}
