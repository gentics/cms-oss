export function spyWithOriginalFn<T extends object, K extends keyof T>(
    obj: T,
    // eslint-disable-next-line @typescript-eslint/no-unsafe-function-type
    fnName: T[K] extends Function ? K : never,
    stub: T[K] extends (...params: infer A) => infer V ? (original: T[K], ...params: A) => V : never,
): void {
    const originalFn = obj[fnName];
    spyOn(obj, fnName).and.callFake(((...args) => {
        return stub(originalFn, ...args);
    }) as any);
}
