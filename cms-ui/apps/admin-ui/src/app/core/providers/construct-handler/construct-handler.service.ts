import {
    EditableEntity,
    EntityCreateRequestModel,
    EntityCreateRequestParams,
    EntityCreateResponseModel,
    EntityEditorHandler,
    EntityLoadResponseModel,
    EntityUpdateRequestModel,
    EntityUpdateResponseModel,
} from '@admin-ui/common';
import { Injectable } from '@angular/core';
import { AnyModelType, TagType } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable()
export class ConstructHandlerService implements EntityEditorHandler<TagType, EditableEntity.CONSTRUCT> {

    constructor(
        protected api: GcmsApi,
    ) {}

    displayName(entity: TagType<AnyModelType>): string {
        return entity.name;
    }

    create(
        data: EntityCreateRequestModel<EditableEntity.CONSTRUCT>,
        options?: EntityCreateRequestParams<EditableEntity.CONSTRUCT>,
    ): Observable<EntityCreateResponseModel<EditableEntity.CONSTRUCT>> {
        return this.api.tagType.createTagType(data, options);
    }

    createMapped(
        data: EntityCreateRequestModel<EditableEntity.CONSTRUCT>,
        options?: EntityCreateRequestParams<EditableEntity.CONSTRUCT>,
    ): Observable<TagType> {
        return this.create(data, options).pipe(
            map(res => res.construct),
        );
    }

    get(id: string | number): Observable<EntityLoadResponseModel<EditableEntity.CONSTRUCT>> {
        return this.api.tagType.getTagType(id);
    }

    getMapped(id: string | number): Observable<TagType> {
        return this.get(id).pipe(
            map(res => res.construct),
        );
    }

    update(
        id: string | number,
        data: EntityUpdateRequestModel<EditableEntity.CONSTRUCT>,
    ): Observable<EntityUpdateResponseModel<EditableEntity.CONSTRUCT>> {
        return this.api.tagType.updateTagType(id, data);
    }

    updateMapped(
        id: string | number,
        data: EntityUpdateRequestModel<EditableEntity.CONSTRUCT>,
    ): Observable<TagType> {
        return this.update(id, data).pipe(
            map(res => res.construct),
        );
    }

    delete(id: string | number): Observable<void> {
        return this.api.tagType.deleteTagType(id);
    }
}
