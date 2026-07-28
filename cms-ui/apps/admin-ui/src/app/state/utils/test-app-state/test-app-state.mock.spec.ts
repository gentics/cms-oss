import { Injectable } from '@angular/core';
import { TestBed, waitForAsync } from '@angular/core/testing';
import { IS_NORMALIZED, RecursivePartial } from '@gentics/cms-models';
import { Action, NgxsModule, State, StateContext, ofActionDispatched, ofActionSuccessful } from '@ngxs/store';
import { cloneDeep } from 'lodash-es';
import { EntityStateModel, INITIAL_ENTITY_STATE } from '../../entity/entity.state';
import { AppStateService } from '../../providers/app-state/app-state.service';
import { defineInitialState } from '../state-utils/state-utils';
import { TEST_APP_STATE, TestAppState } from './test-app-state.mock';

interface SimpleStateModel {
    aString: string;
    aNumber: number;
}

interface NestedStateModel {
    level0Str: string;
    level0ObjA: {
        level1Str: string;
        level1Number: number;
    };
    level0ObjB: {
        level1Str: string;
        level1Number: number;
    };
    level0ArrayA: string[];
    level0ArrayB: string[];
}

interface TestState {
    simple: SimpleStateModel;
    nested: NestedStateModel;
    entity: EntityStateModel;
}

const INITIAL_SIMPLE_STATE = defineInitialState<SimpleStateModel>({
    aString: null,
    aNumber: 10,
});

const INITIAL_NESTED_STATE = defineInitialState<NestedStateModel>({
    level0Str: '',
    level0ObjA: null,
    level0ObjB: {
        level1Str: '',
        level1Number: 0,
    },
    level0ArrayA: [ ],
    level0ArrayB: [ 'test1', 'test2' ],
});

class TestActionA {
    static readonly type = '[nested] TestActionA';
    constructor(public level0Str: string) {}
}

class TestActionB {
    static readonly type = '[nested] TestActionB';
    constructor(public level0ArrayA: string[]) {}
}

@State<SimpleStateModel>({
    name: 'simple',
    defaults: INITIAL_SIMPLE_STATE,
})
@Injectable()
class SimpleStateModule { }

@State<NestedStateModel>({
    name: 'nested',
    defaults: INITIAL_NESTED_STATE,
})
@Injectable()
class NestedStateModule {

    @Action(TestActionA)
    testActionA(ctx: StateContext<NestedStateModel>, action: TestActionA): void {
        ctx.patchState({
            level0Str: action.level0Str,
        });
    }

    @Action(TestActionB)
    testActionB(ctx: StateContext<NestedStateModel>, action: TestActionB): void {
        ctx.patchState({
            level0ArrayA: action.level0ArrayA,
        });
    }

}

@State<EntityStateModel>({
    name: 'entity',
    defaults: INITIAL_ENTITY_STATE,
})
@Injectable()
class MockEntityStateModule { }

describe('TestAppState', () => {

    let appState: TestAppState;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [ NgxsModule.forRoot([ SimpleStateModule, NestedStateModule, MockEntityStateModule ]) ],
            providers: [ TEST_APP_STATE ],
        }).compileComponents();
        appState = TestBed.inject(AppStateService) as any;
    }));

    it('mockState() works for a partial change in one branch', () => {
        const partialState: RecursivePartial<TestState> = {
            simple: {
                aString: 'test',
            },
        };
        const expectedState: TestState = cloneDeep(appState.snapshot()) as any;
        expectedState.simple.aString = 'test';

        appState.mockState(partialState as any);
        expect(appState.snapshot() as any).toEqual(expectedState);
    });

    it('mockState() works for a full change in one branch', () => {
        const partialState: RecursivePartial<TestState> = {
            simple: {
                aString: 'test',
                aNumber: 4711,
            },
        };
        const expectedState: TestState = cloneDeep(appState.snapshot()) as any;
        expectedState.simple.aString = 'test';
        expectedState.simple.aNumber = 4711;

        appState.mockState(partialState as any);
        expect(appState.snapshot() as any).toEqual(expectedState);
    });

    it('mockState() works for a partial change in a branch with nesting', () => {
        const partialState: RecursivePartial<TestState> = {
            nested: {
                level0Str: 'test',
            },
        };
        const expectedState: TestState = cloneDeep(appState.snapshot()) as any;
        expectedState.nested.level0Str = 'test';

        appState.mockState(partialState as any);
        expect(appState.snapshot() as any).toEqual(expectedState);
    });

    it('mockState() works for a partial change to a nested object that was null', () => {
        const partialState: RecursivePartial<TestState> = {
            nested: {
                level0ObjA: {
                    level1Number: 100,
                },
            },
        };
        const expectedState: TestState = cloneDeep(appState.snapshot()) as any;
        expectedState.nested.level0ObjA = {
            level1Number: 100,
        } as any;

        appState.mockState(partialState as any);
        expect(appState.snapshot() as any).toEqual(expectedState);
    });

    it('mockState() works for a partial change to a nested object', () => {
        const partialState: RecursivePartial<TestState> = {
            nested: {
                level0ObjB: {
                    level1Str: 'test',
                },
            },
        };
        const expectedState: TestState = cloneDeep(appState.snapshot()) as any;
        expectedState.nested.level0ObjB.level1Str = 'test';

        appState.mockState(partialState as any);
        expect(appState.snapshot() as any).toEqual(expectedState);
    });

    it('mockState() works for a full change to a nested object', () => {
        const partialState: RecursivePartial<TestState> = {
            nested: {
                level0ObjB: {
                    level1Str: 'test',
                    level1Number: 4711,
                },
            },
        };
        const expectedState: TestState = cloneDeep(appState.snapshot()) as any;
        expectedState.nested.level0ObjB.level1Str = 'test';
        expectedState.nested.level0ObjB.level1Number = 4711;

        appState.mockState(partialState as any);
        expect(appState.snapshot() as any).toEqual(expectedState);
    });

    it('mockState() works for a change to an empty array', () => {
        const partialState: RecursivePartial<TestState> = {
            nested: {
                level0ArrayA: [ 'A', 'B' ],
            },
        };
        const expectedState: TestState = cloneDeep(appState.snapshot()) as any;
        expectedState.nested.level0ArrayA = [ 'A', 'B' ];

        appState.mockState(partialState as any);
        expect(appState.snapshot() as any).toEqual(expectedState);
    });

    it('mockState() works for a change to a non-empty array', () => {
        const partialState: RecursivePartial<TestState> = {
            nested: {
                level0ArrayB: [ 'A', 'B' ],
            },
        };
        const expectedState: TestState = cloneDeep(appState.snapshot()) as any;
        expectedState.nested.level0ArrayB = [ 'A', 'B' ];

        appState.mockState(partialState as any);
        expect(appState.snapshot() as any).toEqual(expectedState);
    });

    it('mockState() copies [IS_NORMALIZED] for entities', () => {
        const USER_ID = 2;
        const partialState: RecursivePartial<TestState> = {
            entity: {
                user: {
                    [USER_ID]: {
                        id: USER_ID,
                        login: 'test',
                        firstName: 'John',
                        lastName: 'Doe',
                        [IS_NORMALIZED]: true,
                    },
                },
            },
        };
        const expectedState: TestState = cloneDeep(appState.snapshot()) as any;
        expectedState.entity.user = partialState.entity.user as any;

        appState.mockState(partialState as any);
        expect(appState.snapshot() as any).toEqual(expectedState);
        expect(appState.now.entity.user[USER_ID][IS_NORMALIZED]).toBe(true, '[IS_NORMALIZED] was not copied.');
    });

    it('trackActions() works', () => {
        const actions$ = appState.trackActions();

        let actionsDispatched = 0;
        const sub = actions$.pipe(
            ofActionDispatched(TestActionA),
        ).subscribe(() => ++actionsDispatched);

        expect(actionsDispatched).toBe(0);

        appState.dispatch(new TestActionA('test0'));
        expect(actionsDispatched).toBe(1);

        appState.dispatch(new TestActionA('test1'));
        expect(actionsDispatched).toBe(2);

        sub.unsubscribe();
    });

    describe('trackActionsAuto()', () => {

        it('works', () => {
            const filterSpy = jasmine.createSpy('ofActionDispatched').and.callFake((...allowedTypes: any[]) => ofActionDispatched(...allowedTypes));

            const trackedActions = appState.trackActionsAuto(filterSpy, TestActionA);
            expect(filterSpy).toHaveBeenCalledTimes(1);
            expect(filterSpy).toHaveBeenCalledWith(TestActionA);

            const actionA0 = new TestActionA('test0');
            appState.dispatch(actionA0);
            expect(trackedActions.count).toBe(1);
            expect(trackedActions.get(0)).toBe(actionA0);

            appState.dispatch(new TestActionB(['testB']));
            expect(trackedActions.count).toBe(1);
            expect(trackedActions.get(0)).toBe(actionA0);

            const actionA1 = new TestActionA('test1');
            appState.dispatch(actionA1);
            expect(trackedActions.count).toBe(2);
            expect(trackedActions.get(0)).toBe(actionA0);
            expect(trackedActions.get(1)).toBe(actionA1);
        });

        it('works for multiple action types', () => {
            const filterSpy = jasmine.createSpy('ofActionSuccessful').and.callFake((...allowedTypes: any[]) => ofActionSuccessful(...allowedTypes));

            const trackedActions = appState.trackActionsAuto(filterSpy as any, TestActionA, TestActionB);
            expect(filterSpy).toHaveBeenCalledTimes(1);
            expect(filterSpy).toHaveBeenCalledWith(TestActionA, TestActionB);

            const action0 = new TestActionA('test0');
            appState.dispatch(action0);
            expect(trackedActions.count).toBe(1);
            expect(trackedActions.get(0)).toBe(action0);

            const action1 = new TestActionB(['test1']);
            appState.dispatch(action1);
            expect(trackedActions.count).toBe(2);
            expect(trackedActions.get(0)).toBe(action0);
            expect(trackedActions.get(1)).toBe(action1);
        });

        it('reset() works', () => {
            const filterSpy = jasmine.createSpy('ofActionDispatched').and.callFake((...allowedTypes: any[]) => ofActionDispatched(...allowedTypes));

            const trackedActions = appState.trackActionsAuto(filterSpy, TestActionA);

            const action0 = new TestActionA('test0');
            appState.dispatch(action0);
            expect(trackedActions.count).toBe(1);

            trackedActions.reset();
            expect(trackedActions.count).toBe(0);

            const action1 = new TestActionA('test1');
            appState.dispatch(action1);
            expect(trackedActions.count).toBe(1);
            expect(trackedActions.get(0)).toBe(action1);
        });

        it('works with multiple trackers', () => {
            const filterSpy = jasmine.createSpy('ofActionDispatched').and.callFake((...allowedTypes: any[]) => ofActionDispatched(...allowedTypes));

            const trackedActionsA = appState.trackActionsAuto(filterSpy, TestActionA);
            const trackedActionsB = appState.trackActionsAuto(filterSpy, TestActionB);

            const actionA = new TestActionA('test0');
            appState.dispatch(actionA);
            const actionB = new TestActionB(['test1']);
            appState.dispatch(actionB);

            expect(trackedActionsA.count).toBe(1);
            expect(trackedActionsA.get(0)).toBe(actionA);
            expect(trackedActionsB.count).toBe(1);
            expect(trackedActionsB.get(0)).toBe(actionB);
        });

    });

});
