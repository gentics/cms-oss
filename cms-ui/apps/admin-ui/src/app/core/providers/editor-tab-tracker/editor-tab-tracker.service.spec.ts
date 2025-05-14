import { ObservableStopper } from '@admin-ui/common';
import { AppStateService, SwitchEditorTab } from '@admin-ui/state';
import { assembleTestAppStateImports, TestAppState, TrackedActions } from '@admin-ui/state/utils/test-app-state';
import { componentTest, subscribeSafely } from '@admin-ui/testing';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { ComponentFixture, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Route, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { ofActionDispatched } from '@ngxs/store';
import { EDITOR_TAB, EditorTabTrackerService } from './editor-tab-tracker.service';

@Component({
    template: '<router-outlet></router-outlet>',
    standalone: false,
})
class TestComponent {}

@Component({
    selector: 'gtx-test-editor',
    template: '',
    standalone: false,
})
class TestEditorComponent implements OnInit, OnDestroy {

    tabChangedSpy = jasmine.createSpy('tabChangedSpy').and.stub();
    private stopper = new ObservableStopper();

    constructor(
        public editorTabTracker: EditorTabTrackerService,
        private route: ActivatedRoute,
    ) {}

    ngOnInit(): void {
        subscribeSafely(
            this.editorTabTracker.trackEditorTab(this.route),
            this.stopper,
            this.tabChangedSpy,
        );
    }

    ngOnDestroy(): void {
        this.stopper.stop();
    }
}

const ROUTES: Route[] = [
    {
        path: `edit/:id/:${EDITOR_TAB}`,
        component: TestEditorComponent,
    },
];

describe('EditorTabTrackerService', () => {

    let appState: TestAppState;
    let router: Router;
    let switchTabActions: TrackedActions<SwitchEditorTab>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                RouterTestingModule.withRoutes(ROUTES),
                assembleTestAppStateImports(),
            ],
            declarations: [
                TestComponent,
                TestEditorComponent,
            ],
            providers: [
                EditorTabTrackerService,
                TestAppState,
                { provide: AppStateService, useExisting: TestAppState },
            ],
        });

        appState = TestBed.inject(TestAppState);
        router = TestBed.inject(Router);
        const filterSpy = jasmine.createSpy('ofActionDispatched').and.callFake((...allowedTypes: any[]) => ofActionDispatched(...allowedTypes));

        switchTabActions = appState.trackActionsAuto(filterSpy, SwitchEditorTab);
    });

    function findEditorComponent(fixture: ComponentFixture<TestComponent>): TestEditorComponent {
        const editorElement = fixture.debugElement.query(By.directive(TestEditorComponent));
        expect(editorElement).toBeTruthy('Could not find TestEditorComponent in DOM.');
        return editorElement.componentInstance;
    }

    function navigateToTab(tabId: string, fixture: ComponentFixture<TestComponent>): void {
        // We need to execute the navigation inside an NgZone, otherwise we get a warning on the console.
        fixture.ngZone.run(() => router.navigateByUrl(`/edit/1/${tabId}`));
        tick();
        fixture.detectChanges();
        tick();
    }

    it('supplies the initial EDITOR_TAB parameter', componentTest(() => TestComponent, (fixture, instance) => {
        const expectedTab = 'properties';
        navigateToTab(expectedTab, fixture);

        expect(switchTabActions.count).toBe(1, 'SwitchEditorTab action was not dispatched the expected number of times.');
        expect(switchTabActions.get(0).tabId).toEqual(expectedTab);

        const editorComponent = findEditorComponent(fixture);
        expect(editorComponent.tabChangedSpy).toHaveBeenCalledTimes(1);
        expect(editorComponent.tabChangedSpy).toHaveBeenCalledWith(expectedTab);
    }));

    it('supplies the EDITOR_TAB parameter on subsequent navigations', componentTest(() => TestComponent, (fixture, instance) => {
        const expectedTabs = ['properties', 'publishing', 'languages'];
        navigateToTab(expectedTabs[0], fixture);

        const editorComponent = findEditorComponent(fixture);
        expect(editorComponent.tabChangedSpy).toHaveBeenCalledTimes(1);
        expect(editorComponent.tabChangedSpy).toHaveBeenCalledWith(expectedTabs[0]);

        // Iterate through the remaining expectedTabs and navigate to each of them.
        for (let i = 1; i < expectedTabs.length; ++i) {
            const expectedTab = expectedTabs[i];
            const totalExpectedDispatchedActions = i + 1;

            navigateToTab(expectedTab, fixture);
            expect(findEditorComponent(fixture)).toBe(editorComponent, 'Editor component was recreated during navigation.');

            expect(switchTabActions.count).toBe(totalExpectedDispatchedActions, 'SwitchEditorTab action was not dispatched the expected number of times.');
            expect(switchTabActions.get(i).tabId).toEqual(expectedTab);

            expect(editorComponent.tabChangedSpy).toHaveBeenCalledTimes(totalExpectedDispatchedActions);
            expect(editorComponent.tabChangedSpy).toHaveBeenCalledWith(expectedTab);
        }
    }));

});
