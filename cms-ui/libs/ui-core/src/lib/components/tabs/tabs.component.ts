import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ContentChildren,
    EventEmitter,
    HostBinding,
    Input,
    OnChanges,
    OnDestroy,
    Output,
    QueryList,
    SimpleChanges,
} from '@angular/core';
import { BehaviorSubject, Subscription, combineLatest } from 'rxjs';
import { distinctUntilChanged } from 'rxjs/operators';
import { coerceToBoolean } from '../../utils';
import { TabComponent } from '../tab/tab.component';

/**
 * Tabs can be either pure or stateful. Stateful tabs will keep track of which one is active by keeping an internal
 * state.
 *
 * Pure tabs will only change the active tab when the `activeId` property is updated.
 *
 * ## Stateful Tabs
 * ```html
 * <gtx-tabs (tabChange)="goToTab($event)">
 *     <gtx-tab title="Details">Optional content here.</gtx-tab>
 *     <gtx-tab title="Orders"></gtx-tab>
 *     <gtx-tab title="Notes"></gtx-tab>
 * </gtx-tabs>
 * ```
 *
 * ## Pure Tabs
 * ```html
 * <gtx-tabs pure [activeId]="activeTab">
 *     <gtx-tab title="Details" id="1" (select)="activeTab = $event"></gtx-tab>
 *     <gtx-tab title="Orders" id="2" (select)="activeTab = $event"></gtx-tab>
 *     <gtx-tab title="Notes" id="3" (select)="activeTab = $event"></gtx-tab>
 * </gtx-tabs>
 * ```
 *
 * ## With `routerLink`
 * A gtx-tab can take an optional `[routerLink]` binding which will set router links for the tabs.
 * ```html
 * <gtx-tabs pure [activeId]="activeTab">
 *     <gtx-tab title="Details" id="1" [routerLink]="['customer', 'details']"></gtx-tab>
 *     <gtx-tab title="Orders" id="2" [routerLink]="['customer', 'orders']"></gtx-tab>
 *     <gtx-tab title="Notes" id="3" [routerLink]="['customer', 'notes']"></gtx-tab>
 * </gtx-tabs>
 * ```
 *  * ##### Vertical Tabs
 * A gtx-tabs can take an optional `vertical` property which allows to display tabs vertically.
 * ```html
 * <gtx-tabs vertical>
 *        <gtx-tab title="Details"></gtx-tab>
 *        <gtx-tab title="Orders"></gtx-tab>
 *        <gtx-tab title="Notes"></gtx-tab>
 * </gtx-tabs>
 * ```
 *  *  * ##### Active Tabs with Icons
 * A gtx-tabs can take an optional `hideTitle` property which allows to hide the title for non-active tabs with icons.
 * ```html
 * <gtx-tabs hideTitle>
 *           <gtx-tab icon="folder" title="Tab 1">Tab 1 Content</gtx-tab>
 *           <gtx-tab icon="cloud" title="Tab 2">Tab 2 Content</gtx-tab>
 * </gtx-tabs>
 * ```
 *
 */
@Component({
    selector: 'gtx-tabs',
    templateUrl: './tabs.component.html',
    styleUrls: ['./tabs.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TabsComponent implements AfterViewInit, OnChanges, OnDestroy {

    /**
     * When present (or set to true), tabs are displayed vertically.
     */
    @Input()
    @HostBinding('class.vertical')
    public vertical = false;

    /**
     * When present (or set to true), only active tabs with icons will show the title.
     * Non-active tabs with icons will hide the title, show only icon.
     */
    @Input()
    public hideTitle = false;

    /**
     * The id of the active tab. Should only be used in pure (stateless) mode.
     */
    @Input()
    public activeId: string;

    /**
     * When present, sets the tabs to pure (stateless) mode.
     */
    @Input()
    public pure = false;

    /**
     * When present (or set to true), tabs which do not fit into the width of their container will wrap onto
     * a new line. Otherwise, tabs will remain on one line and the contents will be elided if all the available
     * space is filled.
     */
    @Input()
    public wrap = false;

    /**
     * Fires an event whenever the active tab changes. Argument is the id of the selected tab.
     */
    @Output()
    public tabChange = new EventEmitter<string>();

    @ContentChildren(TabComponent)
    protected tabs: QueryList<TabComponent>;

    public displayTabs: TabComponent[] = [];

    protected activeTabId$ = new BehaviorSubject<string>(null);

    protected subscriptions: Subscription[] = [];

    constructor(
        protected changeDetector: ChangeDetectorRef,
    ) { }

    ngAfterViewInit(): void {
        let initial = true;

        this.subscriptions.push(combineLatest([
            this.activeTabId$.asObservable().pipe(
                distinctUntilChanged(),
            ),
            this.tabs.changes,
        ]).subscribe(([id, tabs]: [string, QueryList<TabComponent>]) => {
            let hasSet = false;

            // In a timeout, so it is performing this a angular tick later, to prevent the annoying
            // changed after checked error.
            setTimeout(() => {
                if (tabs != null && tabs.length > 0) {
                    tabs.forEach(singleTab => {
                        singleTab.parentRef = this;
                        singleTab.active = id === singleTab.id;
                        singleTab.changeDetector.markForCheck();
                        hasSet = hasSet || singleTab.active;
                    });

                    if (initial && !hasSet && !this.pure) {
                        const first = tabs.first;
                        first.active = true;
                        first.changeDetector.markForCheck();
                        this.activeId = first.id;
                        this.activeTabId$.next(first.id);
                    }

                    initial = false;
                }

                this.changeDetector.markForCheck();
            });

            this.updateDisplayTabs();
            this.changeDetector.markForCheck();
        }));

        this.tabs.notifyOnChanges();
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.activeId) {
            this.activeTabId$.next(this.activeId);
        }

        if (changes.vertical) {
            this.vertical = coerceToBoolean(this.vertical);
        }

        if (changes.wrap) {
            this.wrap = coerceToBoolean(this.wrap);
        }

        if (changes.hideTitle) {
            this.hideTitle = coerceToBoolean(this.hideTitle);
        }

        if (changes.pure) {
            this.pure = coerceToBoolean(this.pure);
        }
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    identifyTab(idx: number, tab: TabComponent): string {
        return tab.id ?? `${idx}`;
    }

    /**
     * Invoked when a tab link is clicked.
     */
    selectTab(tab: TabComponent): void {
        if (tab.disabled) {
            return;
        }
        if (!this.pure) {
            this.setAsActive(tab);
            this.tabChange.emit(tab.id);
        } else {
            tab.select.emit(tab.id);
        }
    }

    public updateDisplayTabs(): void {
        this.displayTabs = this.tabs.toArray();
        this.changeDetector.markForCheck();
    }

    private setAsActive(tab: TabComponent): void {
        this.tabs.toArray().forEach(tab => tab.active = false);
        tab.active = true;
        this.activeTabId$.next(tab.id);
    }

}
