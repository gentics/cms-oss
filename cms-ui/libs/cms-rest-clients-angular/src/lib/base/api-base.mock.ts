import {NEVER} from 'rxjs';
import {ApiBase} from './api-base.service';

// Although these interfaces are not directly used here, they must be imported to allow
// TypeScript to resolve the createListRequest signature when compiling in
// AoT mode. See https://github.com/Microsoft/TypeScript/issues/5711
import {FolderListOptions, FolderListRequest, PageListOptions, PageListRequest} from '@gentics/cms-models';

export class MockApiBase {
     createListRequest = ApiBase.prototype.createListRequest;
     get: any = jasmine.createSpy('ApiBase.get').and.returnValue(NEVER);
     post: any = jasmine.createSpy('ApiBase.post').and.returnValue(NEVER);
     put: any = jasmine.createSpy('ApiBase.put').and.returnValue(NEVER);
     delete: any = jasmine.createSpy('ApiBase.delete').and.returnValue(NEVER);
     upload: any = jasmine.createSpy('ApiBase.upload').and.returnValue(NEVER);
}

// This is unused, but is here just so that we use the imported types above.
// Webstorm can "optimize imports" which will remove all imported symbols which are
// not directly used in the file.
type internal = FolderListOptions | PageListOptions | PageListRequest | FolderListRequest;
