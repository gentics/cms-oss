import { SizeTrackerService } from '../size-tracker/size-tracker.service';
import {OverlayHostService} from './overlay-host.service';

let overlayHostService: OverlayHostService;
const dummyHostView: any = 'dummy_hostview';

describe('OverlayHostService:', () => {

    beforeEach(() => {
        overlayHostService = new OverlayHostService(new SizeTrackerService());
    });

    describe('getHostView()', () => {
        it('should return a promise', () => {
            expect(typeof overlayHostService.getHostView().then).toBe('function');
        });

        it('should resolve immediately if view is already registered', (done) => {
            overlayHostService.registerHostView(dummyHostView);
            const promise = overlayHostService.getHostView();
            promise.then(
                (val: any) => {
                    expect(val).toBe(dummyHostView);
                    done();
                },
                error => done.fail(error),
            );
        });

        it('should resolve when the view is registered later', (done) => {
            const promise = overlayHostService.getHostView();
            promise.then(
                (val: any) => {
                    expect(val).toBe(dummyHostView);
                    done();
                },
                error => done.fail(error),
            );
            overlayHostService.registerHostView(dummyHostView);
        });

        it('should resolve multiple consumers', (done) => {
            const promise1 = overlayHostService.getHostView();
            const promise2 = overlayHostService.getHostView();
            Promise.all([promise1, promise2]).then(
                (results: any[]) => {
                    expect(results[0]).toBe(dummyHostView);
                    expect(results[1]).toBe(dummyHostView);
                    done();
                },
                error => done.fail(error),
            );
            overlayHostService.registerHostView(dummyHostView);
        });
    });

});
