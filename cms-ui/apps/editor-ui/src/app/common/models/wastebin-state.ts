import { SortField } from '@gentics/cms-models';

export interface WastebinTypeList {
    list: number[];
    requesting: boolean;
}

export interface WastebinState {
    folder: WastebinTypeList;
    form: WastebinTypeList;
    page: WastebinTypeList;
    file: WastebinTypeList;
    image: WastebinTypeList;
    sortBy?: SortField;
    sortOrder?: 'asc' | 'desc';
    lastError?: string;
}
