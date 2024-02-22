import { waitForAsync } from '@angular/core/testing';
import { Observable, Subject, from } from 'rxjs';
import { MockManagedIFrame } from '../../../testing/iframe-helpers';
import { ManagedIFrameCollection } from './managed-iframe-collection';

describe('ManagedIFrameCollection', () => {

    describe('masterFrame only', () => {

        let collection: ManagedIFrameCollection;
        let mockFrame: MockManagedIFrame;

        beforeEach(() => {
            mockFrame = new MockManagedIFrame();
            collection = new ManagedIFrameCollection(<any> mockFrame);
        });

        it('should emit on DOMContentLoaded$', (done: DoneFn) => {
            collection.domContentLoaded$.subscribe(val => {
                expect(val).toBe(<any> 'test');
                done();
            });

            mockFrame.domContentLoaded$.next('test');
        });

        it('should emit on DOMContentLoaded$ multiple times', (done: DoneFn) => {
            let emitCount = 0;
            const values: any[] = [];
            collection.domContentLoaded$.subscribe(val => {
                values.push(val);
                emitCount ++;

                if (emitCount === 3) {
                    expect(values).toEqual([1, 2, 3]);
                    done();
                }
            });

            mockFrame.domContentLoaded$.next(1);
            mockFrame.domContentLoaded$.next(2);
            mockFrame.domContentLoaded$.next(3);
        });

        it('should emit on load$', (done: DoneFn) => {
            collection.load$.subscribe(val => {
                expect(val).toBe(<any> 'test');
                done();
            });

            mockFrame.load$.next('test');
        });

        it('should emit on beforeUnload$', (done: DoneFn) => {
            collection.beforeUnload$.subscribe(val => {
                expect(val).toBe(<any> 'test');
                done();
            });

            mockFrame.beforeUnload$.next('test');
        });

        it('should emit on unload$', (done: DoneFn) => {
            collection.unload$.subscribe(val => {
                expect(val).toBe(<any> 'test');
                done();
            });

            mockFrame.unload$.next('test');
        });

        it('should emit on unloadCancelled$', (done: DoneFn) => {
            collection.unloadCancelled$.subscribe(val => {
                expect(val).toBe(<any> 'test');
                done();
            });

            mockFrame.unloadCancelled$.next('test');
        });
    });

    describe('adding and removing', () => {
        let collection: ManagedIFrameCollection;
        let masterFrame: MockManagedIFrame;
        let childFrame1: MockManagedIFrame;
        let childFrame2: MockManagedIFrame;

        beforeEach(() => {
            masterFrame = new MockManagedIFrame();
            childFrame1 = new MockManagedIFrame();
            childFrame2 = new MockManagedIFrame();

            collection = new ManagedIFrameCollection(<any> masterFrame);
        });

        describe('add()', () => {

            it('should aggregate masterFrame with one childFrame', (done: DoneFn) => {
                let emitCount = 0;
                const values: any[] = [];
                collection.domContentLoaded$.subscribe(val => {
                    values.push(val);
                    emitCount ++;

                    if (emitCount === 2) {
                        expect(values).toEqual(['master', 'child']);
                        done();
                    }
                });

                collection.add(<any> childFrame1);

                masterFrame.domContentLoaded$.next('master');
                childFrame1.domContentLoaded$.next('child');
            });

            it('should aggregate masterFrame with two childFrames', (done: DoneFn) => {
                let emitCount = 0;
                const values: any[] = [];
                collection.domContentLoaded$.subscribe(val => {
                    values.push(val);
                    emitCount ++;

                    if (emitCount === 3) {
                        expect(values).toEqual(['master', 'child1', 'child2']);
                        done();
                    }
                });

                collection.add(<any> childFrame1);
                collection.add(<any> childFrame2);

                masterFrame.domContentLoaded$.next('master');
                childFrame1.domContentLoaded$.next('child1');
                childFrame2.domContentLoaded$.next('child2');

            });

            it('should aggregate masterFrame with one native iframe by contructing a new ManagedIFrame', (done: DoneFn) => {
                const nativeFrame = document.createElement('iframe');
                let emitCount = 0;
                const values: any[] = [];
                const childEvent = new Subject<any>();
                collection.managedIFrameCtor = <any> class {
                    domContentLoaded$ = from(childEvent);
                };
                collection.domContentLoaded$.subscribe(val => {
                    values.push(val);
                    emitCount ++;

                    if (emitCount === 2) {
                        expect(values).toEqual(['master', 'child']);
                        done();
                    }
                });

                collection.add(nativeFrame);

                masterFrame.domContentLoaded$.next('master');
                childEvent.next('child');
            });
        });

        describe('remove()', () => {

            beforeEach(() => {
                collection.add(<any> childFrame1);
                collection.add(<any> childFrame2);
            });

            it('should remove the correct childFrame', (done: DoneFn) => {
                let emitCount = 0;
                const values: any[] = [];
                collection.domContentLoaded$.subscribe(val => {
                    values.push(val);
                    emitCount ++;

                    if (emitCount === 2) {
                        expect(values).toEqual(['master', 'child2']);
                        done();
                    }
                });

                collection.remove(<any> childFrame1);

                masterFrame.domContentLoaded$.next('master');
                childFrame1.domContentLoaded$.next('child1');
                childFrame2.domContentLoaded$.next('child2');
            });

            it('should remove ManagedIframe which is not in collection', (done: DoneFn) => {
                let emitCount = 0;
                const values: any[] = [];
                collection.domContentLoaded$.subscribe(val => {
                    values.push(val);
                    emitCount ++;

                    if (emitCount === 3) {
                        expect(values).toEqual(['master', 'child1', 'child2']);
                        done();
                    }
                });

                collection.remove(<any> new MockManagedIFrame());

                masterFrame.domContentLoaded$.next('master');
                childFrame1.domContentLoaded$.next('child1');
                childFrame2.domContentLoaded$.next('child2');
            });

        });

        describe('removeByNativeElement()', () => {

            beforeEach(() => {
                collection.add(<any> childFrame1);
                collection.add(<any> childFrame2);
            });

            it('should remove the correct childFrame', (done: DoneFn) => {
                let emitCount = 0;
                const values: any[] = [];
                collection.domContentLoaded$.subscribe(val => {
                    values.push(val);
                    emitCount ++;

                    if (emitCount === 2) {
                        expect(values).toEqual(['master', 'child2']);
                        done();
                    }
                });

                collection.removeByNativeElement(<any> childFrame1.iframe);

                masterFrame.domContentLoaded$.next('master');
                childFrame1.domContentLoaded$.next('child1');
                childFrame2.domContentLoaded$.next('child2');
            });

            it('should invoke destroy() on the frame being removed', () => {
                const spy = spyOn(childFrame1, 'destroy');
                collection.removeByNativeElement(<any> childFrame1.iframe);
                expect(spy).toHaveBeenCalled();
            });

            it('should not remove iframe which is not in collection', (done: DoneFn) => {
                let emitCount = 0;
                const values: any[] = [];
                collection.domContentLoaded$.subscribe(val => {
                    values.push(val);
                    emitCount ++;

                    if (emitCount === 3) {
                        expect(values).toEqual(['master', 'child1', 'child2']);
                        done();
                    }
                });

                collection.removeByNativeElement({} as HTMLIFrameElement);

                masterFrame.domContentLoaded$.next('master');
                childFrame1.domContentLoaded$.next('child1');
                childFrame2.domContentLoaded$.next('child2');
            });
        });

        describe('removeAllChildren()', () => {

            it('should remove all children', waitForAsync(() => {
                const childFrame1 = new MockManagedIFrame();
                const childFrame2 = new MockManagedIFrame();
                collection.add(<any> childFrame1);
                collection.add(<any> childFrame2);

                collection.domContentLoaded$.subscribe(val => {
                    expect(val).toEqual(<any> 'master');
                });

                collection.removeAllChildren();

                childFrame1.domContentLoaded$.next('child1');
                childFrame2.domContentLoaded$.next('child2');
                masterFrame.domContentLoaded$.next('master');
            }));


            it('should invoke destroy() on child frames', () => {
                const childFrame1 = new MockManagedIFrame();
                const childFrame2 = new MockManagedIFrame();
                collection.add(<any> childFrame1);
                collection.add(<any> childFrame2);

                const spy1 = spyOn(childFrame1, 'destroy');
                const spy2 = spyOn(childFrame2, 'destroy');

                collection.removeAllChildren();

                expect(spy1).toHaveBeenCalled();
                expect(spy2).toHaveBeenCalled();
            });
        });
    });
});
