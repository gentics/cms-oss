import { debounce } from '../../common/utils/debounce';

export const CLASS_OVERFLOW_TARGET = 'overflow-target';
export const CLASS_OVERFLOW_STABLE = 'stable';
export const CLASS_OVERFLOW_OVERFLOWING = 'overflowing';
export const CLASS_OVERFLOW_ELEMENT_OVERFLOWING = 'is-overflowing-element';

export class OverflowManager {

    protected target: Element | null = null;
    protected isWorking = false;
    protected observer: ResizeObserver;

    constructor(
        protected ref: Element,
    ) { }

    init(): void {
        this.target = this.ref.querySelector(`.${CLASS_OVERFLOW_TARGET}`);

        const debouncedWidthChecker = debounce<void>(() => this.checkWidth(), 50);
        this.observer = new ResizeObserver(() => {
            debouncedWidthChecker();
        });
        this.observer.observe(this.ref);
    }

    destroy(): void {
        if (this.observer) {
            this.observer.disconnect();
            this.observer = null;
        }
    }

    checkWidth(): void {
        if (this.isWorking) {
            return;
        }

        this.isWorking = true;
        // Make all items visible to determine overflow
        this.ref.classList.remove(CLASS_OVERFLOW_STABLE, CLASS_OVERFLOW_OVERFLOWING);
        for (const item of Array.from(this.ref.children)) {
            item.classList.remove(CLASS_OVERFLOW_ELEMENT_OVERFLOWING);
        }

        const targetWidth = Math.ceil(this.target?.getBoundingClientRect?.()?.width ?? 0);
        const availablePixels = Math.floor(this.ref.getBoundingClientRect().width);
        // Remove the target width for this check, as it's usually not needed
        const requiredPixels = Math.ceil(this.ref.scrollWidth) - targetWidth;

        // If no item is overflowing, then we don't need to handle anything
        if (requiredPixels <= availablePixels) {
            this.ref.classList.add(CLASS_OVERFLOW_STABLE);
            this.isWorking = false;
            return;
        }

        const overflowingChildren: Element[] = [];
        const furthestRight = Math.floor(this.ref.getBoundingClientRect().right);
        let lastVisibleElement: Element | null = null;

        for (const item of Array.from(this.ref.children)) {
            const itemRight = Math.ceil(item.getBoundingClientRect().right);

            if (item === this.target) {
                continue;
            }

            // Handle overflowing items
            if (itemRight + targetWidth > furthestRight) {
                overflowingChildren.push(item);
                continue;
            }

            lastVisibleElement = item;
        }

        // Edge case: when only one item would overflow, but it's only doing so because of the
        // overflow target, then we check if it would fit without it. If it does, then we're all good.
        if (overflowingChildren.length === 1
            && (Math.ceil(overflowingChildren[0].getBoundingClientRect().right) - targetWidth <= furthestRight)
        ) {
            this.ref.classList.add(CLASS_OVERFLOW_STABLE);
            this.isWorking = false;
            return;
        }

        for (const item of overflowingChildren) {
            item.classList.add(CLASS_OVERFLOW_ELEMENT_OVERFLOWING);
        }

        let doesOverflow = overflowingChildren.length > 0;

        // Another edge case: The calculations for the last item overflow aren't working as expected sometimes,
        // as it doesn't properly add the size of the target for some reason.
        if (this.ref.scrollWidth > Math.ceil(this.ref.getBoundingClientRect().width) && lastVisibleElement != null) {
            lastVisibleElement.classList.add(CLASS_OVERFLOW_ELEMENT_OVERFLOWING);
            doesOverflow = true;
        }

        this.ref.classList.add(CLASS_OVERFLOW_STABLE);
        if (doesOverflow) {
            this.ref.classList.add(CLASS_OVERFLOW_OVERFLOWING);
        }

        this.isWorking = false;
    }

}
