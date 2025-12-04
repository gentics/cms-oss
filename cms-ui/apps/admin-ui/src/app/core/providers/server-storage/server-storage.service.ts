import { Injectable } from '@angular/core';
import { IndexByKey, UserDataResponse } from '@gentics/cms-models';
import { ApiError, GcmsApi } from '@gentics/cms-rest-clients-angular';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { ServiceBase } from '../../../shared/providers/service-base/service.base';

@Injectable()
export class ServerStorageService extends ServiceBase {
    /**
     * Determines whether or not saving user data is supported by the server.
     * Defaults to "unknown" until a request returns.
     */
    supported$ = new BehaviorSubject<boolean | 'unknown'>('unknown');

    /**
     * Determines whether or not saving user data is supported by the server.
     * Defaults to "unknown" until a request returns.
     */
    get supported(): boolean | 'unknown' {
        return this.supported$.value;
    }

    /**
     * Emits the userData when it changes after loading/saving
     */
    userData$ = new BehaviorSubject<IndexByKey<any>>({});

    constructor(
        private api: GcmsApi,
    ) {
        super();
    }

    /**
     * Load all user data stored on the server.
     * Returns an Observable of an empty object if the server does not support storing data.
     */
    getAll(): Observable<IndexByKey<any>> {
        if (this.supported$.value === false) {
            return of({});
        }

        return this.api.userData.getAllKeys().pipe(
            map((response) => response.data),
            tap(() => this.markAsSupported()),
            catchError((err, response) => this.checkIfUnsupported(err)
                ? of({})
                : throwError(err)),
            tap((data) => this.userData$.next(data)),
        );
    }

    /**
     * Load a specific user data key from the server.
     * Returns an Observable of null if the server does not support storing data.
     */
    get(key: string): Observable<any> {
        if (this.supported$.value === false) {
            return of(null);
        }

        return this.api.userData.getKey(key).pipe(
            map((response) => response.data),
            tap((data) => this.markAsSupported()),
            catchError((err, response) => this.checkIfUnsupported(err)
                ? of(null)
                : throwError(err)),
            tap((data) => this.userData$.next(Object.assign({}, this.userData$, { [key]: data }))),
        );
    }

    /**
     * Save a specific user data key stored on the server.
     * Silently resolves if the server does not support saving user data, throws on error.
     */
    set<T>(key: string, data: T): Promise<T> {
        if (this.supported$.value === false) {
            return Promise.resolve(data);
        }

        return this.api.userData.setKey(key, data)
            .toPromise()
            .then(() => {
                this.markAsSupported();
                this.userData$.next(Object.assign({}, this.userData$.value, { [key]: data }));
                return data;
            })
            .catch((err: Error) => {
                if (this.checkIfUnsupported(err)) {
                    return data;
                } else {
                    throw err;
                }
            });
    }

    private markAsSupported(): void {
        if (this.supported$.value === 'unknown') {
            this.supported$.next(true);
        }
    }

    private checkIfUnsupported(error: Error): boolean {
        if (error instanceof ApiError) {
            const response = error.response as UserDataResponse;
            const unsupported = (response && response.responseInfo && response.responseInfo.responseCode === 'FAILURE'
              && response.messages && response.messages.length > 0 && response.messages[0].type === 'CRITICAL');

            if (unsupported && this.supported$.value === 'unknown') {
                this.supported$.next(false);
            }

            return unsupported;
        }
        return false;
    }
}
