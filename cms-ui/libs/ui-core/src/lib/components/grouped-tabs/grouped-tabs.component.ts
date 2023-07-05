import {
    AfterContentInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ContentChildren,
    ElementRef,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    QueryList,
    SimpleChanges,
} from '@angular/core';
import { BehaviorSubject, combineLatest, ObjectUnsubscribedError } from 'rxjs';
import { debounceTime, startWith, switchMap } from 'rxjs/operators';
import { BaseComponent } from '../base-component/base.component';
import { TabGroupComponent } from '../tab-group/tab-group.component';
import { TabPaneComponent } from '../tab-pane/tab-pane.component';

let uniqueGroupedTabsId = 0;

/**
 * GroupedTabs supports tabs either with and without groups.
 *
 * Pure tabs will only change the active tab when the `activeId` property is updated.
 *
 * ## Tabs with simple labels
 * ```html
 * <gtx-grouped-tabs>
 *      <gtx-tab-pane label="First without Group">Content 1</gtx-tab-pane>
 *      <gtx-tab-group label="1st Group name" expanded="true">
 *          <gtx-tab-pane label="First">Content 2</gtx-tab-pane>
 *          <gtx-tab-pane label="Second">Content 3</gtx-tab-pane>
 *      </gtx-tab-group>
 *      <gtx-tab-group label="2nd Group name">
 *          <gtx-tab-pane label="First">Content 4</gtx-tab-pane>
 *          <gtx-tab-pane label="Second">Content 5</gtx-tab-pane>
 *      </gtx-tab-group>
 * </gtx-grouped-tabs>
 * ```
 *
 * ## Tabs with template labels
 * ```html
 * <gtx-grouped-tabs>
 *      <gtx-tab-pane>
 *          <ng-template gtx-tab-label>First without Group</ng-template>
 *          Implicit Content 1
 *      </gtx-tab-pane>
 *      <gtx-tab-group expanded="true">
 *          <ng-template gtx-tab-label>
 *              <icon>add</icon> 1st Group name
 *          </ng-template>
 *          <gtx-tab-pane>
 *              <ng-template gtx-tab-label>First</ng-template>
 *              <ng-template gtx-tab-content>
 *                  Content 2
 *              </ng-template>
 *          </gtx-tab-pane>
 *          <gtx-tab-pane label="Second">Content 3</gtx-tab-pane>
 *      </gtx-tab-group>
 *      <gtx-tab-group label="2nd Group name">
 *          <gtx-tab-pane label="First">Content 4</gtx-tab-pane>
 *          <gtx-tab-pane label="Second">Content 5</gtx-tab-pane>
 *      </gtx-tab-group>
 * </gtx-grouped-tabs>
 * ```
 *
 * ## Export components to use in templates
 * ```html
 * <gtx-grouped-tabs #groupedTabs="gtxGroupedTabs">
 *      <gtx-tab-pane label="First" #tab1>First content</gtx-tab-pane>
 *      <gtx-tab-pane label="Second">
 *          Seconds content
 *          <gtx-button (click)="groupedTabs.selectTab(tab1)">Switch to Tab 1</gtx-button>
 *      </gtx-tab-pane>
 * </gtx-grouped-tabs>
 * ```
 *
 */
@Component({
    selector: 'gtx-grouped-tabs',
    exportAs: 'gtxGroupedTabs',
    templateUrl: './grouped-tabs.component.html',
    styleUrls: ['./grouped-tabs.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GroupedTabsComponent
    extends BaseComponent
    implements AfterContentInit, OnChanges {

    /** Unique id for this input. */
    public readonly UNIQUE_ID = `gtx-grouped-tabs-${uniqueGroupedTabsId++}`;

    /**
     * The id of the active tab. Should only be used in pure (stateless) mode.
     */
    @Input()
    public activeId: string;

    /** Id of the grouped-tabs to differentiate between multiple instances. */
    @Input()
    public id: string = null;

    /**
     * When present, sets the tabs to pure (stateless) mode.
     */
    @Input()
    public pure = false;

    /**
     * When present (or set to true), tabs title will wrap onto a new line. Otherwise, tabs will remain on one line
     * and the contents will be elided if all the available space is filled.
     */
    @Input()
    public wrap = false;

    /**
     * If it should display the status-icons of the tab-panes.
     */
    @Input()
    public statusIcons = false;

    /**
     * Fires an event whenever the active tab changes. Argument is the id of the selected tab.
     */
    @Output()
    public tabChange = new EventEmitter<string>();

    /** All of the defined tab panes. */
    @ContentChildren(TabPaneComponent, { descendants: true })
    public tabPanes: QueryList<TabPaneComponent>;

    /** All of the defined groups of tab panes. */
    @ContentChildren(TabGroupComponent)
    public tabGroups: QueryList<TabGroupComponent>;

    /** The tabs/tab-groups of this component */
    public tabs$ = new BehaviorSubject<Array<TabPaneComponent | TabGroupComponent>>([]);

    constructor(
        changeDetector: ChangeDetectorRef,
        private elementRef: ElementRef,
    ) {
        super(changeDetector);
        this.booleanInputs.push('pure', 'wrap', 'statusIcons');
    }

    ngAfterContentInit(): void {
        const tabChanges = combineLatest([
            this.tabPanes.changes,
            this.tabGroups.changes,
        ]).pipe(
            switchMap(([tabPanes, tabGroups]: [QueryList<TabPaneComponent>, QueryList<TabGroupComponent>]) => {
                const allChanges = [
                    tabPanes.changes.pipe(startWith(tabPanes)),
                    tabGroups.changes.pipe(startWith(tabGroups)),
                ];

                tabGroups.map((group) => {
                    try {
                        group.tabs.notifyOnChanges();
                        allChanges.push(group.tabs.changes.pipe(startWith(group.tabs)));
                    } catch (e) {
                        if (e instanceof ObjectUnsubscribedError) {
                        // To prevent Unsubscribe error
                        } else {
                            throw e;
                        }
                    }
                });

                return combineLatest(allChanges);
            }),
            debounceTime(5),
        );

        this.subscriptions.push(tabChanges.subscribe(() => {
            this.collectTabs(true);
        }));

        this.tabPanes.notifyOnChanges();
        this.tabGroups.notifyOnChanges();
        this.collectTabs(true);
    }

    ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.pure || changes.activeId) {
            this.setActiveTab();
        }
    }

    get currentTab(): TabPaneComponent {
        return this.tabPanes.filter(tab => tab.active === true)[0];
    }

    collectTabs(activate: boolean = false): void {
        const tabs = Array<TabPaneComponent | TabGroupComponent>();

        // Collect all the available tabs and groups
        this.tabPanes.map(item => {
            const tabGroup = this.tabGroups.find(group => group.tabs.some(tab => tab === item));
            if (tabGroup !== undefined) {
                if (tabs.indexOf(tabGroup) === -1) {
                    tabs.push(tabGroup);
                }
            } else {
                tabs.push(item);
            }
        });

        // Activates the first tab if there are no active currently
        if (activate) {
            this.preActivateTab();
        }
        this.tabs$.next(tabs);
    }

    preActivateTab(): void {
        if (this.pure) {
            this.setActiveTab();
            return;
        }
        const activeTabs = this.tabPanes.filter(tab => tab.active);

        // if there is no active tab set, activate the first
        if (activeTabs.length === 0) {
            this.setAsActive(this.tabPanes.first);
        }
    }

    /**
     * Sets the tab with id === this.activeId to active.
     */
    setActiveTab(): void {
        if (this.tabPanes) {
            const tabToActivate = this.tabPanes.filter(t => t.id === this.activeId)[0];
            this.setAsActive(tabToActivate);
        }
    }

    /**
     * Invoked when a tab link is clicked.
     */
    selectTab(tab: TabPaneComponent): void {
        if (tab.disabled) {
            return;
        }
        if (!this.pure) {
            this.setAsActive(tab);
        }
        tab.select.emit(tab.id);
        this.tabChange.emit(tab.id);
    }

    /**
     * Toggle TabGroup open/close state.
     */
    toggleGroup(group: TabGroupComponent): void {
        group.toggle();
    }

    /**
     * Calculates TabGroup body height to to make it correctly animateable.
     */
    tabsHeight(group: TabGroupComponent): number {
        if (this.elementRef && group.expand) {
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            const body: HTMLOListElement = this.elementRef.nativeElement.querySelector(`li#${group.uniqueId} div.collapsible-body > ul`);

            if (body) {
                // eslint-disable-next-line @typescript-eslint/restrict-plus-operands
                return body.getBoundingClientRect().height + 30;
            }
        }

        return 0;
    }

    public setAsActive(tabToActivate?: TabPaneComponent): void {
        this.tabPanes.toArray().forEach(tab => {
            if (tabToActivate != null) {
                tab.active = tab.id == null && tabToActivate.id == null
                    ? tab === tabToActivate
                    : tab.id === tabToActivate.id;
            } else {
                tab.active = false;
            }
            tab.changeDetector.markForCheck();
        });

        if (tabToActivate != null) {
            this.tabGroups.map(group => {
                if (group.tabs.some(currentTab => currentTab.id == null && tabToActivate.id == null
                    ? currentTab === tabToActivate
                    : currentTab.id === tabToActivate.id,
                )) {
                    group.expand = true;
                }
            });
        }

        this.changeDetector.markForCheck();

        this.collectTabs();
    }
}
