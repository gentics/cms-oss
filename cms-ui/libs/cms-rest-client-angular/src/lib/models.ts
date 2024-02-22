import { GCMSRestClientResponse } from '@gentics/cms-rest-client';
import { Observable } from 'rxjs';

export interface NGGCMSRestClientResponse<T> extends GCMSRestClientResponse<T> {
    rx(): Observable<T>;
}
