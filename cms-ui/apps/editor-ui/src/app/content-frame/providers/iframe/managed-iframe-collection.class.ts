import {BehaviorSubject, Observable, Subject} from 'rxjs';
import {ManagedIFrame, ManagedIFrameEvent} from './managed-iframe.class';

export interface ManagedIFrameConstructor {
    new (...args: any[]): ManagedIFrame;
}

declare var window: Window & {
    gcmsui_getIFrames(): ManagedIFrame[];
};

/**
 * The purpose of the ManagedIFrameCollection is to aggregate many ManagedIFrame instances and expose their
 * lifecycle streams as a single aggregated stream, as well as providing methods for adding to and removing frames
 * from the collection.
 */
export class ManagedIFrameCollection {

    public domContentLoaded$: Observable<ManagedIFrameEvent>;
    public load$: Observable<ManagedIFrameEvent>;
    public beforeUnload$: Observable<ManagedIFrameEvent>;
    public unload$: Observable<ManagedIFrameEvent>;
    public unloadCancelled$: Observable<ManagedIFrameEvent>;
    public get masterFrame(): ManagedIFrame {
        return this._masterFrame;
    }
    public set managedIFrameCtor(value: ManagedIFrameConstructor) {
        if (typeof value !== 'function') {
            throw new Error(`managedIFrameCtor() expects a constructor function, got ${typeof value}.`);
        }
        this._managedIFrameCtor = value;
    }

    private _managedIFrameCtor: ManagedIFrameConstructor = ManagedIFrame;
    private _masterFrame: ManagedIFrame;
    public managedIFrames: ManagedIFrame[] = [];
    private managedIFrames$: Subject<ManagedIFrame[]>;

    constructor(masterFrame: ManagedIFrame) {
        window.gcmsui_getIFrames = () => this.managedIFrames;

        this._masterFrame = masterFrame;
        this.managedIFrames.push(masterFrame);
        this.managedIFrames$ = new BehaviorSubject(this.managedIFrames);

        let collectionChange$ = this.managedIFrames$;

        this.load$ = collectionChange$
            .switchMap(mifs => {
                let loads = mifs.map(mif => mif.load$);
                return Observable.merge(...loads);
            });

        this.domContentLoaded$ = collectionChange$
            .switchMap(mifs => {
                let domContentLoadedEvents = mifs.map(mif => mif.domContentLoaded$);
                return Observable.merge(...domContentLoadedEvents);
            });

        this.beforeUnload$ = collectionChange$
            .switchMap(mifs => {
                let beforeUnloads = mifs.map(mif => mif.beforeUnload$);
                return Observable.merge(...beforeUnloads);
            });

        this.unload$ = collectionChange$
            .switchMap(mifs => {
                let unloads = mifs.map(mif => mif.unload$);
                return Observable.merge(...unloads);
            });

        this.unloadCancelled$ = collectionChange$
            .switchMap(mifs => {
                let unloadCancelleds = mifs.map(mif => mif.unloadCancelled$);
                return Observable.merge(...unloadCancelleds);
            });
    }

    /**
     * Add a new iframe or ManagedIFrame to the collection.
     */
    add(frameOrManagedIFrame: ManagedIFrame | HTMLIFrameElement): void {
        let managedIFrame: ManagedIFrame;
        if (this.isIFrameElement(frameOrManagedIFrame)) {
            managedIFrame = new this._managedIFrameCtor(frameOrManagedIFrame);
        } else {
            managedIFrame = frameOrManagedIFrame;
        }
        this.managedIFrames.push(managedIFrame);
        this.managedIFrames$.next(this.managedIFrames);
    }

    /**
     * Remove a ManagedIFrame from the collection.
     */
    remove(frame: ManagedIFrame): void {
        let index = this.managedIFrames.indexOf(frame);
        if (-1 < index) {
            this.managedIFrames.splice(index, 1);
            this.managedIFrames$.next(this.managedIFrames);
        }
    }

    /**
     * Remove a ManagedIFrame based on the native iframe element which it wraps.
     */
    removeByNativeElement(iframe: HTMLIFrameElement): void {
        let index = this.managedIFrames.map(mif => mif.iframe).indexOf(iframe);
        if (-1 < index) {
            this.managedIFrames[index].destroy();
            this.managedIFrames.splice(index, 1);
            this.managedIFrames$.next(this.managedIFrames);
        }
    }

    /**
     * Remove all iframes from the collection apart from the master frame.
     */
    removeAllChildren(): void {
        for (let i = this.managedIFrames.length - 1; 0 < i; i--) {
            if (this.managedIFrames[i] !== this._masterFrame) {
                this.managedIFrames[i].destroy();
                this.managedIFrames.splice(i, 1);
            }
        }
        this.managedIFrames$.next(this.managedIFrames);
    }

    /**
     * In testing, there were strange cases where `iframe instanceof HTMLIFrameElement` -> false, even though
     * the object *was* an iframe. Also IE11 fails the instanceof and constructor name tests for valid iframes.
     */
    private isIFrameElement(element: any): element is HTMLIFrameElement {
        return (
            element instanceof HTMLIFrameElement
            || Object.prototype.toString.call(element) === '[object HTMLIFrameElement]'
            || element.tagName === 'IFRAME'
        );
    }
}
