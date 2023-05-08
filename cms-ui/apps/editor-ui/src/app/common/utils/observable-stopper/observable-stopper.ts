import {Subject, Observable} from 'rxjs';

// tslint:disable: jsdoc-format
/**
 * Utility class for completing observables using the `takeUntil` pattern.
 *
 * When creating an observable, to which you want to subscribe directly,
 * pipe it into the `takeUntil` operator dependent on an `ObservableStopper`,
 * as the last operator before subscribing. In `ngOnDestroy()` call
 * the `stop()` method to instruct all such observables to complete,
 * which allows avoiding a manual unsubscription.
 *
 * ```TypeScript
   class MyComponent {
       private stopper = new ObservableStopper();

       ngOnInit(): void {
           const myObservable$ = someEvent$.pipe(
               ...
               takeUntil(this.stopper.stopper$)
           );
           myObservable.subscribe(...);
       }

       ngOnDestroy(): void {
           this.stopper.stop();
       }
   }
 * ```
 *
 * Note that `ObservableStopper` extends `Subject` for implementation convenience.
 * Do not use any of Subject's methods directly.
 *
 * @see https://medium.com/@benlesh/rxjs-dont-unsubscribe-6753ed4fda87
 */
export class ObservableStopper {
    // tslint:enable: jsdoc-format

    private stopperSubj$ = new Subject<void>();

    get stopper$(): Observable<void> {
        return this.stopperSubj$;
    }

    get isStopped(): boolean {
        return this.stopperSubj$.isStopped;
    }

    /**
     * Instructs all observables that depend on this stopper, to complete.
     */
    stop(): void {
        this.stopperSubj$.next();
        this.stopperSubj$.complete();
    }

}
