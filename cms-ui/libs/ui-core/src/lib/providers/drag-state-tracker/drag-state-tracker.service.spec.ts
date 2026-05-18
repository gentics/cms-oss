import { SpyEventTarget, subscribeSpyObserver, triggerFakeDragEvent } from '@gentics/ui-core/testing';
import { Observable } from 'rxjs';
import { DragStateTrackerFactoryService, FileDragState } from './drag-state-tracker.service';

describe('DragStateTrackerFactory', () => {
    describe('trackElement()', () => {
        it('returns an Observable', () => {
            const fakeEventTarget = new SpyEventTarget();
            const factory = new DragStateTrackerFactoryService();
            const result = factory.trackElement(fakeEventTarget);
            expect(result).toBeDefined();
            expect(typeof result.subscribe).toBe('function');
        });
    });
});

describe('DragStateTracker', () => {

    let eventTarget: SpyEventTarget;
    let tracker: Observable<FileDragState>;

    beforeEach(() => {
        eventTarget = new SpyEventTarget();
        const factory = new DragStateTrackerFactoryService();
        tracker = factory.trackElement(eventTarget);
    });

    it('does not add event listeners until subscribed to', () => {
        expect(eventTarget.addEventListener).not.toHaveBeenCalled();
    });

    it('adds event listeners for "dragenter", "dragleave" and "drop"', () => {
        subscribeSpyObserver(tracker);
        expect(eventTarget.addEventListener).toHaveBeenCalled();
        expect(eventTarget.hasListener('dragenter')).toBe(true);
        expect(eventTarget.hasListener('dragleave')).toBe(true);
        expect(eventTarget.hasListener('drop')).toBe(true);
    });

    it('removes all event listeners it added when unsubscribed', () => {
        const sub = tracker.subscribe(() => {});
        try {
            expect(eventTarget.addEventListener).toHaveBeenCalled();
            expect(eventTarget.listeners.length).toBeGreaterThan(0);
            sub.unsubscribe();
            expect(eventTarget.removeEventListener).toHaveBeenCalled();
            expect(eventTarget.listeners.length).toBe(0);
        } finally {
            sub.unsubscribe();
        }
    });

    it('does not add event listeners on every subscription', () => {
        subscribeSpyObserver(tracker);
        const listenersAfterFirstSubscribe = eventTarget.listeners.length;
        expect(listenersAfterFirstSubscribe).toBeGreaterThan(0);
        subscribeSpyObserver(tracker);
        expect(eventTarget.listeners.length).toBe(listenersAfterFirstSubscribe);
    });

    it('only removes the event listeners after the last subscription was unsubscribed', () => {
        const subA = tracker.subscribe(() => {});
        const subB = tracker.subscribe(() => {});
        try {
            subA.unsubscribe();
            expect(eventTarget.removeEventListener).not.toHaveBeenCalled();
            expect(eventTarget.listeners.length).not.toBe(0);
            subB.unsubscribe();
            expect(eventTarget.removeEventListener).toHaveBeenCalled();
            expect(eventTarget.listeners.length).toBe(0);
        } finally {
            subA.unsubscribe();
            subB.unsubscribe();
        }
    });

    it('emits no value before an event happens', () => {
        const observer = subscribeSpyObserver(tracker);
        expect(observer.next).not.toHaveBeenCalled();
        expect(observer.complete).not.toHaveBeenCalled();
        expect(observer.error).not.toHaveBeenCalled();
    });

    it('emits a list of file types on "dragenter"', () => {
        const observer = subscribeSpyObserver(tracker);

        triggerFakeDragEvent(eventTarget, 'dragenter', ['image/jpeg']);
        expect(observer.next).toHaveBeenCalledTimes(1);
        expect(observer.next).toHaveBeenCalledWith([{ type: 'image/jpeg' }]);
        expect(observer.complete).not.toHaveBeenCalled();
        expect(observer.error).not.toHaveBeenCalled();
    });

    it('does not emit anything on a not-a-file "dragenter"', () => {
        const observer = subscribeSpyObserver(tracker);

        const NO_FILES = <any[]> [];
        triggerFakeDragEvent(eventTarget, 'dragenter', NO_FILES);

        expect(observer.next).not.toHaveBeenCalled();
        expect(observer.complete).not.toHaveBeenCalled();
        expect(observer.error).not.toHaveBeenCalled();
    });

    it('emits an empty array on "dragleave"', () => {
        const observer = subscribeSpyObserver(tracker);
        triggerFakeDragEvent(eventTarget, 'dragenter', ['image/jpeg']);
        observer.next.calls.reset();

        triggerFakeDragEvent(eventTarget, 'dragleave', ['image/jpeg']);
        expect(observer.next).toHaveBeenCalledTimes(1);
        expect(observer.next).toHaveBeenCalledWith([]);
        expect(observer.complete).not.toHaveBeenCalled();
        expect(observer.error).not.toHaveBeenCalled();
    });

    it('emits an empty array on "drop"', () => {
        const observer = subscribeSpyObserver(tracker);
        triggerFakeDragEvent(eventTarget, 'dragenter', ['image/jpeg']);
        observer.next.calls.reset();

        triggerFakeDragEvent(eventTarget, 'drop', ['image/jpeg']);
        expect(observer.next).toHaveBeenCalledTimes(1);
        expect(observer.next).toHaveBeenCalledWith([]);
        expect(observer.complete).not.toHaveBeenCalled();
        expect(observer.error).not.toHaveBeenCalled();
    });

    it('emits an empty array if the mouse enters with no "dragleave"/"drop" before', () => {
        const observer = subscribeSpyObserver(tracker);
        triggerFakeDragEvent(eventTarget, 'dragenter', ['image/jpeg']);
        observer.next.calls.reset();

        eventTarget.triggerListeners('mouseenter', {
            type: 'mouseenter',
            button: 0,
            buttons: 0,
        });
        expect(observer.next).toHaveBeenCalledTimes(1);
        expect(observer.next).toHaveBeenCalledWith([]);
        expect(observer.complete).not.toHaveBeenCalled();
        expect(observer.error).not.toHaveBeenCalled();
    });

});
