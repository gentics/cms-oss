import {fakeAsync, tick} from '@angular/core/testing';
import {throttle} from './throttle';

describe('throttle()', () => {

    // Simulate a long time so that the setTimeout is cleared.
    function clearTimer(): void {
        tick(100000);
    }

    let count = 0;
    const inc = () => count++;
    const throttledInc = throttle(inc, 100);

    beforeEach(() => count = 0);

    it('should invoke the callback on first call', fakeAsync(() => {
        throttledInc();

        expect(count).toBe(1);
        clearTimer();
    }));

    it('should not invoke the second call before the limit has passed', fakeAsync(() => {
        throttledInc();
        tick(10);
        throttledInc();

        expect(count).toBe(1);
        clearTimer();
    }));

    it('should invoke the second call after the limit has passed', fakeAsync(() => {
        throttledInc();
        tick(100);
        throttledInc();

        expect(count).toBe(2);
        clearTimer();
    }));

    it('should handle multiple valid and invalid calls', fakeAsync(() => {
        throttledInc();
        tick(10);
        throttledInc();
        throttledInc();
        tick(80);
        throttledInc();
        tick(20);
        throttledInc();
        tick(50);
        throttledInc();
        tick(200);
        throttledInc();

        expect(count).toBe(3);
        clearTimer();
    }));

    it('throttled function should accept arguments', fakeAsync(() => {
        let foo = '';
        const throttledConcat = throttle((arg1: string, arg2: string = ' ') => foo += (arg1 + arg2), 100);

        throttledConcat('bar');
        expect(foo).toBe('bar ');

        tick(100);

        throttledConcat('baz', '!');

        expect(foo).toBe('bar baz!');

        clearTimer();
    }));
});
