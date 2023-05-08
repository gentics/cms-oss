import { Page, PagingSortOption } from '@gentics/cms-models';

export interface FilterOptions {
    nodeId: number;
    editable: boolean;
    isCreator: boolean;
    isEditor: boolean;
    languages: number[];
    page: number;
    pageSize: number;
    searchTerm: string;
    sortOptions: PagingSortOption<Page>[];
    status: 'invalid' | 'unchecked' | 'valid';
    online: boolean;
}
