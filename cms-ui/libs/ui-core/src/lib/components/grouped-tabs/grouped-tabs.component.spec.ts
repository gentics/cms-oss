import { Component, ElementRef, NO_ERRORS_SCHEMA, ViewChild } from '@angular/core';
import { TestBed, tick } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { IconDirective } from '../../directives/icon/icon.directive';
import { TabContentDirective } from '../../directives/tab-content/tab-content.directive';
import { TabLabelDirective } from '../../directives/tab-label/tab-label.directive';
import { componentTest } from '../../testing';
import { TabGroupComponent } from '../tab-group/tab-group.component';
import { TabPaneComponent } from '../tab-pane/tab-pane.component';
import { GroupedTabsComponent } from './grouped-tabs.component';

export class MockElementRef extends ElementRef {
    constructor() { super(null); }
}

describe('GroupedTabsComponent', () => {

    beforeEach(() => TestBed.configureTestingModule({
        providers: [
            { provide: ElementRef, useClass: MockElementRef },
        ],
        declarations: [
            IconDirective,
            GroupedTabsComponent,
            TabGroupComponent,
            TabPaneComponent,
            TabLabelDirective,
            TabContentDirective,
            TestComponent,
        ],
        teardown: { destroyAfterEach: false },
        schemas: [NO_ERRORS_SCHEMA],
    }));

    it('is created ok',
        componentTest(() => TestComponent, fixture => {
            expect(fixture).toBeDefined();
        }),
    );

    describe('tabs', () => {
        it('tab panes created correctly',
            componentTest(() => TestComponent,
                `<gtx-grouped-tabs>
                    <gtx-tab-pane label="Test Label">Test Content</gtx-tab-pane>
                    <gtx-tab-pane label="Test Label 2">Second Test Content</gtx-tab-pane>
                </gtx-grouped-tabs>`,
                fixture => {
                    let tabId = 0;
                    const testLabels = ['Test Label', 'Test Label 2'];
                    const testContents = ['Test Content', 'Second Test Content'];

                    // Check if everything created as assumed
                    fixture.detectChanges();
                    const tabs = fixture.componentInstance.groupedTabs.tabPanes.toArray() ;
                    const tabGroups = fixture.componentInstance.groupedTabs.tabGroups.toArray() ;

                    expect(tabGroups.length).toBe(0);

                    const tabContent: HTMLDivElement = fixture.nativeElement.querySelector('div.grouped-tab-content');
                    expect(tabContent.textContent).toEqual(testContents[tabId]);
                    checkTabPaneLabels(fixture, tabId, testLabels);

                    // Switch to the second tab
                    tabId = 1;
                    fixture.componentInstance.groupedTabs.selectTab(tabs[tabId]);
                    // Check if everything re-rendered as assumed
                    fixture.detectChanges();
                    tick();

                    expect(tabContent.textContent).toEqual(testContents[tabId]);
                    checkTabPaneLabels(fixture, tabId, testLabels);
                }),
        );

        it('tab panes with groups created correctly',
            componentTest(() => TestComponent,
                `<gtx-grouped-tabs>
                    <gtx-tab-pane label="Test Label">Test Content</gtx-tab-pane>
                    <gtx-tab-group label="Test Group">
                        <gtx-tab-pane label="Test Label 2">Second Test Content</gtx-tab-pane>
                        <gtx-tab-pane label="Test Label 3">Third Test Content</gtx-tab-pane>
                    </gtx-tab-group>
                </gtx-grouped-tabs>`,
                fixture => {
                    let tabId = 0;
                    let groupId = 0;
                    const testLabels = ['Test Label', 'Test Label 2', 'Test Label 3'];
                    const testContents = ['Test Content', 'Second Test Content', 'Third Test Content'];
                    const collectedTabs = fixture.componentInstance.groupedTabs.tabs$;

                    // Check if everything created as assumed
                    fixture.detectChanges();
                    const tabs = fixture.componentInstance.groupedTabs.tabPanes.toArray() ;
                    const tabGroups = fixture.componentInstance.groupedTabs.tabGroups.toArray() ;

                    expect(tabGroups.length).toBe(1);

                    const tabContent: HTMLDivElement = fixture.nativeElement.querySelector('div.grouped-tab-content');
                    expect(tabContent.textContent).toEqual(testContents[tabId]);
                    checkTabPaneLabels(fixture, tabId, testLabels);

                    // Switch to the third tab in first group
                    tabId = 2;
                    groupId = 1;

                    expect((collectedTabs.value[groupId] as TabGroupComponent).hasActiveChild).toBeFalsy();

                    fixture.componentInstance.groupedTabs.selectTab(tabs[tabId]);

                    // Check if everything re-rendered as assumed
                    fixture.detectChanges();
                    tick();

                    expect(tabContent.textContent).toEqual(testContents[tabId]);
                    expect((collectedTabs.value[groupId] as TabGroupComponent).hasActiveChild).toBeTruthy();
                    checkTabPaneLabels(fixture, tabId, testLabels);
                }),
        );

        it('tabs are rendered in correct order',
            componentTest(() => TestComponent,
                `<gtx-grouped-tabs>
                    <gtx-tab-pane label="Test Label">Test Content</gtx-tab-pane>
                    <gtx-tab-group label="Test Group">
                        <gtx-tab-pane label="Test Label 2">Second Test Content</gtx-tab-pane>
                        <gtx-tab-pane label="Test Label 3">Third Test Content</gtx-tab-pane>
                    </gtx-tab-group>
                    <gtx-tab-pane label="Test Label 4">Test Content 4</gtx-tab-pane>
                    <gtx-tab-group label="Test Group 2">
                        <gtx-tab-pane label="Test Label 5">Second Test Content 2</gtx-tab-pane>
                        <gtx-tab-pane label="Test Label 6">Third Test Content 2</gtx-tab-pane>
                    </gtx-tab-group>
                </gtx-grouped-tabs>`,
                fixture => {
                    const testLabels = ['Test Label', 'Test Label 2', 'Test Label 3', 'Test Label 4', 'Test Label 5', 'Test Label 6'];
                    const testContents = [
                        'Test Content',
                        'Second Test Content',
                        'Third Test Content',
                        'Test Content 4',
                        'Second Test Content 2',
                        'Third Test Content 2',
                    ];
                    const collectedTabs = fixture.componentInstance.groupedTabs.tabs$;

                    // Check if everything created as assumed
                    fixture.detectChanges();
                    const tabs = fixture.componentInstance.groupedTabs.tabPanes.toArray() ;
                    const tabGroups = fixture.componentInstance.groupedTabs.tabGroups.toArray() ;

                    expect(tabGroups.length).toBe(2);
                    expect(tabs.length).toBe(6);
                    expect(collectedTabs.value.length).toBe(4);
                    expect(collectedTabs.value[0] instanceof TabPaneComponent).toBeTruthy();
                    expect(collectedTabs.value[1] instanceof TabGroupComponent).toBeTruthy();
                    expect(collectedTabs.value[2] instanceof TabPaneComponent).toBeTruthy();
                    expect(collectedTabs.value[3] instanceof TabGroupComponent).toBeTruthy();

                    const tabContent: HTMLDivElement = fixture.nativeElement.querySelector('div.grouped-tab-content');

                    const checkTab = (tabId, groupId?) => {
                        fixture.componentInstance.groupedTabs.selectTab(tabs[tabId]);
                        fixture.detectChanges();
                        expect(tabContent.textContent).toEqual(testContents[tabId]);

                        if (groupId !== undefined) {
                            expect((collectedTabs.value[groupId] as TabGroupComponent).hasActiveChild).toBeTruthy();
                        }

                        checkTabPaneLabels(fixture, tabId, testLabels);
                    }

                    collectedTabs.value.forEach((item, index) => {
                        if (item instanceof TabGroupComponent) {
                            item.tabs.forEach((gitem, gindex) => {
                                const tabId = tabs.findIndex((tab) => tab === gitem);
                                checkTab(tabId);
                            });
                        } else {
                            const tabId = tabs.findIndex((tab) => tab === item);
                            checkTab(tabId);
                        }
                    });
                }),
        );

        it('add tabs asynchronously with templates',
            componentTest(() => TestComponent,
                `<gtx-grouped-tabs>
                    <gtx-tab-pane>
                        <ng-template gtx-tab-label>Test Label</ng-template>
                        <ng-template gtx-tab-content>Test Content</ng-template>
                    </gtx-tab-pane>
                    <gtx-tab-group *ngFor="let tabGroup of asyncTabs$ | async">
                        <ng-template gtx-tab-label>{{ tabGroup.label }}</ng-template>
                        <gtx-tab-pane *ngFor="let tab of tabGroup.tabs">
                            <ng-template gtx-tab-label>{{ tab.label }}</ng-template>
                            <ng-template gtx-tab-content>{{ tab.content }}</ng-template>
                        </gtx-tab-pane>
                    </gtx-tab-group>
                </gtx-grouped-tabs>`,
                fixture => {
                    const testLabelsBase = ['Test Label'];
                    let testLabels: string[] = [];
                    const testContents = ['Test Content'];
                    const collectedTabs = fixture.componentInstance.groupedTabs.tabs$;
                    const testAsync = [{
                        label: 'Test Async Group 1', tabs: [
                            { label: 'Test Async Tab 1', content: 'Test Async Tab 1 Content' },
                            { label: 'Test Async Tab 2', content: 'Test Async Tab 2 Content' },
                        ],
                    },
                    {
                        label: 'Test Async Group 2', tabs: [
                            { label: 'Test Async Tab 3', content: 'Test Async Tab 3 Content' },
                            { label: 'Test Async Tab 4', content: 'Test Async Tab 4 Content' },
                            { label: 'Test Async Tab 5', content: 'Test Async Tab 5 Content' },
                        ],
                    }];

                    // Check if everything created as assumed
                    fixture.detectChanges();
                    let tabs = fixture.componentInstance.groupedTabs.tabPanes.toArray() ;
                    let tabGroups = fixture.componentInstance.groupedTabs.tabGroups.toArray() ;
                    const tabContent: HTMLDivElement = fixture.nativeElement.querySelector('div.grouped-tab-content');

                    expect(tabGroups.length).toBe(0);
                    expect(tabs.length).toBe(1);
                    expect(collectedTabs.value.length).toBe(1);

                    const checkTab = (tabId, groupId?, tabItem?) => {
                        fixture.componentInstance.groupedTabs.selectTab(tabs[tabId]);
                        fixture.detectChanges();
                        if (tabId === 0) {
                            expect(tabContent.textContent).toEqual(testContents[tabId]);
                        } else {
                            expect(tabContent.textContent).toEqual(tabItem.content);
                        }

                        if (groupId !== undefined) {
                            expect((collectedTabs.value[groupId] as TabGroupComponent).hasActiveChild).toBeTruthy();
                        }

                        checkTabPaneLabels(fixture, tabId, testLabels);
                    }

                    fixture.componentInstance.asyncTabs$.subscribe((item) => {
                        fixture.detectChanges();
                        // Same value as debounceTime in grouped-tabs.component.ts
                        tick(5);

                        tabs = fixture.componentInstance.groupedTabs.tabPanes.toArray() ;
                        tabGroups = fixture.componentInstance.groupedTabs.tabGroups.toArray() ;
                        expect(tabGroups.length).toBe(item.length);
                        expect(tabs.length).toBe(item.map(i => i.tabs.length).reduce<number>((acc: number, val: number) => acc + val, 0) + 1);
                        expect(collectedTabs.value.length).toBe(item.length + 1);

                        // Collect all testable labels
                        const itemLabels: string[] = item.map(group => {
                            return group.tabs.map(groupItem => groupItem.label);
                        })
                            .reduce((acc, val) => acc.concat(val), []);

                        testLabels = [...testLabelsBase, ...itemLabels];

                        collectedTabs.value.forEach((citem, index) => {
                            if (citem instanceof TabGroupComponent) {
                                citem.tabs.forEach((gitem, gindex) => {
                                    const tabId = tabs.findIndex((tab) => tab === gitem);
                                    checkTab(tabId, index, item[index - 1].tabs[gindex]);
                                });
                            } else {
                                const tabId = tabs.findIndex((tab) => tab === citem);
                                checkTab(tabId);
                            }
                        })
                    });

                    testAsync.forEach((item) => {
                        fixture.componentInstance.asyncTabs$.next([item]);
                    });

                    fixture.componentInstance.asyncTabs$.next(testAsync);
                }),
        );
    });
});

@Component({
    template: '<gtx-grouped-tabs></gtx-grouped-tabs>',
})
class TestComponent {
    @ViewChild(GroupedTabsComponent, { static: true })
    groupedTabs: GroupedTabsComponent;

    asyncTabs$ = new Subject<Array<any>>();
}

function checkTabPaneLabels(fixture, tabId, testLabels): void {
    const tabPanesLabels: HTMLLIElement[] = fixture.nativeElement.querySelectorAll('li.tab-link');

    tabPanesLabels.forEach((label, index: number) => {
        // expect(tabPanesLabels[index].classList.contains('is-active')).toEqual(tabId === index);
        expect(label.textContent)
            .withContext(`Tab Index: ${index}`)
            .toEqual(testLabels[index]);
    });
}
