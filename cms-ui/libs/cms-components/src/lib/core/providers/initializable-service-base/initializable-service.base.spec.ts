import { takeUntil } from 'rxjs/operators';
import { ObservableStopper } from '../../../common';
import { InitializableServiceBase } from './initializable-service.base';

class MockService extends InitializableServiceBase {

    onServiceInitSpy = jasmine.createSpy('onServiceInitSpy').and.stub();
    onServiceDestroySpy = jasmine.createSpy('onServiceInitSpy').and.stub();

    get observableStopper(): ObservableStopper {
        return this.stopper;
    }

    protected onServiceInit(): void {
        this.onServiceInitSpy();
    }

    protected onServiceDestroy(): void {
        this.onServiceDestroySpy();
    }

}

describe('InitializableServiceBase', () => {

    let service: MockService;

    beforeEach(() => {
        service = new MockService();
    });

    describe('initialization and destruction', () => {

        it('init() calls onServiceInit() and supplies an ObservableStopper', () => {
            expect(service.onServiceInitSpy).not.toHaveBeenCalled();

            service.init();
            expect(service.onServiceInitSpy).toHaveBeenCalledTimes(1);
            expect(service.observableStopper instanceof ObservableStopper).toBe(true);
        });

        it('init() throws an error if called more than once', () => {
            service.init();
            expect(() => service.init()).toThrowError('This service was already initialized.');
        });

        it('onServiceDestroy() is called in ngOnDestroy() before the ObservableStopper is stopped', () => {
            service.init();

            service.onServiceDestroySpy.and.callFake(() => {
                expect(service.observableStopper.isStopped).toBe(false, 'onServiceDestroy() should be called before the stopper is stopped');
            });

            service.ngOnDestroy();
            expect(service.onServiceDestroySpy).toHaveBeenCalledTimes(1);
        });

    });

    describe('stopper', () => {

        let testStopper: ObservableStopper;

        beforeEach(() => {
            testStopper = new ObservableStopper();
        });

        afterEach(() => {
            testStopper.stop();
        });

        it('the ObservableStopper is stopped in ngOnDestroy()', () => {
            service.init();
            let stopped = false;

            service.observableStopper.stopper$.pipe(
                takeUntil(testStopper.stopper$),
            ).subscribe(() => stopped = true);

            service.ngOnDestroy();
            expect(stopped).toBe(true);
        });

    });

});
