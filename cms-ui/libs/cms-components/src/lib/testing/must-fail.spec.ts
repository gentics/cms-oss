import { mustFail } from './must-fail';

// TODO: these tests fail when run with full suite, but pass on their own.
xdescribe('mustFail', () => {
    it('works with a synchronous function',
        mustFail(() => {
            expect(true).toBe(false);
        }),
    );

    it('works with exceptions in a synchronous function',
        mustFail(() => {
            throw new Error('This exception should be caught by mustFail().');
        }),
    );

    it('works with a function expecting a callback',
        mustFail((done: DoneFn) => {
            setTimeout(done.fail, 100);
        }),
    );

    it('works with a function returning a promise',
        mustFail(() =>
            new Promise((resolve, reject) => {
                setTimeout(reject, 100);
            },
        )),
    );

    it('fails with a successful synchronous function',
        mustFail(
            mustFail(
                () => { expect(true).toBe(true); },
            ),
        ),
    );

    it('fails with a successful function expecting a callback',
        mustFail(
            mustFail(
                (done: DoneFn) => {
                    setTimeout(done, 100);
                },
            ),
        ),
    );

    it('fails with a successful function returning a promise',
        mustFail(
            mustFail(
                () => new Promise((resolve, reject) => {
                    setTimeout(resolve, 100);
                }),
            ),
        ),
    );

});
