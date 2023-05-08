import { Group } from './group';
import { DefaultModelType, ModelType, Normalizable, NormalizableEntity, Raw } from './type-util';

/**
 * A GCMS user.
 */
export interface User<T extends ModelType = DefaultModelType> extends NormalizableEntity<T> {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    login?: string;
    description?: string;
    groups?: Normalizable<T, Group<Raw>, number>[];
}
