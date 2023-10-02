import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ComponentRef,
    ContentChild,
    EventEmitter,
    HostListener,
    Input,
    OnDestroy,
    Output,
    TemplateRef,
    ViewChild,
    ViewContainerRef,
} from '@angular/core';
import { take } from 'rxjs/operators';
import { DropdownAlignment, DropdownWidth, KeyCode } from '../../common';
import { DropdownTriggerDirective } from '../../directives/dropdown-trigger/dropdown-trigger.directive';
import { OverlayHostService } from '../../providers/overlay-host/overlay-host.service';
import { BaseComponent } from '../base-component/base.component';
import { DropdownContentWrapperComponent } from '../dropdown-content-wrapper/dropdown-content-wrapper.component';
import { DropdownContentComponent } from '../dropdown-content/dropdown-content.component';
import { ScrollMaskComponent } from '../scroll-mask/scroll-mask.component';

/**
 * A Dropdown component. Depends on the [`<gtx-overlay-host>`](#/overlay-host) being present in the app.
 *
 * The component expects two child elements:
 *
 * * `<gtx-dropdown-trigger>` - this element is the button/label which the user will click to open the dropdown.
 * * `<gtx-dropdown-content>` - contains the contents of the dropdown. If it contains a `<ul>`, specific styles will be applied
 *
 * The `<gtx-dropdown-content>` element may contain arbitrary content, but list items should be wrapped in `<gtx-dropdown-item>`.
 * This will allow keyboard support for list navigation.
 *
 *
 * ```html
 * <gtx-dropdown-list>
 *     <gtx-dropdown-trigger>
 *         <a>Show List</a>
 *     </gtx-dropdown-trigger>
 *     <gtx-dropdown-content>
 *          <gtx-dropdown-item>First</gtx-dropdown-item>
 *          <gtx-dropdown-item>Second</gtx-dropdown-item>
 *          <gtx-dropdown-item>Third</gtx-dropdown-item>
 *     </gtx-dropdown-content>
 * </gtx-dropdown-list>
 * ```
 *
 * ## Programmatic Use
 * When used programmatically (e.g. by getting a reference to the component via `@ContentChild(DropdownList)`, the
 * following extended API is available:
 *
 * - `dropdownList.isOpen: boolean`
 * - `dropdownList.openDropdown(): void`
 * - `dropdownList.closeDropdown(): void`
 * - `dropdownList.resize(): void`
 */
@Component({
    selector: 'gtx-dropdown-list',
    templateUrl: './dropdown-list.component.html',
    styleUrls: ['./dropdown-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DropdownListComponent extends BaseComponent implements OnDestroy {

    /**
     * Set the alignment of the dropdown, either 'left' or 'right'. *Default: 'left'*.
     */
    @Input()
    public align: DropdownAlignment = 'left';

    /**
     * Set the width of the dropdown. Can be either `contents`, `trigger`, `expand` or a numeric value. 'Contents' will
     * set a width sufficient to accommodate the widest list item. 'Trigger' sets the width to equal the width
     * of the trigger element. 'Expand' is equivalent to the maximum of 'trigger' and 'contents'.
     * A numeric value sets the width the a specific number of pixels. *Default: 'contents'*.
     */
    @Input()
    public width: DropdownWidth = 'contents';

    /**
     * If true, the dropdown will be positioned below the bottom of the trigger element. *Default: false*.
     */
    @Input()
    public belowTrigger = false;

    /**
     * If true, the dropdown will not close when clicked, but may only be closed by clicking outside the dropdown or
     * pressing escape. *Default: false*
     */
    @Input()
    public sticky = false;

    /**
     * If true, the dropdown will close when the escape key is pressed. *Default: true*
     */
    @Input()
    public closeOnEscape = true;

    /**
     * Fired whenever the dropdown contents are opened.
     */
    @Output()
    public open = new EventEmitter<void>();

    /**
     * Fired whenever the dropdown contents are closed.
     */
    @Output()
    public close = new EventEmitter<void>();

    @ViewChild('contents', { static: true, read: TemplateRef })
    protected contentsTemplate: TemplateRef<any>;

    @ContentChild(DropdownTriggerDirective, { static: true })
    protected trigger: DropdownTriggerDirective;

    @ContentChild(DropdownContentComponent)
    protected content: DropdownContentComponent;

    private overlayHostView: ViewContainerRef;
    private scrollMaskRef: ComponentRef<ScrollMaskComponent>;
    private contentComponentRef: ComponentRef<DropdownContentWrapperComponent> | null;

    get isOpen(): boolean {
        return !!this.contentComponentRef;
    }

    constructor(
        changeDetector: ChangeDetectorRef,
        overlayHostService: OverlayHostService,
    ) {
        super(changeDetector);

        overlayHostService.getHostView()
            .then(view => this.overlayHostView = view)
            .catch(err => console.error('Dropdown could not aquire a Overlay-Host instance!', err));
    }

    /**
     * Remove the content wrapper from the body.
     */
    ngOnDestroy(): void {
        this.closeDropdown();
    }

    /**
     * Prevent the user from causing a scroll via the keyboard.
     */
    @HostListener('keydown', ['$event'])
    keyHandler(e: KeyboardEvent): void {
        const keyCode = e.keyCode;
        const toPrevent = [
            KeyCode.UpArrow,
            KeyCode.DownArrow,
            KeyCode.PageUp,
            KeyCode.PageDown,
            KeyCode.Space,
            KeyCode.Home,
            KeyCode.End,
        ];

        if (-1 < toPrevent.indexOf(keyCode)) {
            e.preventDefault();
        }

        switch (keyCode) {
            case KeyCode.Escape:
                if (this.closeOnEscape === true) {
                    this.closeDropdown();
                }
                break;

            case KeyCode.Tab:
                if (this.isOpen) {
                    e.preventDefault();
                    this.content.focusFirstItem();
                }
        }
    }

    /**
     * Open the dropdown contents in the correct position.
     * @param ignoreDisabled If it should ignore that this dropdown is disabled. Useful for programmatic opening.
     */
    openDropdown(ignoreDisabled: boolean = false): void {
        if (this.disabled && !ignoreDisabled) {
            return;
        }

        this.contentComponentRef = this.overlayHostView.createComponent(DropdownContentWrapperComponent);
        const contentInstance = this.contentComponentRef.instance;
        contentInstance.content = this.contentsTemplate;
        contentInstance.trigger = this.trigger.elementRef.nativeElement;

        Object.assign(contentInstance.options, {
            alignment: this.align,
            width: this.width,
            belowTrigger: this.belowTrigger,
        });

        contentInstance.clicked.pipe(take(1)).subscribe(() => {
            if (!this.sticky) {
                this.closeDropdown();
            }
        });

        contentInstance.escapeKeyPressed.pipe(take(1)).subscribe(() => {
            if (this.closeOnEscape) {
                this.closeDropdown();
            }
        });
        // When focus is lost from the list items (by tabbing), close the dropdown and focus the
        // first child of the trigger is possible.
        this.content.focusLost.pipe(take(1)).subscribe(() => {
            this.closeDropdown();
            this.trigger.focus();
        });

        this.scrollMaskRef = this.overlayHostView.createComponent(ScrollMaskComponent);
        this.scrollMaskRef.instance.clicked.pipe(take(1)).subscribe(() => this.closeDropdown());
        this.open.emit();
    }

    resize(): void {
        if (this.contentComponentRef) {
            this.contentComponentRef.instance.setPositionAndSize();
        }
    }

    onTriggerClick(): void {
        if (this.disabled) {
            return;
        }

        if (!this.isOpen) {
            this.openDropdown();
        } else {
            this.closeDropdown();
        }
    }

    /**
     * Close the dropdown.
     */
    closeDropdown(): void {
        if (this.scrollMaskRef) {
            this.scrollMaskRef.destroy();
        }
        if (this.contentComponentRef) {
            this.contentComponentRef.destroy();
            this.contentComponentRef = null;
        }
        this.close.emit();
    }
}
