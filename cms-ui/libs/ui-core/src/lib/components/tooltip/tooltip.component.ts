import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    ComponentRef,
    ContentChild,
    ElementRef,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    SimpleChanges,
    TemplateRef,
    ViewChild,
    ViewContainerRef,
} from '@angular/core';
import { ColorThemes, TooltipAlignment, TooltipPosition } from '../../common';
import { TooltipContentDirective } from '../../directives/tooltip-content/tooltip-content.directive';
import { TooltipTriggerDirective } from '../../directives/tooltip-trigger/tooltip-trigger.directive';
import { StyleObj } from '../../internal';
import { OverlayHostService } from '../../providers/overlay-host/overlay-host.service';
import { TooltipContentWrapperComponent } from '../tooltip-content-wrapper/tooltip-content-wrapper.component';

/**
 * The Tooltip component wraps existing content and shows additional content when hovered over.
 * Addtional content is being layed over the existing content and allows you to display bigger,
 * miscelanoius, or any other content that doesn't usually have space.
 *
 * ```html
 * <gtx-tooltip>
 *     <div class="something">
 *         <gtx-button>Hello World!</gtx-button>
 *     </div>
 *     <div gtx-tooltip-content>
 *         I will only be displayed on hover!
 *     </div>
 * </gtx-tooltip>
 * ```
 */
@Component({
    selector: 'gtx-tooltip',
    templateUrl: './tooltip.component.html',
    styleUrls: ['./tooltip.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TooltipComponent implements OnInit, OnChanges, AfterViewInit, OnDestroy {

    /**
     * The position where the tooltip should show up. Has to be one of the following:
     * `top`, `right`, `bottom`, `left`.
     */
    @Input()
    public position: TooltipPosition = 'top';

    /**
     * How the Tooltip should be aligned once it's in position.
     */
    @Input()
    public align: TooltipAlignment = 'center';

    /**
     * The color-type to use for the tooltip. Has to be one of the following:
     * `primary`, `secondary`, `success`, `warning`, `error`
     */
    @Input()
    public type: (ColorThemes | 'light' | 'dark') = 'secondary';

    /**
     * How many milliseconds it should wait (on hover) before it'll show the content.
     */
    @Input()
    public delay = 500;

    /**
     * Projected trigger content. Will be the hover reference unless additionally specified via
     * the `TooltipTriggerDirective`. See `triggerDir`.
     */
    @ViewChild('trigger', { static: true })
    public triggerRef: ElementRef<HTMLDivElement>;

    @ContentChild(TooltipTriggerDirective, { static: true })
    public triggerDir: TooltipTriggerDirective;

    @ViewChild('projectedContent', { static: true, read: TemplateRef })
    public contentRef: TemplateRef<any>;

    @ContentChild(TooltipContentDirective, { static: true })
    public contentDir: TooltipContentDirective;

    private overlayHostView: ViewContainerRef;
    private contentWrapper: ComponentRef<TooltipContentWrapperComponent>;
    private viewInitialized = false;
    private hostViewInitialzed = false;

    private isHovered = false;
    private manuallyOpened = false;

    private hoverElement: Element;
    private hoverDelayTimeout: number;
    private hoverStillInbound = false;
    private hoverStartHandler: () => void;
    private hoverEndHandler: () => void;

    constructor(
        private overlayService: OverlayHostService,
    ) {}

    ngOnInit(): void {
        this.overlayService.getHostView()
            .then(view => {
                this.overlayHostView = view;
                this.hostViewInitialzed = true;
                this.setupContentWrapper();
            })
            .catch(err => {
                console.error('Could not get an instance of the overlay-host for the tooltip!', err);
            });
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.position || changes.mobilePosition || changes.align || changes.type) {
            this.updateWrapperContent();
        }
    }

    ngAfterViewInit(): void {
        if (this.triggerDir?.element?.nativeElement) {
            this.hoverElement = this.triggerDir.element.nativeElement;
        }

        if (this.triggerRef?.nativeElement) {
            if (!this.hoverElement) {
                this.hoverElement = this.triggerRef.nativeElement;
            }
        }

        if (this.hoverElement) {
            this.hoverStartHandler = () => this.hoverStart();
            this.hoverEndHandler = () => this.hoverEnd();
            this.hoverElement.addEventListener('mouseover', this.hoverStartHandler);
            this.hoverElement.addEventListener('mouseout', this.hoverEndHandler);
        }

        this.viewInitialized = true;
        this.setupContentWrapper();

        // Additional call to update the wrapper with a delay, to prevent the initial repositioning to be too intrusive.
        setTimeout(() => {
            this.updateWrapperContent();
        }, 10);
    }

    ngOnDestroy(): void {
        if (this.hoverElement) {
            if (this.hoverStartHandler) {
                this.hoverElement.removeEventListener('mouseenter', this.hoverStartHandler);
            }
            if (this.hoverEndHandler) {
                this.hoverElement.removeEventListener('mouseout', this.hoverEndHandler);
            }
        }

        if (this.contentWrapper) {
            this.contentWrapper.destroy();
        }
    }

    public open(): void {
        this.manuallyOpened = true;
        this.updateWrapperContent();
    }

    public close(): void {
        this.manuallyOpened = false;
        this.updateWrapperContent();
    }

    public toggle(): void {
        this.manuallyOpened = !this.manuallyOpened;
        this.updateWrapperContent();
    }

    private hoverStart(): void {
        if (this.hoverDelayTimeout) {
            window.clearTimeout(this.hoverDelayTimeout);
        }

        this.hoverStillInbound = true;
        this.hoverDelayTimeout = window.setTimeout(() => {
            if (this.hoverStillInbound) {
                this.isHovered = true;
                this.updateWrapperContent();
            }
        }, this.delay);
    }

    private hoverEnd(): void {
        if (this.hoverDelayTimeout) {
            window.clearTimeout(this.hoverDelayTimeout);
        }
        this.hoverStillInbound = false;
        this.isHovered = false;
        this.updateWrapperContent();
    }

    private setupContentWrapper(): void {
        if (!this.hostViewInitialzed || !this.viewInitialized || !this.overlayHostView || !this.contentDir?.element?.nativeElement) {
            return;
        }

        this.contentWrapper = this.overlayHostView.createComponent(TooltipContentWrapperComponent);
        this.contentWrapper.instance.content = this.contentRef;
        this.contentWrapper.changeDetectorRef.markForCheck();

        this.updateWrapperContent();
    }

    private updateWrapperContent(): void {
        const el: HTMLElement = this.contentWrapper?.instance?.element?.nativeElement;
        if (!el) {
            return;
        }

        // Update the class-property to the new values.
        el.className = `position-${this.position} align-${this.align} type-${this.type}`;

        if (this.isHovered || this.manuallyOpened) {
            el.classList.add('is-active');
        }

        this.contentWrapper.instance.styling = this.createStyleObj();
        this.contentWrapper.instance.updateAppliedStyles();

        this.contentWrapper.changeDetectorRef.markForCheck();
    }

    private createStyleObj(): StyleObj {
        const rect = this.hoverElement.getBoundingClientRect();

        return {
            /* eslint-disable @typescript-eslint/naming-convention */
            '--trigger-width': `${rect.width}px`,
            '--trigger-height': `${rect.height}px`,
            '--trigger-x': `${rect.x}px`,
            '--trigger-y': `${rect.y}px`,
            /* eslint-enable @typescript-eslint/naming-convention */
        };
    }
}
