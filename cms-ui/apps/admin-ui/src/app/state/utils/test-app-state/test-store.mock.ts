import { Observable, of, Subscription } from 'rxjs';

export class MockStore {
    /**
     * Dispatches event(s).
     */
    dispatch: (event: any | any[]) => Observable<any> = jasmine.createSpy('dispatch').and.returnValue(of());
    /**
     * Selects a slice of data from the store.
     */
    select = jasmine.createSpy('select').and.returnValue(of());
    /**
     * Select one slice of data from the store.
     */
    selectOnce = jasmine.createSpy('selectOnce').and.returnValue(of());
    /**
     * Select a snapshot from the state.
     */
    selectSnapshot = jasmine.createSpy('selectOnce').and.returnValue(of());
    /**
     * Allow the user to subscribe to the root of the state
     */
    subscribe: (fn?: (value: any) => void) => Subscription = jasmine.createSpy('subscribe').and.returnValue(of().subscribe());
    /**
     * Return the raw value of the state.
     */
    snapshot: () => any = jasmine.createSpy('snapshot').and.returnValue(of());
    /**
     * Reset the state to a specific point in time. This method is useful
     * for plugin's who need to modify the state directly or unit testing.
     */
    reset: (state: any) => any = jasmine.createSpy('reset').and.returnValue(of());
}
