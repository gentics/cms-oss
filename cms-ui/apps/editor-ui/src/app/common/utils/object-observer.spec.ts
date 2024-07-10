import { observeObjectChanges } from './object-observer';

describe('observeObjectChanges', () => {

    it('should work on a simple object', () => {
        const ref = {
            hello: 'world',
            foo: ['bar'],
        };

        const fn = jasmine.createSpy();
        const obs = observeObjectChanges(fn, ref, ref);
        const NEW_VALUE = ['new'];
        obs.foo = NEW_VALUE

        expect(fn).toHaveBeenCalledOnceWith({
            path: ['foo'],
            value: NEW_VALUE,
        });

        expect(ref.foo).toEqual(NEW_VALUE);
        expect(obs.foo).toEqual(NEW_VALUE);
        expect(ref.hello).toEqual('world');
        expect(obs.hello).toEqual('world');

        fn.calls.reset();
        const OTHER_VALUE = 'newer world!';
        obs.hello = OTHER_VALUE;

        expect(fn).toHaveBeenCalledOnceWith({
            path: ['hello'],
            value: OTHER_VALUE,
        });

        expect(ref.hello).toEqual(OTHER_VALUE);
        expect(obs.hello).toEqual(OTHER_VALUE);
    });

    it('should work on a nested object', () => {
        const ref = {
            hello: 'world',
            foo: {
                bar: 'example1',
                even: {
                    deeper: 1,
                },
            },
        };

        const fn = jasmine.createSpy();
        const obs = observeObjectChanges(fn, ref, ref);

        const NEW_VALUE = 2;
        obs.foo.even.deeper = NEW_VALUE;

        expect(fn).toHaveBeenCalledOnceWith({
            path: ['foo', 'even', 'deeper'],
            value: NEW_VALUE,
        });

        expect(ref.foo.even.deeper).toEqual(NEW_VALUE);
        expect(obs.foo.even.deeper).toEqual(NEW_VALUE);
        expect(ref.foo.bar).toEqual('example1');
        expect(obs.foo.bar).toEqual('example1');
        expect(ref.hello).toEqual('world');
        expect(obs.hello).toEqual('world');

        fn.calls.reset();
        const OTHER_VALUE = 'newer world!';
        obs.foo.bar = OTHER_VALUE;

        expect(fn).toHaveBeenCalledOnceWith({
            path: ['foo', 'bar'],
            value: OTHER_VALUE,
        });

        expect(ref.foo.bar).toEqual(OTHER_VALUE);
        expect(obs.foo.bar).toEqual(OTHER_VALUE);
    });
});
