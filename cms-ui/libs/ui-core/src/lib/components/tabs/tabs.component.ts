import {
    AfterContentInit,
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
import { Subscription } from 'rxjs';
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
})
export class TabsComponent implements AfterContentInit, OnChanges, OnDestroy {

    /**
     * When present (or set to true), tabs are displayed vertically.
     */
    @Input()
    set vertical(val: any) {
        this.verticalTabs = coerceToBoolean(val);
    }

    /**
     * When present (or set to true), only active tabs with icons will show the title.
     * Non-active tabs with icons will hide the title, show only icon.
     */
    @Input()
    set hideTitle(val: any) {
        this.shouldHideTitle = coerceToBoolean(val);
    }

    /**
     * The id of the active tab. Should only be used in pure (stateless) mode.
     */
    @Input()
    activeId: string;

    /**
     * When present, sets the tabs to pure (stateless) mode.
     */
    @Input()
    set pure(val: any) {
        this.isPure = val != null;
    }

    /**
     * When present (or set to true), tabs which do not fit into the width of their container will wrap onto
     * a new line. Otherwise, tabs will remain on one line and the contents will be elided if all the available
     * space is filled.
     */
    @Input()
    set wrap(val: any) {
        this.tabsShouldWrap = coerceToBoolean(val);
    }

    /**
     * Fires an event whenever the active tab changes. Argument is the id of the selected tab.
     */
    @Output()
    tabChange = new EventEmitter<string>();

    @ContentChildren(TabComponent)
    tabs: QueryList<TabComponent>;

    @HostBinding('class.vertical')
    verticalTabs = false;
    shouldHideTitle = false;
    tabsShouldWrap = false;

    private isPure = false;

    tabsChangeSubscription: Subscription;

    ngAfterContentInit(): void {
        if (this.isPure) {
            this.tabsChangeSubscription = this.tabs.changes.subscribe(() => {
                setTimeout(() => this.setActiveTab());
            });
        } else {
            let activeTabs = this.tabs.filter(tab => tab.active);

            // if there is no active tab set, activate the first
            if (activeTabs.length === 0) {
                this.tabs.first.active = true;
            }
        }
        this.tabs.notifyOnChanges();
    }

    ngOnChanges(changes: SimpleChanges): void {
        this.setActiveTab();
    }

    ngOnDestroy(): void {
        if (this.tabsChangeSubscription) {
            this.tabsChangeSubscription.unsubscribe();
        }
    }

    /**
     * Sets the tab with id === this.activeId to active.
     */
    setActiveTab(): void {
        if (this.tabs) {
            let tabToActivate = this.tabs.filter(t => t.id === this.activeId)[0];
            if (tabToActivate) {
                this.setAsActive(tabToActivate);
            }
        }
    }

    /**
     * Invoked when a tab link is clicked.
     */
    selectTab(tab: TabComponent): void {
        if (tab.disabled) {
            return;
        }
        if (!this.isPure) {
            this.setAsActive(tab);
            this.tabChange.emit(tab.id);
        } else {
            tab.select.emit(tab.id);
        }
    }

    private setAsActive(tab: TabComponent): void {
        this.tabs.toArray().forEach(tab => tab.active = false);
        tab.active = true;
    }

}
