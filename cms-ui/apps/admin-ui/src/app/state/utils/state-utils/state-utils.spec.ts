import { Injectable } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Action, ActionDef, NgxsModule, StateContext, Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { AuthStateModel, INITIAL_AUTH_STATE } from '../../auth/auth.state';
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
    static readonly type = '[auth] TestAction';
    constructor(public userId: number) { }
}

@ActionDeclaration('auth')
class DecoratedTestAction1 {
    constructor(public sid: number) { }
}

@ActionDeclaration('auth')
class DecoratedTestAction2 {
    constructor(public lastError: string) { }
}

@ActionDeclaration('auth')
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

let authStateModuleInstance: TestAuthStateModule;

@AppStateBranch<AuthStateModel>({
    name: 'auth',
    defaults: INITIAL_AUTH_STATE,
})
@Injectable()
class TestAuthStateModule {

    constructor() {
        authStateModuleInstance = this;
    }

    @Action(TestAction)
    runTestAction(ctx: StateContext<AuthStateModel>, action: TestAction): void {
        ctx.patchState({
            currentUserId: action.userId,
        });
    }

    @ActionDefinition(DecoratedTestAction1)
    runDecoratedTestAction1(ctx: StateContext<AuthStateModel>, action: DecoratedTestAction1): void {
        ctx.patchState({
            sid: action.sid,
        });
    }

    @ActionDefinition(DecoratedTestAction2)
    runDecoratedTestAction2(ctx: StateContext<AuthStateModel>, action: DecoratedTestAction2): void {
        ctx.patchState({
            lastError: action.lastError,
        });
    }

}

@Injectable()
class TestService {

    @SelectState(state => state.auth)
    authState$: Observable<AuthStateModel>;

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
                imports: [ NgxsModule.forRoot([ TestAuthStateModule ]) ],
                providers: [TestService],
            }).compileComponents();
            store = TestBed.inject(Store);
        });

        afterEach(() => {
            authStateModuleInstance = null;
        });

        it('@AppStateBranch adds a branch to the Store', () => {
            expect(authStateModuleInstance).toBeTruthy();
            expect(authStateModuleInstance instanceof TestAuthStateModule).toBeTruthy();
            expect(store.snapshot()).toEqual({
                auth: INITIAL_AUTH_STATE,
            });
        });

        it('@AppStateBranch registers actions properly', () => {
            const action = new TestAction(4711);
            const runTestActionSpy = spyOn(authStateModuleInstance, 'runTestAction').and.callThrough();
            const runDecoratedTestAction1Spy = spyOn(authStateModuleInstance, 'runDecoratedTestAction1').and.callThrough();
            const runDecoratedTestAction2Spy = spyOn(authStateModuleInstance, 'runDecoratedTestAction2').and.callThrough();

            store.dispatch(action);
            expect(runTestActionSpy).toHaveBeenCalledTimes(1);
            expect(runTestActionSpy.calls.argsFor(0)[1]).toBe(action);

            expect(runDecoratedTestAction1Spy).not.toHaveBeenCalled();
            expect(runDecoratedTestAction2Spy).not.toHaveBeenCalled();
        });

        it('@SelectState works', () => {
            const testService = TestBed.inject(TestService) as TestService;
            expect(isObservable(testService.authState$)).toBeTruthy();

            let emissionCount = 0;
            let latestValue: AuthStateModel;
            const sub = testService.authState$.subscribe(authState => {
                ++emissionCount;
                latestValue = authState;
            });

            expect(emissionCount).toBe(1);
            expect(latestValue).toEqual(INITIAL_AUTH_STATE);

            store.dispatch(new TestAction(4711));
            expect(emissionCount).toBe(2);
            expect(latestValue).toEqual({
                ...INITIAL_AUTH_STATE,
                currentUserId: 4711,
            });

            sub.unsubscribe();
        });

        it('@ActionDeclaration adds the static type property to the action class', () => {
            const manualAction: ActionDef<any, any> = TestAction as any;
            const decoratedAction1: ActionDef<any, any> = DecoratedTestAction1 as any;
            const decoratedAction2: ActionDef<any, any> = DecoratedTestAction2 as any;

            expect(manualAction.type).toEqual('[auth] TestAction');
            expect(decoratedAction1.type).toEqual('[auth] DecoratedTestAction1');
            expect(decoratedAction2.type).toEqual('[auth] DecoratedTestAction2');
        });

        it('@ActionDeclaration does not overwrite an existing type property', () => {
            expect(DecoratedTestActionWithCustomType.type).toEqual('my custom type');
        });

        it('@ActionDeclaration and @ActionDefinition register actions properly', () => {
            const runTestActionSpy = spyOn(authStateModuleInstance, 'runTestAction').and.callThrough();
            const runDecoratedTestAction1Spy = spyOn(authStateModuleInstance, 'runDecoratedTestAction1').and.callThrough();
            const runDecoratedTestAction2Spy = spyOn(authStateModuleInstance, 'runDecoratedTestAction2').and.callThrough();

            const action1 = new DecoratedTestAction1(4711);
            store.dispatch(action1);
            // Make sure that the correct handler method has been called with the right parameters.
            expect(runDecoratedTestAction1Spy).toHaveBeenCalledTimes(1);
            expect(runDecoratedTestAction1Spy.calls.argsFor(0)[1]).toBe(action1);
            expect(store.snapshot()).toEqual({
                auth: {
                    ...INITIAL_AUTH_STATE,
                    sid: 4711,
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
                auth: {
                    ...INITIAL_AUTH_STATE,
                    sid: 4711,
                    lastError: 'test',
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
