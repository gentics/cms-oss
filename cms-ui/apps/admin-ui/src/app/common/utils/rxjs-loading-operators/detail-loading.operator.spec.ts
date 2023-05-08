import { TestBed } from '@angular/core/testing';
import { ActionType, ofActionDispatched } from '@ngxs/store';
import { Subject, Subscription } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { DecrementDetailLoading, IncrementDetailLoading } from '../../../state';
import { AppStateService } from '../../../state/providers/app-state/app-state.service';
import { assembleTestAppStateImports, TestAppState, TEST_APP_STATE, TrackedActions } from '../../../state/utils/test-app-state';
import { ObservableStopper } from '../observable-stopper/observable-stopper';
import { detailLoading } from './detail-loading.operator';

describe('detailLoading', () => {
    let appState: TestAppState;

    let source$: Subject<any>;

    let stopper: ObservableStopper;

    let dispatchedIncrementActions: TrackedActions<IncrementDetailLoading>;
    let dispatchedDecrementActions: TrackedActions<DecrementDetailLoading>;

    let emittedValue: any;
    let emissionCount = 0;

    let completed = false;
    let error = false;

    let subscription: Subscription;

    function createSubscription(): void {
        emissionCount = 0;
        completed = false;
        error = false;

        const trackedSource$ = source$.pipe(
            detailLoading(appState),
            takeUntil(stopper.stopper$),
        );

        expect(dispatchedIncrementActions.count).toBe(0);
        expect(dispatchedDecrementActions.count).toBe(0);

        subscription = trackedSource$.subscribe({
            next: (value) => {
                emittedValue = value;
                emissionCount++;
            },
            complete: () => completed = true,
            error: () => error = true,
        });

        expect(dispatchedIncrementActions.count).toBe(1);
        expect(dispatchedDecrementActions.count).toBe(0);
        expect(source$.observers.length).toBe(1);
    }

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [assembleTestAppStateImports()],
            providers: [
                TestAppState,
                { provide: AppStateService, useExisting: TestAppState },
            ],
        }).compileComponents();

        source$ = new Subject();

        stopper = new ObservableStopper();

        appState = TestBed.inject(TestAppState);

        const filterSpy = jasmine.createSpy('ofActionDispatched').and.callFake((...allowedTypes: any[]) => ofActionDispatched(...allowedTypes));

        dispatchedIncrementActions = appState.trackActionsAuto(filterSpy, IncrementDetailLoading);
        dispatchedDecrementActions = appState.trackActionsAuto(filterSpy, DecrementDetailLoading);
    });

    afterEach(() => {
        stopper.stop();
    });

    it('works when source observable completes', () => {
        createSubscription();

        source$.next('test');

        expect(emittedValue).toBe('test');
        expect(emissionCount).toBe(1);

        expect(dispatchedIncrementActions.count).toBe(1);
        expect(dispatchedDecrementActions.count).toBe(0);

        source$.complete();

        expect(dispatchedIncrementActions.count).toBe(1);
        expect(dispatchedDecrementActions.count).toBe(1);
        expect(completed).toBe(true);
        expect(error).toBe(false);
        expect(source$.observers.length).toBe(0);
    });

    it('works when source observable emits some error', () => {
        createSubscription();

        source$.error('error');

        expect(dispatchedIncrementActions.count).toBe(1);
        expect(dispatchedDecrementActions.count).toBe(1);
        expect(completed).toBe(false);
        expect(error).toBe(true);
        expect(source$.observers.length).toBe(0);
    });

    it('works when subscription is unsubscribed', () => {
        createSubscription();

        source$.next('test');

        expect(emittedValue).toBe('test');
        expect(emissionCount).toBe(1);

        expect(dispatchedIncrementActions.count).toBe(1);
        expect(dispatchedDecrementActions.count).toBe(0);

        subscription.unsubscribe();

        expect(dispatchedIncrementActions.count).toBe(1);
        expect(dispatchedDecrementActions.count).toBe(1);
        expect(completed).toBe(false);
        expect(error).toBe(false);
        expect(source$.observers.length).toBe(0);
    });
});
