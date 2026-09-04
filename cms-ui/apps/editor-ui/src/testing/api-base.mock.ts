// Yes a copy paste from the original, but there're no changes in the old one
// as it is deprecated and would be too much efford to make it work properly.
import { NEVER } from 'rxjs';

// Although these interfaces are not directly used here, they must be imported to allow
// TypeScript to resolve the createListRequest signature when compiling in
// AoT mode. See https://github.com/Microsoft/TypeScript/issues/5711
import { FolderListOptions, FolderListRequest, PageListOptions, PageListRequest } from '@gentics/cms-models';
import { ApiBase } from '@gentics/cms-rest-clients-angular';

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
