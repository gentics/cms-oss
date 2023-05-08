import { ChangeDetectorRef, Directive, ElementRef, HostBinding, Input, OnDestroy, OnInit, Optional, Renderer2, Self, SkipSelf } from '@angular/core';
import {
    ButtonComponent,
    DropdownItemComponent,
    DropdownListComponent,
    FileDropAreaDirective,
    FilePickerComponent,
} from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { ApplicationStateService } from '../../../state';
import {
    UIButtonOverride, UIOverrideParameters,
    UIOverrideSlot,
    UIToolOverride,
} from '../../providers/ui-overrides/ui-overrides.model';
import { UIOverridesService } from '../../providers/ui-overrides/ui-overrides.service';


/**
 * Directive that makes certain UI elements hide or change behavior
 * based on a customer configuration, e.g. to redirect internal behavior to an
 * [embedded tool](https://www.gentics.com/Content.Node/guides/admin_custom_tools.html).
 */
@Directive({
    selector: '[overrideSlot]',
})
export class OverrideSlotDirective implements OnInit, OnDestroy {

    /** The name of the UI element in "ui-overrides.json" */
    @Input()
    overrideSlot: UIOverrideSlot;

    /** Parameters that can be used in strings in "ui-overrides.json" */
    @Input()
    overrideParams: UIOverrideParameters;

    /** Hiding is simply done via a global class and "!important" */
    @HostBinding('class.hidden-via-ui-override')
    shouldHide = false;

    private override: UIButtonOverride;
    private shouldDisable = false;

    private subscriptions = new Subscription();

    constructor(
        private changeDetector: ChangeDetectorRef,
        private elementRef: ElementRef,
        private renderer: Renderer2,
        private state: ApplicationStateService,
        private uiOverrides: UIOverridesService,
        @Optional() @Self() private button: ButtonComponent,
        @Optional() @Self() private dropdownItem: DropdownItemComponent,
        @Optional() @SkipSelf() private dropdownList: DropdownListComponent,
        @Optional() @Self() private filePicker: FilePickerComponent,
        @Optional() @Self() private fileDropArea: FileDropAreaDirective,
    ) { }

    ngOnInit(): void {
        this.followOverridesState();
    }

    ngAfterViewChecked(): void {
        if (this.override && this.disableShouldBeReapplied()) {
            this.applyOverride();
            this.changeDetector.markForCheck();
        }
    }

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }

    private followOverridesState(): void {
        let alreadyIntialized = false;
        const sub = this.state
            .select(state => state.ui)
            .filter(ui => ui.overridesReceived)
            .take(1)
            .subscribe(ui => {
                if (!ui.overrides.hasOwnProperty(this.overrideSlot)) {
                    // Most customers don't use overrides, which means we can disable all logic
                    return this.subscriptions.unsubscribe();
                }

                this.override = ui.overrides[this.overrideSlot];
                this.interceptNativeClickEvents();
                this.applyOverride();

                if (alreadyIntialized) {
                    this.changeDetector.markForCheck();
                }
            });

        this.subscriptions.add(sub);
        alreadyIntialized = true;
    }

    private interceptNativeClickEvents(): void {
        const hostElement: HTMLElement = this.elementRef && this.elementRef.nativeElement;
        if (!hostElement) { return; }

        if (this.button) {
            const eventWrapper = hostElement.querySelector('.button-event-wrapper');
            const clickSub = this.renderer.listen(eventWrapper, 'click', (event: MouseEvent) => {
                this.buttonClicked(event);
            });
            this.subscriptions.add(clickSub);
        }

        if (this.filePicker) {
            const clickSub = this.renderer.listen(hostElement, 'click', (event: MouseEvent) => {
                this.buttonClicked(event);
            });
            this.subscriptions.add(clickSub);
        }

        const isAnyOtherUIElement = !this.button && !this.filePicker && !this.fileDropArea;
        if (isAnyOtherUIElement) {
            const clickHandler = (event: MouseEvent): void => {
                if (event.target === hostElement || (event.target && (event.target as Element).parentNode === hostElement)) {
                    this.buttonClicked(event);
                }
            };
            hostElement.addEventListener('click', clickHandler, true);
            this.subscriptions.add(() => hostElement.removeEventListener('click', clickHandler, true));

            document.body.addEventListener('click', clickHandler, true);
            this.subscriptions.add(() => document.body.removeEventListener('click', clickHandler, true));
        }
    }

    private applyOverride(): void {
        if (!this.override) { return; }

        if ('hide' in this.override) {
            this.shouldHide = (this.override ).hide;
        }

        if ('disable' in this.override && (this.override ).disable) {
            this.shouldDisable = true;

            if (this.button) {
                this.button.disabled = true;
            }

            if (this.filePicker) {
                this.filePicker.disabled = true;
            }

            if (this.fileDropArea && !this.fileDropArea.options.disabled) {
                this.fileDropArea.options = {
                    ...this.fileDropArea.options,
                    disabled: true,
                };
            }
        }
    }

    private disableShouldBeReapplied(): boolean {
        if (!this.shouldDisable) {
            return false;
        } else if (this.button && !this.button.isDisabled) {
            return true;
        } else if (this.filePicker && !this.filePicker.disabled) {
            return true;
        } else if (this.fileDropArea && !this.fileDropArea.options.disabled) {
            return true;
        }

        return false;
    }

    private buttonClicked(event: MouseEvent): void {
        const isDisabled = this.shouldDisable || (this.button && this.button.disabled);

        if ((this.override as UIToolOverride).openTool && !isDisabled && !event.defaultPrevented) {
            this.interceptClick(event);
        }
    }

    private interceptClick(event: MouseEvent): void {
        event.preventDefault();
        event.stopImmediatePropagation();

        if (this.dropdownItem && this.dropdownList && !this.dropdownList.sticky) {
            this.dropdownList.closeDropdown();
        }

        this.uiOverrides.runOverride(this.overrideSlot, this.overrideParams);
    }

}
