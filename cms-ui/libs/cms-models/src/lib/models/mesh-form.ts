import { PagingMetaInfo, SchemaResponse } from '@gentics/mesh-models';

export interface GtxForm {
    name: string;
    description?: string;
    json?: JSON;
    pageId?: number;
}

export interface GtxFormWithUuid extends GtxForm {
    uuid: string;
}

export interface GtxFormResponse extends Omit<SchemaResponse, 'fields'> {
    fields: GtxForm;
}

export interface FormgeneratorListResponse extends PagingMetaInfo {
    forms: GtxFormWithUuid[];
}

export interface GtxFormCreateRequest {
    name: string;
    json: JSON;
    pageId?: number;
}

export interface GtxFormListOptions {
    currentPage: number;
    pageCount: number;
    perPage: number;
    totalCount: number;
    activeNodeId?: number;
    searchTerm?: string;
    languages?: string[];
}

export interface GtxFormEditorData {
    pageId: string;
    name: string;
    additional_infos: any;
    properties: any;
    id: string;
    type: string;
    object: any;
    elements: any[];
    data: {
        email: string;
        successurl: string;
        mailsubject_i18n: string;
        mailtemp_i18n: string;
    }
}
