import { ItemType } from '@gentics/cms-models';

export interface UsageState {
    itemType: ItemType;
    itemId: number;
    linkedPages: number[];
    linkedFiles: number[];
    linkedImages: number[];
    files: number[];
    folders: number[];
    images: number[];
    pages: number[];
    tags: number[];
    templates: number[];
    variants: number[];
    fetching: boolean;
}
