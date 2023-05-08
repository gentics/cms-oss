import {ObservableStopper} from './observable-stopper';

describe('ObservableStopper', () => {

    let stopper: ObservableStopper;

    beforeEach(() => {
        stopper = new ObservableStopper();
    });

    it('stop() works', () => {
        let emitted = false;
        let completed = false;

        stopper.stopper$.subscribe(
            () => emitted = true,
            () => {},
            () => completed = true
        );

        expect(emitted).toBe(false);
        expect(completed).toBe(false);
        expect(stopper.isStopped).toBe(false);

        stopper.stop();
        expect(emitted).toBe(true);
        expect(completed).toBe(true);
        expect(stopper.isStopped).toBe(true);
    });

});
