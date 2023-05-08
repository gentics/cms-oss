import { AppStateService } from '@admin-ui/state';
import { AfterViewInit, Component, OnInit } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { takeUntil } from 'rxjs/operators';
import { componentTest } from '../../../../testing';
import { ObservableStopper } from '../../../common';
import { assembleTestAppStateImports, TEST_APP_STATE, TestAppState } from '../../../state/utils/test-app-state';
import { LoadingTriggerComponent } from './loading-trigger.component';

@Component({
    template: `<gtx-loading-trigger></gtx-loading-trigger>`,
})
class TestComponent implements OnInit, AfterViewInit {
    ngOnInitSpy = jasmine.createSpy('ngOnInit');
    ngAfterViewInitSpy = jasmine.createSpy('ngAfterViewInit');

    ngOnInit(): void {
        this.ngOnInitSpy();
    }

    ngAfterViewInit(): void {
        this.ngAfterViewInitSpy();
    }
}

describe('LoadingTriggerComponent', () => {
    let appState: TestAppState;
    let stopper: ObservableStopper;

    beforeEach(() => {
        stopper = new ObservableStopper();
        TestBed.configureTestingModule({
            imports: [...assembleTestAppStateImports()],
            declarations: [
                LoadingTriggerComponent,
                TestComponent,
            ],
            providers: [
                TEST_APP_STATE,
            ],
        }).compileComponents();

        appState = TestBed.get(AppStateService);
    });

    afterEach(() => {
        stopper.stop();
    });

    it('should increment and decrement masterLoading right',
        componentTest(() => TestComponent, (fixture, instance) => {
            const masterLoadings: number[] = [];

            appState.select(state => state.loading)
                .pipe(takeUntil(stopper.stopper$))
                .subscribe(loadingState => {
                    masterLoadings.push(loadingState.masterLoading);
            });

            fixture.detectChanges();

            expect(masterLoadings).toEqual([0, 1, 0]);
        }),
    );
});
