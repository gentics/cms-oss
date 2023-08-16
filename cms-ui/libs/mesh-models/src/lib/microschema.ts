/* eslint-disable @typescript-eslint/naming-convention */
import {
    BasicListOptions,
    BranchedEntityOptions,
    ElasticSearchSettings,
    ListResponse,
    RolePermissionsOptions,
    VersionedEntity,
    VersionedEntityOptions,
} from './common';
import { FieldSchema } from './fields';

export interface EditableMicroschemaProperties {
    /** Name of the microschema */
    name: string;
    /** Description of the microschema */
    description?: string;
    /**
     * Additional search index configuration. This can be used to setup custom analyzers
     * and filters.
     */
    elasticsearch?: ElasticSearchSettings;
    /** List of microschema fields */
    fields: FieldSchema[];
}

export interface Microschema extends EditableMicroschemaProperties, VersionedEntity {}

export interface MicroschemaCreateRequest extends EditableMicroschemaProperties {}

export interface MicroschemaLoadOptions extends BranchedEntityOptions, VersionedEntityOptions {}

export interface MicroschemaListOptions extends BasicListOptions, RolePermissionsOptions { }

export type MicroschemaListResponse = ListResponse<MicroschemaResponse>;

export interface MicroschemaReference {
    uuid?: string;
    name?: string;
}

export interface MicroschemaResponse extends Microschema {}

export interface MicroschemaUpdateRequest extends Partial<EditableMicroschemaProperties> {
    /** Version of the microschema */
    version?: string;
}
