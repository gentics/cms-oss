import { PagingOptions } from '@gentics/mesh-models';


export interface MeshSchemaListParams extends PagingOptions  {
    schemaName?: string,
    nodeUuid: string,
}

export interface MeshSchemaListResponse {
    rootNodeUuid: string,
    schemas: Array<Schema>
}

export interface Schema {
    name: string;
    elements: Array<SchemaElement>;
}

export interface SchemaElement {
    uuid: string;
    displayName: string;
    isContainer: boolean;
    languages: Array<ElementLanguage>;
}

export interface ElementLanguage {
    language: string;
}

