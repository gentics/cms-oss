import { Page } from '@gentics/cms-models';

export interface LanguageVariantMap {
    [pageId: number]: Page[];
}
