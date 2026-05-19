type Fn = (...args: any) => any;

type SpyFn<T extends Fn> = jasmine.Spy<T>;

export type Mocked<T> = {
    [K in keyof T]?: T[K] extends Fn ? SpyFn<T[K]> : T[K];
};

export function createSpyFn<T extends Fn>(name: string, fakeFn?: T): SpyFn<T> {
    // eslint-disable-next-line @typescript-eslint/no-unsafe-call
    const spy = jasmine.createSpy(name);

    if (typeof fakeFn === 'function') {
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        spy.and.callFake(fakeFn);
    }

    return spy as any;
};
