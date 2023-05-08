/* eslint-disable @typescript-eslint/no-unsafe-call */
/* eslint-disable no-var */
declare var window: any;
declare var global: any;
// eslint-disable-next-line @typescript-eslint/naming-convention
declare var Zone: any;

type MustFailTest = (done?: DoneFn) => void | Promise<any>;

/**
 * Specifies a test that must fail in order for the outer test to succeed.
 *
 * Usage:
 * ```typescript
 * describe('some feature', () => {
 *     it('fails when something happens',
 *         mustFail(() => {
 *             expect('no').toBe('yes')
 *         })
 *     )
 * });
 * ```
 */
export function mustFail(testThatShouldFail: MustFailTest): (inverted: DoneFn) => void {
    const globalRef: any = (typeof window !== undefined ? window : global);

    // We need to get a reference to the root Zone in order to avoid the exception
    // "unexpected Zone: ProxyZone". If the root node is not available, use the current zone.
    const zoneToUse: any = Zone.current.parent || Zone.current;

    return function invertedDoneFunction(realDone: DoneFn): void {
        let outerExpect = expect;
        let env: any = new (jasmine as any).Env();
        let spec: any;

        if (zoneToUse === null) {
            console.log('mustFail() - no zone.js Zone!');
            throw new Error('no zone.js Zone!');
            // debugger;
        }

        env.describe('fake test suite', () => {
            spec = env.it('test that should fail', function (callback: DoneFn): void {
                let delegateWasUsed = false;
                const callbackDelegate: DoneFn = () => {
                    delegateWasUsed = true;
                    callback();
                };
                callbackDelegate.fail = (error: string | Error) => {
                    delegateWasUsed = true;
                    callback.fail(error);
                }

                zoneToUse.run(() => {
                    try {
                        // Execute the test with a delegate
                        const result = testThatShouldFail(callbackDelegate);

                        // If the result is a Promise, we wait for it to finish
                        if (result != null && result instanceof Promise) {
                            result.then(() => {
                                if (delegateWasUsed) {
                                    console.warn('DO NOT mit the done-function and a promise result!');
                                    return;
                                }

                                callback();
                            }).catch(error => {
                                if (delegateWasUsed) {
                                    console.warn('DO NOT mit the done-function and a promise result!');
                                    return;
                                }

                                callback.fail(error);
                            });
                            return;
                        }

                        // If it's a simple sync test, this should just mark the test as successful
                        callback();
                    } catch (err) {
                        callback.fail(err);
                    }
                });
            });
        });

        env.addReporter({
            jasmineDone(): void {
                globalRef.expect = outerExpect;

                if (spec.result.status === 'passed') {
                    realDone.fail('Test should have failed, but it passed');
                } else {
                    realDone();
                }
            },
        });

        env.execute();
    };
}
