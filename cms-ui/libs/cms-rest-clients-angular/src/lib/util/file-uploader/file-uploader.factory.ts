import {Inject, Injectable, Optional} from '@angular/core';

import {FileUploader} from './file-uploader.service';
import {RequestFactory} from './request.factory';

@Injectable()
export class FileUploaderFactory {
    private requestFactory: RequestFactory;
    constructor(@Inject(RequestFactory) @Optional() requests: RequestFactory) {
        this.requestFactory = requests || new RequestFactory();
    }

    create(): FileUploader {
        return new FileUploader(this.requestFactory);
    }
}
