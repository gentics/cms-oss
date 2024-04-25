import { GCMSRestClientRequest } from '@gentics/cms-rest-client';
import { Observable } from 'rxjs';

export interface NGGCMSRestClientRequest<T> extends GCMSRestClientRequest<T> {
    rx(): Observable<T>;
}
