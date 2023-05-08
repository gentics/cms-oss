import {
    EntityIdType, TagmapEntryParentType,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { EntityGridDataProvider } from './entity-grid-data-provider';

export interface Parent {
    type: TagmapEntryParentType;
    id: EntityIdType;
}

export interface ChildGridDataProvider<E> extends EntityGridDataProvider<E> {
    getParentEntity(): Observable<Parent>;
}
