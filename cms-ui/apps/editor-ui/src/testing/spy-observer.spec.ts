import {SpyObserver} from './spy-observer';

describe('SpyObserver', () => {
    it('creates an object with next, error & complete spies', () => {
        let observer = new SpyObserver();
        function expectSpy(obj: any): void {
            expect(typeof obj).toBe('function', 'spy is not a function');
            expect(typeof obj.and).toBe('object', 'spy.and is not an object');
            expect('identity' in obj.and).toBe(true, 'no identity property');
            expect('calls' in obj).toBe(true, 'no calls property');
        }
        expectSpy(observer.next);
        expectSpy(observer.error);
        expectSpy(observer.complete);
    });
});
