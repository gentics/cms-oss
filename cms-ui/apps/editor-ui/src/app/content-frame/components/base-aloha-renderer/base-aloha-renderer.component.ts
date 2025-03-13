import {
    ChangeDetectorRef,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChanges,
} from '@angular/core';
import { AlohaComponent } from '@gentics/aloha-models';
import { BaseFormElementComponent } from '@gentics/ui-core';
import { AlohaIntegrationService } from '../../providers/aloha-integration/aloha-integration.service';
import { patchMultipleAlohaFunctions, unpatchAllAlohaFunctions } from '../../utils';

@Component({ template: '' })
export abstract class BaseAlohaRendererComponent<C extends AlohaComponent, T>
    extends BaseFormElementComponent<T>
    implements OnInit, OnChanges, OnDestroy {

    @Input()
    public slot?: string;

    @Input()
    public renderContext: string;

    @Input()
    public settings?: C | Partial<C> | Record<string, any>;

    /**
     * Event which is triggered to let the parent component (typically `DynamicDropdownComponent`) know,
     * if this component requires the user to manually confirm it with an additional confirm button.
     */
    @Output()
    public requiresConfirm = new EventEmitter<boolean>();

    /**
     * Event which is triggered when the parent component should confirm the current value of this component.
     * Typically used together with with `requiresConfirm` (`true`), and on final user input (i.E. text input and pressing `ENTER`).
     */
    @Output()
    public manualConfirm = new EventEmitter<void>();

    constructor(
        changeDetector: ChangeDetectorRef,
        public element: ElementRef<HTMLElement>,
        protected aloha: AlohaIntegrationService,
    ) {
        super(changeDetector);
    }

    public ngOnInit(): void {
        this.setupAlohaHooks();
        this.registerAsRendered();
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);
        if (changes.settings && !changes.settings.firstChange) {
            this.setupAlohaHooks();
        }
    }

    public override ngOnDestroy(): void {
        super.ngOnDestroy();
        unpatchAllAlohaFunctions(this.settings);
        this.unregisterAsRendered();
    }

    protected registerAsRendered(): void {
        const name = this.slot || this.settings?.name;
        if (!name) {
            return;
        }
        this.aloha.renderedComponents[name] = this;
    }

    protected unregisterAsRendered(): void {
        const name = this.slot || this.settings?.name;
        if (!name) {
            return;
        }
        delete this.aloha.renderedComponents[name];
    }

    protected setupAlohaHooks(): void {
        // Initialize/override setting functions
        patchMultipleAlohaFunctions(this.settings as AlohaComponent, {
            disable: () => this.setDisabledState(true),
            enable: () => this.setDisabledState(false),
            getValue: () => this.getFinalValue(),
            setValue: (val) => this.writeValue(val),
            show: () => {
                if (!this.settings.visible) {
                    this.settings.visible = true;
                    this.aloha.reloadToolbarSettings();
                    this.changeDetector.markForCheck();
                }
            },
            hide: () => {
                if (this.settings.visible) {
                    this.settings.visible = false;
                    this.aloha.reloadToolbarSettings();
                    this.changeDetector.markForCheck();
                }
            },
            touch: () => this.triggerTouch(),
        });
    }

    protected onValueChange(): void {
        if (!this.settings) {
            return;
        }
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.settings.triggerChangeNotification?.();
    }

    protected override onDisabledChange(): void {
        if (!this.settings) {
            return;
        }
        this.settings.disabled = true;
    }

    protected override onTouch(): void {
        if (!this.settings) {
            return;
        }
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.settings.triggerTouchNotification?.();
    }
}
