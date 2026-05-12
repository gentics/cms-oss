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
    ViewChild,
    ViewContainerRef,
} from '@angular/core';
import { ControlValueAccessor } from '@angular/forms';
import { AlohaComponent, AlohaCoreComponentNames } from '@gentics/aloha-models';
import { generateFormProvider } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { RenderedAlohaComponent } from '../../models/internal';
import { AlohaComponentResolverService } from '../../providers/aloha-component-resolver/aloha-component-resolver.service';

@Component({
    selector: 'gtx-aloha-component-renderer',
    templateUrl: './aloha-component-renderer.component.html',
    styleUrls: ['./aloha-component-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaComponentRendererComponent)],
    standalone: false,
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
     * When the rendered component is initialized
     */
    @Output()
    public initialized = new EventEmitter<void>();

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
    protected instanceRef: ComponentRef<RenderedAlohaComponent<any, any>>;

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
        protected resolver: AlohaComponentResolverService,
    ) { }

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
        this.subscriptions.forEach((s) => s.unsubscribe());
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
        const componentType = this.resolver.resolveComponent(this.createInstanceType);
        if (componentType == null) {
            console.warn(`Could not render unknown aloha component from type "${this.createInstanceType}"`);
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
        this.initialized.emit();
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
        this.subscriptions.forEach((s) => s.unsubscribe());
        this.subscriptions = [];

        this.subscriptions.push(this.instanceRef.instance.requiresConfirm.subscribe((confirm) => {
            this.requiresConfirm.emit(confirm);
        }));
        this.subscriptions.push(this.instanceRef.instance.manualConfirm.subscribe(() => {
            this.manualConfirm.emit();
        }));

        if (this.createInstanceType === AlohaCoreComponentNames.SELECT_MENU) {
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            this.subscriptions.push((this.instanceRef.instance as any).multiStepActivation.subscribe((stepId) => {
                this.hideHeader.emit(stepId != null);
            }));
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
