import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    Output,
    QueryList,
    SimpleChanges,
    ViewChild,
    ViewChildren,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { BehaviorSubject, Subscription, timer } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { coerceInstance } from '../../utils';

export interface IBreadcrumbLink {
    href?: string;
    route?: any;
    text: string;
    tooltip?: string;
    [key: string]: any;
}

export interface IBreadcrumbRouterLink {
    route: any[];
    text: string;
    tooltip?: string;
    [key: string]: any;
}

/** The width configured in the .ellipsis CSS class. */
const ELLIPSIS_WIDTH = 13;

/**
 * A Breadcrumbs navigation component.
 *
 * ```html
 * <gtx-breadcrumbs></gtx-breadcrumbs>
 * ```
 */
@Component({
    selector: 'gtx-breadcrumbs',
    templateUrl: './breadcrumbs.component.html',
    styleUrls: ['./breadcrumbs.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BreadcrumbsComponent implements OnChanges, OnDestroy, AfterViewInit {

    /**
     * A list of links to display
     */
    @Input()
    public links: IBreadcrumbLink[];

    /**
     * A list of RouterLinks to display
     */
    @Input()
    public routerLinks: IBreadcrumbRouterLink[];

    /**
     * A color that is used for collapsed state background.
     */
    @Input()
    public collapsedColor: string;

    /**
     * If true the first folder and all the folder names from the end of the breadcrumbs, which fit into one line are shown
     * and an ellipsis in between.
     */
    @Input()
    public multiline = false;

    /**
     * If true the breadcrumbs are always expanded
     */
    @Input()
    public multilineExpanded = false;

    /**
     * Controls whether the navigation is disabled.
     */
    @Input()
    public disabled = false;

    /**
     * Fires when a link is clicked
     */
    @Output()
    public linkClick = new EventEmitter<IBreadcrumbLink | IBreadcrumbRouterLink>();

    /**
     * Fires when the expand button is clicked
     */
    @Output()
    public multilineExpandedChange = new EventEmitter<boolean>();

    isOverflowing = false;
    showArrow = false;

    backLink: IBreadcrumbLink | IBreadcrumbRouterLink;

    @ViewChildren(RouterLink)
    routerLinkChildren: QueryList<RouterLink>;

    @ViewChild('navWrapper')
    navWrapper: ElementRef<HTMLElement>;

    @ViewChild('lastPart')
    lastPart: ElementRef<HTMLElement>;

    private subscriptions = new Subscription();
    private resizeEvents = new BehaviorSubject<void>(null);

    constructor(
        private changeDetector: ChangeDetectorRef,
        private elementRef: ElementRef<HTMLElement>,
    ) { }

    ngAfterViewInit(): void {
        const element = this.elementRef.nativeElement;
        if (element) {
            // Listen in the "capture" phase to prevent routerLinks when disabled
            element.firstElementChild.addEventListener('click', this.preventClicksWhenDisabled, true);
            element.style.setProperty('--collapsedColor', this.collapsedColor);
        }

        const timerSub = timer(500, 500)
            .subscribe(() => this.resizeEvents.next());
        this.subscriptions.add(timerSub);
        this.setUpResizeSub();

        this.preventDisabledRouterLinks();
        this.routerLinkChildren.changes.subscribe(() => this.preventDisabledRouterLinks());
        this.resizeEvents.next(null);
    }

    ngOnChanges(changes: SimpleChanges): void {
        coerceInstance(this, ['multiline', 'multilineExpanded', 'disabled'], changes);
        if (changes['links'] || changes['routerLinks']) {
            const allLinks = (this.links || []).concat(this.routerLinks || []);
            this.backLink = allLinks[allLinks.length - 2];
            this.resizeEvents.next(null);
        }
        if (changes['multiline'] || changes['multilineExpanded']) {
            this.resizeEvents.next(null);
        }
    }

    ngOnDestroy(): void {
        const element: HTMLElement = this.elementRef.nativeElement;
        element.firstElementChild.removeEventListener('click', this.preventClicksWhenDisabled, true);
        this.subscriptions.unsubscribe();
    }

    onLinkClicked(link: IBreadcrumbLink | IBreadcrumbRouterLink, event: Event): void {
        if (this.disabled) {
            event.preventDefault();
            event.stopImmediatePropagation();
        } else {
            this.linkClick.emit(link);
        }
    }

    toggleMultilineExpanded(): void {
        this.multilineExpanded = !this.multilineExpanded;
        this.multilineExpandedChange.emit(this.multilineExpanded);
        this.resizeEvents.next(null);
        this.changeDetector.markForCheck();
    }

    private setUpResizeSub() {
        let prevLinks: IBreadcrumbLink[];
        let prevRouterLinks: IBreadcrumbRouterLink[];
        let prevIsExpanded: boolean;
        let prevNavWidth = -1;

        this.subscriptions.add(this.resizeEvents.pipe(
            debounceTime(5),
        ).subscribe(() => {
            if (!this.lastPart || !this.navWrapper) {
                return;
            }
            // If neither the links, nor isMultilineExpanded, nor the navWrapper element's clientWidth has changed, we don't need to do anything.
            const currNavWidth = this.navWrapper.nativeElement.clientWidth;
            if (
                prevLinks === this.links
                && prevRouterLinks === this.routerLinks
                && prevIsExpanded === this.multilineExpanded
                && prevNavWidth === currNavWidth
            ) {
                return;
            }
            prevLinks = this.links;
            prevRouterLinks = this.routerLinks;
            prevIsExpanded = this.multilineExpanded;
            prevNavWidth = currNavWidth;

            const elements: NodeListOf<HTMLElement> = this.lastPart.nativeElement.querySelectorAll('a.breadcrumb');
            if (elements.length > 0) {
                const firstOffsetBottom = elements[0].offsetTop + elements[0].offsetHeight;
                const lastOffsetBottom = elements[elements.length - 1].offsetTop + elements[elements.length - 1].offsetHeight;
                this.showArrow = firstOffsetBottom !== lastOffsetBottom;
            } else {
                this.showArrow = false;
            }
            this.shortenTexts();
            this.changeDetector.markForCheck();
        }));
    }

    private shortenTexts() {
        const navWrapper = this.navWrapper.nativeElement;
        const lastPart = this.lastPart.nativeElement;
        const innerElements = lastPart.querySelectorAll('a.breadcrumb');
        const defaultElements = this.getCuttableBreadcrumbsTexts();

        this.isOverflowing = false;

        // Reset all elements to their default states.
        const offset = this.multilineExpanded ? 0 : 1;
        for (let i = 0; i < innerElements.length; i++) {
            const innerElement = innerElements[i];
            innerElement.classList.remove('without');
            innerElement.classList.remove('hidden');
            innerElement.textContent = defaultElements[i + offset];
        }

        if (this.multilineExpanded) {
            return;
        }

        for (let i = 0; i < innerElements.length; ++i) {
            const innerElement = innerElements[i];
            while (lastPart.offsetLeft + lastPart.scrollWidth + ELLIPSIS_WIDTH > navWrapper.clientWidth) {
                this.isOverflowing = true;
                if (innerElement.textContent.length === 0) {
                    innerElement.classList.add('hidden');
                    const nextInnerElement = innerElements[i + 1];
                    if (nextInnerElement) {
                        nextInnerElement.classList.add('without');
                    }
                    break;
                } else {
                    innerElement.textContent = innerElement.textContent.substring(1);
                }
            }
        }
    }

    private getCuttableBreadcrumbsTexts(): string[] {
        const defaultBreadcrumbs: string[] = [];
        if (this.links) {
            for (const link of this.links) {
                defaultBreadcrumbs.push(link.text);
            }
        }
        if (this.routerLinks) {
            for (const link of this.routerLinks) {
                defaultBreadcrumbs.push(link.text);
            }
        }
        return defaultBreadcrumbs;
    }

    onResize(event: any): void {
        this.resizeEvents.next(null);
    }

    private preventClicksWhenDisabled = (ev: Event): void => {
        if (this.disabled) {
            const target = ev.target as HTMLElement;
            if (target.tagName.toLowerCase() === 'a' && target.classList.contains('breadcrumb')) {
                ev.preventDefault();
                ev.stopImmediatePropagation();
            }
        }
    }

    /**
     * Workaround/Hack for the native angular "RouterLink" having no way to disable navigation on click.
     */
    private preventDisabledRouterLinks(): void {
        // eslint-disable-next-line @typescript-eslint/no-this-alias
        const thisComponent = this;

        for (const link of this.routerLinkChildren.filter(link => !link.hasOwnProperty('onClick'))) {
            const originalOnClick = link.onClick;
            link.onClick = function interceptedOnClick(...args: any[]): boolean {
                if (thisComponent.disabled) {
                    return true;
                } else {
                    return originalOnClick.apply(this, args);
                }
            };
        }
    }
}
