import {
    AfterContentChecked,
    AfterContentInit,
    ChangeDetectionStrategy,
    Component,
    ContentChildren,
    ElementRef,
    Input,
    OnDestroy,
    Optional,
    QueryList,
} from '@angular/core';
import { SplitViewContainerComponent } from '@gentics/ui-core';
import * as Masonry from 'masonry-layout';
import { Subscription, merge } from 'rxjs';
import { delay } from 'rxjs/operators';
import { MasonryItemDirective } from '../../directives/masonry-item/masonry-item.directive';

const gridAnimationDuration = 300;
const splitViewContainerAnimationDuration = 300;

/**
 * Creates a grid using Masonry.js (http://masonry.desandro.com/).
 *
 * Usage:
 * ```
 * <masonry-grid [gutter]="10"
 *               [columnWidth]="280">
 *
 *     <masonry-item *ngFor="let item of collection">
 *         <div><!-- the content --> </div>
 *     </masonry-item>
 *
 * </masonry-grid>
 * ```
 */
@Component({
    selector: 'masonry-grid',
    template: '<ng-content></ng-content>',
    styleUrls: ['./masonry-grid.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class MasonryGridComponent implements AfterContentInit, OnDestroy, AfterContentChecked {

    @Input() columnWidth: number;
    @Input() gutter = 0;

    masonryLayout: Masonry;
    subscriptions: Subscription[] = [];
    itemOutputSubscriptions = new Subscription();

    @ContentChildren(MasonryItemDirective, { descendants: true })
    items: QueryList<MasonryItemDirective>;

    private anyItemChangedInSize = false;

    constructor(
        private elementRef: ElementRef,
        @Optional()
        private splitViewContainer: SplitViewContainerComponent,
    ) { }

    ngAfterContentInit(): void {
        this.initializeMasonryGrid();
        this.triggerLayoutWhenItemsChange();
        this.triggerLayoutWhenSplitViewContainerIsResized();
        this.triggerLayoutWhenItemSizeChanges();
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
        this.masonryLayout.destroy();
        this.itemOutputSubscriptions.unsubscribe();
    }

    ngAfterContentChecked(): void {
        // Check if content children changed in size since last change detection cycle.
        // This is done here and not in the EventEmitter subscription to avoid repeated layouting.
        // Since this function is called after all children have been checked, it is sufficient to trigger layouting here once, if necessary.
        if (this.anyItemChangedInSize) {
            this.triggerLayout();
            this.anyItemChangedInSize = false;
        }
    }

    triggerLayout(): void {
        setTimeout(() => {
            const masonryLayout = this.masonryLayout;
            masonryLayout.reloadItems();
            (masonryLayout as any).options.transitionDuration = 0;
            masonryLayout.layout();
            (masonryLayout as any).options.transitionDuration = gridAnimationDuration;
        });
    }

    private initializeMasonryGrid(): void {
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.masonryLayout = new Masonry(this.elementRef.nativeElement, {
            itemSelector: 'masonry-item,[masonryItem]',
            columnWidth: Number(this.columnWidth),
            fitWidth: true,
            gutter: this.gutter,
            transitionDuration: gridAnimationDuration,
        });
    }

    private triggerLayoutWhenItemsChange(): void {
        const changeSub = this.items.changes
            .subscribe((changes) => this.triggerLayout());
        this.subscriptions.push(changeSub);
    }

    private triggerLayoutWhenSplitViewContainerIsResized(): void {
        const splitViewContainer = this.splitViewContainer;
        if (splitViewContainer) {
            const resizeSub = merge(
                splitViewContainer.splitDragEnd,
                splitViewContainer.rightPanelOpened,
                splitViewContainer.rightPanelClosed,
            ).pipe(
                delay(splitViewContainerAnimationDuration),
            ).subscribe(() => this.masonryLayout.layout());

            this.subscriptions.push(resizeSub);
        }
    }

    private triggerLayoutWhenItemSizeChanges(): void {
        const changeSub = this.items.changes.subscribe((items: MasonryItemDirective[]) => {
            this.itemOutputSubscriptions.unsubscribe();
            this.itemOutputSubscriptions = new Subscription();

            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            items.forEach((item) => {
                this.itemOutputSubscriptions.add(item.sizeChange.subscribe(() => {
                    this.anyItemChangedInSize = true;
                    this.triggerLayout();
                }));
            });
        });
        this.subscriptions.push(changeSub);
    }

}

