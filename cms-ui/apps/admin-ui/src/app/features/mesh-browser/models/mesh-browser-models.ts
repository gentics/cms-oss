import { FieldType, NodeResponse, PagingOptions, SchemaField } from '@gentics/mesh-models';

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
    elements: SchemaElement[];
    fields: SchemaField[];
}

export interface NumberOfSchemaElements {
    schemaName: {[key:string]: string }
    totalCount: number;
}

export interface SchemaPage {
    elements: SchemaElement[];
    hasNextPage: boolean;
    pageCount: number;
    totalCount: number;
    currentPage: number;
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
    availableLanguages: string[];
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

export interface BreadcrumbNode {
    uuid: string;
    displayName?: string;
}

export interface ResolvedParentNode extends Pick<NodeResponse, 'uuid' | 'availableLanguages' | 'language' | 'displayName'> {
    breadcrumb: NavigationEntry[];
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
