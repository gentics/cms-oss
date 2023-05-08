import {Injectable} from '@angular/core';

/**
 * Can be used to inject a mock in unit tests.
 */
@Injectable()
export class RequestFactory {
    create(): XMLHttpRequest {
        return new XMLHttpRequest();
    }
}
