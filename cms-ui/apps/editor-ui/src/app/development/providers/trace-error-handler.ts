import {ErrorHandler, Injectable} from '@angular/core';
import * as StackTrace from 'stacktrace-js';
import { ReplaySubject } from 'rxjs';

@Injectable()
export class TraceErrorHandler implements ErrorHandler {

    static collectErrors$: ReplaySubject<any> = new ReplaySubject();

    handleError(error: any): void {
        StackTrace.fromError(error, { offline: true }).then((trace) => {
            TraceErrorHandler.collectErrors$.next({error, trace});
        });
        console.error(error);
    }
}
