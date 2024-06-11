import { TestBed, waitForAsync } from '@angular/core/testing';
import { NgxsModule, Store } from '@ngxs/store';
import { Observable, of } from 'rxjs';
import { AppState } from '../../app-state';
import { STATE_MODULES } from '../../state.module';
import { AppStateService } from './app-state.service';

class MockAction {
    static readonly type = '[test] MockAction';
}

describe('AppStateService', () => {

    let store: Store;
    let appState: AppStateService;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [ NgxsModule.forRoot(STATE_MODULES) ],
            providers: [ AppStateService ],
        }).compileComponents();
        store = TestBed.get(Store);
        appState = TestBed.get(AppStateService);
    }));

    it('dispatch() works', () => {
        const expectedResult = of<void>();
        const dispatchSpy = spyOn(store, 'dispatch').and.returnValue(expectedResult);
        const action = new MockAction();

        const actualResult = appState.dispatch(action);
        expect(dispatchSpy).toHaveBeenCalledTimes(1);
        expect(dispatchSpy).toHaveBeenCalledWith(action);
        expect(actualResult).toBe(expectedResult);
    });

    it('select() works', () => {
        const expectedResult: Observable<any> = of({});
        const selectSpy = spyOn(store, 'select').and.returnValue(expectedResult);
        const selector = (state: AppState) => state.auth;

        const actualResult = appState.select(selector);
        expect(selectSpy).toHaveBeenCalledTimes(1);
        expect(selectSpy.calls.argsFor(0)[0]).toBe(selector as any);
        expect(actualResult).toBe(expectedResult);
    });

    it('selectOnce() works', () => {
        const expectedResult: Observable<any> = of({});
        const selectOnceSpy = spyOn(store, 'selectOnce').and.returnValue(expectedResult);
        const selector = (state: AppState) => state.auth;

        const actualResult = appState.selectOnce(selector);
        expect(selectOnceSpy).toHaveBeenCalledTimes(1);
        expect(selectOnceSpy.calls.argsFor(0)[0]).toBe(selector as any);
        expect(actualResult).toBe(expectedResult);
    });

    it('snapshot() works', () => {
        const expectedResult: any = { };
        const snapshotSpy = spyOn(store, 'snapshot').and.returnValue(expectedResult);

        const actualResult = appState.snapshot();
        expect(snapshotSpy).toHaveBeenCalledTimes(1);
        expect(actualResult).toBe(expectedResult);
    });

    it('now getter works', () => {
        const expectedResult: any = { };
        const snapshotSpy = spyOn(store, 'snapshot').and.returnValue(expectedResult);

        const actualResult = appState.now;
        expect(snapshotSpy).toHaveBeenCalledTimes(1);
        expect(actualResult).toBe(expectedResult);
    });

});
