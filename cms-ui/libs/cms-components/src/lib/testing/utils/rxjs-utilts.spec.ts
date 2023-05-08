import { fakeAsync, tick } from '@angular/core/testing';
import { BehaviorSubject, Subject, Subscription } from 'rxjs';
import { delay } from 'rxjs/operators';
import { ObservableStopper } from '../../common/utils';
import { createDelayedError, subscribeSafely, tickAndGetEmission } from './rxjs-utils';

describe('rxjs-utils', () => {

    let source$: Subject<number>;
    let stopper: ObservableStopper;

    beforeEach(() => {
        source$ = new Subject();
        stopper = new ObservableStopper();
    });

    afterEach(() => {
        stopper.stop();
    });

    describe('subscribeSafely()', () => {

        it('passes the next and complete callbacks to subscribe() and returns a Subscription', () => {
            const next = jasmine.createSpy('next').and.stub();
            const error = jasmine.createSpy('error').and.stub();
            const complete = jasmine.createSpy('complete').and.stub();

            const expectedValue1 = 4711;
            const expectedValue2 = 1234;

            const sub = subscribeSafely(source$, stopper, next, error, complete);
            expect(sub instanceof Subscription).toBe(true);

            source$.next(expectedValue1);
            expect(next).toHaveBeenCalledTimes(1);
            expect(next).toHaveBeenCalledWith(expectedValue1);
            expect(error).not.toHaveBeenCalled();
            expect(complete).not.toHaveBeenCalled();

            source$.next(expectedValue2);
            expect(next).toHaveBeenCalledTimes(2);
            expect(next).toHaveBeenCalledWith(expectedValue2);
            expect(error).not.toHaveBeenCalled();
            expect(complete).not.toHaveBeenCalled();

            source$.complete();
            expect(complete).toHaveBeenCalledTimes(1);
        });

        it('passes the error callback to subscribe()', fakeAsync(() => {
            const next = jasmine.createSpy('next').and.stub();
            const error = jasmine.createSpy('error').and.stub();
            const complete = jasmine.createSpy('complete').and.stub();

            const expectedError = new Error('Test Error');
            subscribeSafely(createDelayedError(expectedError), stopper, next, error, complete);

            tick();
            expect(error).toHaveBeenCalledTimes(1);
            expect(error).toHaveBeenCalledWith(expectedError);
            expect(next).not.toHaveBeenCalled();
            expect(complete).not.toHaveBeenCalled();
        }));

        it('unsubscribes when the stopper stops', fakeAsync(() => {
            subscribeSafely(source$, stopper, () => {});
            expect(source$.observers.length).toBe(1);

            stopper.stop();
            expect(source$.observers.length).toBe(0);
        }));

    });

    describe('tickAndGetEmission()', () => {

        it('returns the emitted value and unsubscribes after the emission', fakeAsync(() => {
            const expectedValue = 4711;
            source$ = new BehaviorSubject(null);
            source$.next(expectedValue);
            const result = tickAndGetEmission(source$.pipe(delay(0)));

            expect(result).toBe(expectedValue);
            expect(source$.observers.length).toBe(0);
        }));

        it('returns undefined if the source does not emit and unsubscribes', fakeAsync(() => {
            const result = tickAndGetEmission(source$);

            expect(result).toBeUndefined();
            expect(source$.observers.length).toBe(0);
        }));

        it('unsubscribes if there is an error during tick()', fakeAsync(() => {
            const error = new Error('Test Error during tick()');
            setTimeout(() => { throw error; });

            expect(() => tickAndGetEmission(source$)).toThrowError(error.message);
            expect(source$.observers.length).toBe(0);
        }));

    });

});
