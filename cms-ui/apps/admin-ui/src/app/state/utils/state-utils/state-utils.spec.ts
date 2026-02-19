import { Injectable } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Action, NgxsModule, StateContext, Store } from '@ngxs/store';
import { ActionDef } from '@ngxs/store/src/actions/symbols';
import { Observable } from 'rxjs';
import {
    ActionDeclaration,
    ActionDefinition,
    AppStateBranch,
    defineInitialState,
    NO_ACTION_CLASS_ERROR_MSG,
    SelectState,
} from './state-utils';

interface TestStateModel {
    aString: string;
    aNumber: number;
}

class TestAction {
    static readonly type = '[test] TestAction';
    constructor(public userId: number) { }
}

@ActionDeclaration('test' as any)
class DecoratedTestAction1 {
    constructor(public sid: number) { }
}

@ActionDeclaration('test' as any)
class DecoratedTestAction2 {
    constructor(public lastError: string) { }
}

@ActionDeclaration('test' as any)
class DecoratedTestActionWithCustomType {
    static readonly type = 'my custom type';
}

class NoAction {
    constructor(public param: number) { }
}

function isObservable<T>(value: any): value is Observable<T> {
    return value != null
        && typeof value === 'object'
        && typeof value.subscribe === 'function';
}

let testModule: TestStateModule;

const TEST_STATE_DEFAULTS: TestStateModel = {
    aNumber: 123,
    aString: 'foobar',
} as const;

@AppStateBranch<TestStateModel>({
    name: 'test' as any,
    defaults: TEST_STATE_DEFAULTS,
})
@Injectable()
class TestStateModule {

    constructor() {
        testModule = this;
    }

    @Action(TestAction)
    runTestAction(ctx: StateContext<TestStateModel>, action: TestAction): void {
        ctx.patchState({
            aNumber: action.userId,
        });
    }

    @ActionDefinition(DecoratedTestAction1)
    runDecoratedTestAction1(ctx: StateContext<TestStateModel>, action: DecoratedTestAction1): void {
        ctx.patchState({
            aNumber: action.sid,
        });
    }

    @ActionDefinition(DecoratedTestAction2)
    runDecoratedTestAction2(ctx: StateContext<TestStateModel>, action: DecoratedTestAction2): void {
        ctx.patchState({
            aString: action.lastError,
        });
    }

}

@Injectable()
class TestService {

    @SelectState((state: any) => state.test)
    state$: Observable<TestStateModel>;

}

describe('StateUtils', () => {

    it('defineInitialState() returns a frozen object', () => {
        const initialState = defineInitialState<TestStateModel>({
            aString: '',
            aNumber: 0,
        });
        expect(() => initialState.aNumber = 10).toThrow();
    });

    describe('decorators', () => {

        let store: Store;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [ NgxsModule.forRoot([ TestStateModule ]) ],
                providers: [TestService],
            }).compileComponents();
            store = TestBed.get(Store);
        });

        afterEach(() => {
            testModule = null;
        });

        it('@AppStateBranch adds a branch to the Store', () => {
            expect(testModule).toBeTruthy();
            expect(testModule instanceof TestStateModule).toBeTruthy();
            expect(store.snapshot()).toEqual({
                test: TEST_STATE_DEFAULTS,
            });
        });

        it('@AppStateBranch registers actions properly', () => {
            const action = new TestAction(4711);
            const runTestActionSpy = spyOn(testModule, 'runTestAction').and.callThrough();
            const runDecoratedTestAction1Spy = spyOn(testModule, 'runDecoratedTestAction1').and.callThrough();
            const runDecoratedTestAction2Spy = spyOn(testModule, 'runDecoratedTestAction2').and.callThrough();

            store.dispatch(action);
            expect(runTestActionSpy).toHaveBeenCalledTimes(1);
            expect(runTestActionSpy.calls.argsFor(0)[1]).toBe(action);

            expect(runDecoratedTestAction1Spy).not.toHaveBeenCalled();
            expect(runDecoratedTestAction2Spy).not.toHaveBeenCalled();
        });

        it('@SelectState works', () => {
            const testService = TestBed.get(TestService) as TestService;
            expect(isObservable(testService.state$)).toBeTruthy();

            let emissionCount = 0;
            let latestValue: TestStateModel;
            const sub = testService.state$.subscribe(data => {
                ++emissionCount;
                latestValue = data;
            });

            expect(emissionCount).toBe(1);
            expect(latestValue).toEqual(TEST_STATE_DEFAULTS);

            store.dispatch(new TestAction(4711));
            expect(emissionCount).toBe(2);
            expect(latestValue).toEqual({
                ...TEST_STATE_DEFAULTS,
                aNumber: 4711,
            });

            sub.unsubscribe();
        });

        it('@ActionDeclaration adds the static type property to the action class', () => {
            const manualAction: ActionDef<any, any> = TestAction as any;
            const decoratedAction1: ActionDef<any, any> = DecoratedTestAction1 as any;
            const decoratedAction2: ActionDef<any, any> = DecoratedTestAction2 as any;

            expect(manualAction.type).toEqual('[test] TestAction');
            expect(decoratedAction1.type).toEqual('[test] DecoratedTestAction1');
            expect(decoratedAction2.type).toEqual('[test] DecoratedTestAction2');
        });

        it('@ActionDeclaration does not overwrite an existing type property', () => {
            expect(DecoratedTestActionWithCustomType.type).toEqual('my custom type');
        });

        it('@ActionDeclaration and @ActionDefinition register actions properly', () => {
            const runTestActionSpy = spyOn(testModule, 'runTestAction').and.callThrough();
            const runDecoratedTestAction1Spy = spyOn(testModule, 'runDecoratedTestAction1').and.callThrough();
            const runDecoratedTestAction2Spy = spyOn(testModule, 'runDecoratedTestAction2').and.callThrough();

            const action1 = new DecoratedTestAction1(4711);
            store.dispatch(action1);
            // Make sure that the correct handler method has been called with the right parameters.
            expect(runDecoratedTestAction1Spy).toHaveBeenCalledTimes(1);
            expect(runDecoratedTestAction1Spy.calls.argsFor(0)[1]).toBe(action1);
            expect(store.snapshot()).toEqual({
                test: {
                    ...TEST_STATE_DEFAULTS,
                    aNumber: 4711,
                },
            });
            // Make sure that the other handler methods have not been called.
            expect(runTestActionSpy).not.toHaveBeenCalled();
            expect(runDecoratedTestAction2Spy).not.toHaveBeenCalled();
            runDecoratedTestAction1Spy.calls.reset();

            const action2 = new DecoratedTestAction2('test');
            store.dispatch(action2);
            // Make sure that the correct handler method has been called with the right parameters.
            expect(runDecoratedTestAction2Spy).toHaveBeenCalledTimes(1);
            expect(runDecoratedTestAction2Spy.calls.argsFor(0)[1]).toBe(action2);
            expect(store.snapshot()).toEqual({
                test: {
                    ...TEST_STATE_DEFAULTS,
                    aNumber: 4711,
                    aString: 'test',
                },
            });
            // Make sure that the other handler methods have not been called.
            expect(runTestActionSpy).not.toHaveBeenCalled();
            expect(runDecoratedTestAction1Spy).not.toHaveBeenCalled();
            runDecoratedTestAction1Spy.calls.reset();
        });

        it('@ActionDefinition throws an error if the supplied class does not have a type property', () => {
            expect(() => ActionDefinition(NoAction)).toThrow(jasmine.objectContaining({
                message: NO_ACTION_CLASS_ERROR_MSG,
            }));
        });

    });

});
