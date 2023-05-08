import { Injectable } from '@angular/core';
import { Observable, BehaviorSubject } from 'rxjs';

/**
 * @description This service is supposed to handle all cross-component information
 * which is irrelevant logic-wise but has impact on visual appearance.
 */
@Injectable()
export class PresentationService {

    /** Get async vertical distance of main container component gtx-split-view from window top */
    get headerHeight$(): Observable<string | null> {
        return this._headerHeight$.asObservable();
    }
    /** Get vertical distance of main container component gtx-split-view from window top */
    get headerHeight(): string | null {
        return this._headerHeight$.getValue();
    }
    /** Set async vertical distance of main container component gtx-split-view from window top */
    set headerHeight(v: string) {
        this._headerHeight$.next(v);
    }
    private _headerHeight$ = new BehaviorSubject<string>(null);

}
