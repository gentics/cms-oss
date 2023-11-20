import { FieldType, PagingOptions } from '@gentics/mesh-models';


export interface MeshSchemaListParams extends PagingOptions  {
    schemaName?: string,
    nodeUuid: string,
    lang?: Array<string>,
}

export interface MeshSchemaListResponse {
    rootNodeUuid: string,
    schemas: Array<SchemaContainer>
}

export interface SchemaContainer {
    name: string;
    elements: Array<SchemaElement>;
}

export interface SchemaElement {
    uuid: string;
    displayName: string;
    isContainer: boolean;
    language: string;
    languages: Array<ElementLanguage>;
}

export interface ElementLanguage {
    language: string;
}

export interface MeshField {
    label: string;
    value: string;
    type: FieldType;
}

export interface BreadcrumbNode {
    uuid: string;
    displayName?: string;
}

export interface NavigationEntry {
    parent: NavigationEntry;
    node: BreadcrumbNode;
}


export enum RESIZE_MODE {
    PROP = 'prop',
    SMART = 'smart',
    FORCE = 'force'
}