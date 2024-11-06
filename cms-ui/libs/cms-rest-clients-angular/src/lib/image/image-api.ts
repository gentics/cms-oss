import { EntityIdType, ImageRequestOptions, ImageResponse } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';

export class ImageApi {
    constructor(
        private apiBase: ApiBase,
    ) {}

    getImage(id: EntityIdType, options?: ImageRequestOptions): Observable<ImageResponse> {
        return this.apiBase.get(`image/load/${id}`, options);
    }
}
