import { FieldType, PagingOptions } from '@gentics/mesh-models';

export interface MeshSchemaListParams extends PagingOptions {
    schemaName?: string;
    nodeUuid: string;
    lang?: Array<string>;
}

export interface MeshSchemaListResponse {
    rootNodeUuid: string;
    schemas: Array<SchemaContainer>;
}

export interface SchemaContainer {
    name: string;
    elements: Array<SchemaElement>;
}

export interface SchemaElement {
    uuid: string;
    displayName: string;
    isContainer: boolean;
    isPublished: boolean;
    isDraft: boolean;
    version: string;
    versions?: SchemaElementVersion[];
    language: string;
    languages: Array<ElementLanguage>;
}

export interface SchemaElementVersion {
    version: string;
    published: string;
    draft: string;
    created: string;
}

export enum PublishedState {
    DRAFT = 'DRAFT',
    UPDATED = 'UPDATED',
    PUBLISHED = 'PUBLISHED',
    ARCHIVED = 'ARCHIVED',
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

export enum ResizeMode {
    PROP = 'prop',
    SMART = 'smart',
    FORCE = 'force',
}
