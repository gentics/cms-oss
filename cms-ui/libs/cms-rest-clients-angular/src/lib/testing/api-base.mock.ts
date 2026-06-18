import { NEVER } from 'rxjs';
import { ApiBase } from '../base/api-base.service';

/**
 * Used for mocking `ApiBase` in unit tests.
 *
 * As long as we do not have a distinct npm package with test utilities we need to duplicate
 * this class here, because including it in @gentics/cms-rest-clients-angular causes
 * build errors in AOT mode.
 */
export class MockApiBase {
    createListRequest = ApiBase.prototype.createListRequest;
    get: any = jasmine.createSpy('ApiBase.get').and.returnValue(NEVER);
    post: any = jasmine.createSpy('ApiBase.post').and.returnValue(NEVER);
    put: any = jasmine.createSpy('ApiBase.put').and.returnValue(NEVER);
    delete: any = jasmine.createSpy('ApiBase.delete').and.returnValue(NEVER);
    upload: any = jasmine.createSpy('ApiBase.upload').and.returnValue(NEVER);
}
