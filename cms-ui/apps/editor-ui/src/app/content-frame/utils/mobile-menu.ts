import * as Hammer from 'hammerjs';
import { debounce } from '../../common/utils/debounce';

const PAGE_ATTRIBUTE = 'data-page';
const MAX_PAGES_ATTRIBUTE = 'data-max-pages';
const CLASS_INACTIVE = 'inactive';
const CLASS_STABLE = 'stable';
const CLASS_HAS_PREVIOUS = 'has-previous';
const CLASS_HAS_NEXT = 'has-next';
const CLASS_ENTRY = 'gtx-menu-element';

function pixelStringToNumber(str: string): number {
    return parseFloat(str.substring(0, str.length - 2));
}

function getElementWidth(element: Element): number {
    let width = element.getBoundingClientRect().width;
    const style = getComputedStyle(element);
    width += pixelStringToNumber(style.marginLeft);
    width += pixelStringToNumber(style.marginRight);

    return width;
}

export class MobileMenu {

    public currentPage = 1;
    public maxPages = 1;
    public pagedElements: Element[][] = [];

    public threshHold = 440;

    private initialized = false;
    private resize: ResizeObserver | null = null;
    private mngr: HammerManager | null = null;

    private debouncedWidthChecker: () => void = () => {};

    constructor(
        private ref: Element,
    ) { }

    init(threshHold?: number): void {
        if (this.initialized) {
            return;
        }

        if (threshHold != null) {
            this.threshHold = threshHold;
        }

        this.initialized = true;
        this.debouncedWidthChecker = debounce<void>(() => this.checkWidth(), 250);
        this.resize = new ResizeObserver(() => {
            this.debouncedWidthChecker();
        });
        this.resize.observe(this.ref);
        this.debouncedWidthChecker();

        this.mngr = new Hammer.Manager(this.ref, {
            recognizers: [
                [Hammer.Swipe, { direction: Hammer.DIRECTION_HORIZONTAL }],
            ],
        });

        this.mngr.on('swipe', event => {
            if (event.direction === Hammer.DIRECTION_LEFT) {
                this.modifyPage(1);
            } else if (event.direction === Hammer.DIRECTION_RIGHT) {
                this.modifyPage(-1);
            }
        });
    }

    destroy(): void {
        if (!this.initialized) {
            return;
        }

        if (this.resize) {
            this.resize.disconnect();
            this.resize = null;
        }

        if (this.mngr) {
            this.mngr.destroy();
            this.mngr = null;
        }

        this.initialized = false;
    }

    setThreshHold(threshHold: number): void {
        this.threshHold = threshHold;
        this.debouncedWidthChecker();
    }

    setPage(page: number): void {
        this.currentPage = Math.max(1, Math.min(this.maxPages, page));
        this.ref.setAttribute(PAGE_ATTRIBUTE, `${this.currentPage}`);
        if (this.currentPage > 1) {
            this.ref.classList.add(CLASS_HAS_PREVIOUS);
        } else {
            this.ref.classList.remove(CLASS_HAS_PREVIOUS);
        }
        if (this.currentPage < this.maxPages) {
            this.ref.classList.add(CLASS_HAS_NEXT);
        } else {
            this.ref.classList.remove(CLASS_HAS_NEXT);
        }

        this.determineElementsToDisplay();
    }

    determineElementsToDisplay(): void {
        for (let i = 0; i < this.pagedElements.length; i++) {
            const elements = this.pagedElements[i];
            for (const elem of elements) {
                if (i === (this.currentPage - 1)) {
                    elem.classList.remove(CLASS_INACTIVE);
                } else {
                    elem.classList.add(CLASS_INACTIVE);
                }
            }
        }
    }

    modifyPage(mod: number): void {
        const newPage: number = (parseInt(this.ref.getAttribute(PAGE_ATTRIBUTE) || '0', 10) || 0) + mod;
        this.setPage(newPage);
    }

    checkWidth(): void {
        this.ref.classList.remove(CLASS_STABLE);
        const children = Array.from(this.ref.querySelectorAll('.' + CLASS_ENTRY));

        // Make all items visible first, as it's needed for size calculations
        for (const item of children) {
            item.classList.remove(CLASS_INACTIVE);
            item.removeAttribute(PAGE_ATTRIBUTE);
        }

        const bodyRect = document.body.getBoundingClientRect();

        // If the threshhold isn't reached, we don't want the menu behaviour yet
        if (bodyRect.width > this.threshHold) {
            this.ref.classList.add(CLASS_STABLE);
            this.setPage(1);
            return;
        }

        const width = this.ref.getBoundingClientRect().width;

        if (width === this.ref.scrollWidth) {
            this.maxPages = 1;
            // Only one page available
            this.pagedElements = [children];
            this.pagedElements[0].forEach(item => {
                item.setAttribute(PAGE_ATTRIBUTE, `${this.maxPages}`);
            });

            this.ref.classList.add(CLASS_STABLE);
            // Set the page to 1 if there's no scrolling
            this.setPage(1);
            return;
        }

        this.recreatePages(children);
    }

    recreatePages(children: Element[]): void {
        this.ref.classList.remove(CLASS_STABLE);
        this.maxPages = 1;
        this.pagedElements = [];

        const availablePixels = this.ref.getBoundingClientRect().width;
        let usedPixels = 0;
        let elements: Element[] = [];
        let doAgain = false;

        for (const item of children) {
            if (item == null) {
                continue;
            }

            const itemWidth = getElementWidth(item);

            // Edge case, where the items aren't rendered properly yet
            if (itemWidth === 0) {
                doAgain = true;
                break;
            }

            // Still fits in the current page
            if ((usedPixels + itemWidth) <= availablePixels) {
                elements.push(item);
                item.setAttribute(PAGE_ATTRIBUTE, `${this.maxPages}`);
                usedPixels += itemWidth;
                continue;
            }

            // Would overflow, therefore finish the page and start a new one
            this.pagedElements.push(elements);
            this.maxPages++;
            elements = [item];
            item.setAttribute(PAGE_ATTRIBUTE, `${this.maxPages}`);
            usedPixels = itemWidth;
        }

        if (doAgain) {
            setTimeout(() => this.recreatePages(children));
            return;
        }

        // If the page has been filled up but not finished yet
        if (elements.length > 0) {
            this.pagedElements.push(elements);
        } else {
            // Correct the max page, as the last page has been started, but isn't actually used
            this.maxPages--;
        }

        this.ref.setAttribute(MAX_PAGES_ATTRIBUTE, `${this.maxPages}`);
        this.ref.classList.add(CLASS_STABLE);
        this.setPage(this.currentPage);
    }
}
