export function spyWithOriginalFn<T extends object, K extends keyof T>(
    obj: T,
    fnName: K,
    stub: T[K] extends (...params: infer A) => infer V ? (original: T[K], ...params: A) => V : never,
): void {
    const originalFn = obj[fnName];
    obj[fnName] = jasmine.createSpy(fnName as string).and.callFake((...args) => {
        return stub(originalFn, ...args as any);
    }) as any;
}
