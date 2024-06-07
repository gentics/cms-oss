import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import {NgModule} from '@angular/core';

import {GCMS_API_ERROR_HANDLER} from '../lib/base/api-base.service';
import {AngularErrorHandler} from './error/error-handler';
import {API_PROVIDERS} from './gcms-api.service';
import {FileUploaderFactory} from './util/file-uploader/file-uploader.factory';
import {FileUploader} from './util/file-uploader/file-uploader.service';

// IMPORTANT: NEVER import an element from the local project through a barrel index.ts file if one of the following conditions applies:
// - the element is used in the `providers` array of an NgModule declaration or
// - the element is supposed to be injected into a constructor.
//
// E.g. use `import {FileUploaderFactory} from './util/file-uploader/file-uploader.factory';`
// instead of `import {FileUploaderFactory} from './util/file-uploader';`
//
// Otherwise the following error may occur when using this library in an app that is compiled in AOT mode:
// "Encountered undefined provider! Usually this means you have a circular dependencies. This might be caused by using 'barrel' index.ts files."

/**
 * This module provides services for communicating with the GCMS REST API.
 */
@NgModule({ declarations: [],
    exports: [], imports: [], providers: [
        FileUploader,
        FileUploaderFactory,
        { provide: GCMS_API_ERROR_HANDLER, useClass: AngularErrorHandler },
        ...API_PROVIDERS,
        provideHttpClient(withInterceptorsFromDi())
    ] })
export class GcmsRestClientsAngularModule {}
