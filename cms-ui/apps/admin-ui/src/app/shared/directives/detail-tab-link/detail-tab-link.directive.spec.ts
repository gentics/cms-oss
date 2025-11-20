import { InterfaceOf } from '@admin-ui/common';
import { componentTest, configureComponentTest } from '@admin-ui/testing';
import { Component } from '@angular/core';
import { TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { DetailTabLinkDirective } from './detail-tab-link.directive';

@Component({
    template: `
        <gtx-tabs pure [activeId]="activeTabId">
            <gtx-tab id="tabA" gtxDetailTabLink>
                <div id="tabA-content">Tab A Content</div>
            </gtx-tab>
            <gtx-tab id="tabB" gtxDetailTabLink>
                <div id="tabB-content">Tab B Content</div>
            </gtx-tab>
            <gtx-tab id="tabC" gtxDetailTabLink>
                <div id="tabC-content">Tab C Content</div>
            </gtx-tab>
        </gtx-tabs>
    `,
    standalone: false,
})
class TestComponent {
    activeTabId = 'tabA';
}

class MockRouter implements Partial<InterfaceOf<Router>> {
    navigate = jasmine.createSpy('navigate').and.stub();
}

class MockActivatedRoute {
    mockRoute = 'test';
}

describe('DetailTabLinkDirective', () => {

    let router: MockRouter;
    let route: MockActivatedRoute;

    beforeEach(() => {
        configureComponentTest({
            declarations: [
                DetailTabLinkDirective,
                TestComponent,
            ],
            providers: [
                { provide: ActivatedRoute, useClass: MockActivatedRoute },
                { provide: Router, useClass: MockRouter },
            ],
        });

        router = TestBed.inject(Router) as any;
        route = TestBed.inject(ActivatedRoute) as any;
    });

    it('does not trigger a navigation upon initialization', componentTest(() => TestComponent, (fixture, instance) => {
        fixture.detectChanges();
        tick();

        expect(router.navigate).not.toHaveBeenCalled();
    }));

    it('triggers a navigation when a tab is clicked', componentTest(() => TestComponent, (fixture, instance) => {
        const destTabId = 'tabB';
        fixture.detectChanges();
        tick();
        fixture.detectChanges();
        tick();

        const tabLinks = fixture.debugElement.queryAll(By.css('.tab-links .tab-link a'));
        expect(tabLinks.length).toBe(3, 'Could not find the link tags of the gtx-tab elements. Did the gtx-tabs implementation change?');

        // Click the second tab.
        tabLinks[1].nativeElement.click();
        fixture.detectChanges();
        tick();

        expect(router.navigate).toHaveBeenCalledTimes(1);
        expect(router.navigate).toHaveBeenCalledWith([ '../', destTabId ], { relativeTo: route });
    }));

    // This test is first of all in the wrong component, as it's testing the functionality of the `gtx-tabs` component, rather than the directive.
    // Second of all, the `gtx-tabs` are seeminly completely broken in unit-tests as they simply don't render properly during tests.
    // Regular usage works fine and as expected
    // Thanks Angular for the non existent rendering and breaking this test.
    xit('does not trigger a navigation if the tab is changed using the activeId property', componentTest(() => TestComponent, (fixture, instance) => {
        const destTabId = 'tabB';
        fixture.detectChanges();
        tick();

        // Change the active tab from code (e.g., as the result of a navigation).
        instance.activeTabId = destTabId;
        fixture.detectChanges();
        tick();

        const destTabContent = fixture.debugElement.query(By.css(`#${destTabId}-content`));
        expect(destTabContent.parent.classes['is-active']).toBe(true, 'gtx-tabs did not activate the correct tab.');
        expect(router.navigate).not.toHaveBeenCalled();
    }));

});
