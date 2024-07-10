import { ChangeDetectorRef, Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges } from '@angular/core';
import { AlohaComponent } from '@gentics/aloha-models';
import { BaseFormElementComponent } from '@gentics/ui-core';
import { AlohaIntegrationService } from '../../providers/aloha-integration/aloha-integration.service';

@Component({ template: '' })
export abstract class BaseAlohaRendererComponent<C extends AlohaComponent, T> extends BaseFormElementComponent<T> implements OnInit, OnChanges, OnDestroy {

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
        this.unregisterAsRendered();
    }

    protected registerAsRendered(): void {
        if (!this.slot && !this.settings.name) {
            return;
        }
        this.aloha.renderedComponents[this.slot || this.settings.name] = this;
    }

    protected unregisterAsRendered(): void {
        if (!this.slot && !this.settings.name) {
            return;
        }
        delete this.aloha.renderedComponents[this.slot || this.settings.name];
    }

    protected setupAlohaHooks(): void {
        if (!this.settings) {
            return;
        }

        // Initialize/override setting functions
        this.settings.disable = () => {
            this.setDisabledState(true);
        };
        this.settings.enable = () => {
            this.setDisabledState(false);
        };
        this.settings.getValue = () => {
            return this.getFinalValue();
        };
        this.settings.setValue = (value) => {
            this.writeValue(value);
        };
        this.settings.show = () => {
            if (!this.settings.visible) {
                this.settings.visible = true;
                this.aloha.reloadToolbarSettings();
                this.changeDetector.markForCheck();
            }
        };
        this.settings.hide = () => {
            if (this.settings.visible) {
                this.settings.visible = false;
                this.aloha.reloadToolbarSettings();
                this.changeDetector.markForCheck();
            }
        };
        this.settings.touch = () => {
            this.triggerTouch();
        };
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
