import { ChangeDetectorRef, Component, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { AlohaComponent } from '@gentics/aloha-models';
import { BaseFormElementComponent } from '@gentics/ui-core';
import { AlohaIntegrationService } from '../../providers/aloha-integration/aloha-integration.service';

@Component({ template: '' })
export abstract class BaseAlohaRendererComponent<C extends AlohaComponent, T> extends BaseFormElementComponent<T> implements OnInit, OnDestroy {

    @Input()
    public slot?: string;

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

    public ngOnDestroy(): void {
        super.ngOnDestroy();
        this.unregisterAsRendered();
    }

    protected registerAsRendered(): void {
        if (!this.slot) {
            return;
        }
        this.aloha.renderedComponents[this.slot] = this;
    }

    protected unregisterAsRendered(): void {
        if (!this.slot) {
            return;
        }
        delete this.aloha.renderedComponents[this.slot];
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
            this.settings.visible = true;
            this.changeDetector.markForCheck();
        };
        this.settings.hide = () => {
            this.settings.visible = false;
            this.changeDetector.markForCheck();
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
