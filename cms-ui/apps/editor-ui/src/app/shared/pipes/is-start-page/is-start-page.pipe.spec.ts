import { Folder, GcmsTestData, Raw, Normalized, NormalizedEntityStore, Page, File, Image } from '@gentics/cms-models';
import { getExamplePageData } from '@gentics/cms-models/testing/test-data.mock';

import {IsStartPagePipe} from './is-start-page.pipe';

describe('IsStartPagePipe', () => {

    const mockEntities: NormalizedEntityStore = GcmsTestData.getExampleEntityStore();
    const FOLDER_ID = 1;
    const PAGE_ID_OF_PAGE_WITH_LANGUAGE_VARIANTS = 3;
    const PAGE_ID_OF_PAGE_WITHOUT_LANGUAGE_VARIANTS = 37;
    const FILE_ID = 10;
    const IMAGE_ID = 10;

    let pipe: IsStartPagePipe;

    beforeEach(() => {
        pipe = new IsStartPagePipe();
    });

    describe('transform', () => {
        it('called with item but without startPageId should return false', () => {
            const item: Folder<Raw | Normalized> | Page<Raw | Normalized> | File<Raw | Normalized> | Image<Raw | Normalized>
                = mockEntities.page[PAGE_ID_OF_PAGE_WITH_LANGUAGE_VARIANTS];
            const result = pipe.transform(item);
            expect(result).toBe(false);
        });

        it('called with item but with startPageId that is undefined should return false', () => {
            const item: Folder<Raw | Normalized> | Page<Raw | Normalized> | File<Raw | Normalized> | Image<Raw | Normalized>
                = mockEntities.page[PAGE_ID_OF_PAGE_WITH_LANGUAGE_VARIANTS];
            const startPageId: number = undefined;
            const result = pipe.transform(item, startPageId);
            expect(result).toBe(false);
        });

        it('called with item but with startPageId that is null should return false', () => {
            const item: Folder<Raw | Normalized> | Page<Raw | Normalized> | File<Raw | Normalized> | Image<Raw | Normalized>
                = mockEntities.page[PAGE_ID_OF_PAGE_WITH_LANGUAGE_VARIANTS];
            const startPageId: number = null;
            const result = pipe.transform(item, startPageId);
            expect(result).toBe(false);
        });

        it('called with folder and correct startPageId should return false', () => {
            const item: Folder<Raw | Normalized> = mockEntities.folder[FOLDER_ID];
            const startPageId: number = 1;
            const result = pipe.transform(item, startPageId);
            expect(result).toBe(false);
        });

        it('called with file and correct startPageId should return false', () => {
            const item: File<Raw | Normalized> = mockEntities.file[FILE_ID];
            const startPageId: number = 1;
            const result = pipe.transform(item, startPageId);
            expect(result).toBe(false);
        });

        it('called with image and correct startPageId should return false', () => {
            const item: Image<Raw | Normalized> = mockEntities.image[IMAGE_ID];
            const startPageId: number = 1;
            const result = pipe.transform(item, startPageId);
            expect(result).toBe(false);
        });

        it('called with page without language variants and an id that matches startPageId should return true', () => {
            const item: Page<Raw | Normalized> = mockEntities.page[PAGE_ID_OF_PAGE_WITHOUT_LANGUAGE_VARIANTS];
            const startPageId: number = 37;
            const result = pipe.transform(item, startPageId);
            expect(result).toBe(true);
        });

        it('called with page without language variants and an id that does not match startPageId should return false', () => {
            const item: Page<Raw | Normalized> = mockEntities.page[PAGE_ID_OF_PAGE_WITHOUT_LANGUAGE_VARIANTS];
            const startPageId: number = 3;
            const result = pipe.transform(item, startPageId);
            expect(result).toBe(false);
        });

        it('called with normalized page with language variants and a startPageId that matches a language variant should return true', () => {
            const item: Page<Normalized> = mockEntities.page[PAGE_ID_OF_PAGE_WITH_LANGUAGE_VARIANTS];
            const startPageId: number = 37;
            const result = pipe.transform(item, startPageId);
            expect(result).toBe(true);
        });

        it('called with normalized page with language variants and a startPageId that matches no language variant should return false', () => {
            const item: Page<Normalized> = mockEntities.page[PAGE_ID_OF_PAGE_WITH_LANGUAGE_VARIANTS];
            const startPageId: number = 1;
            const result = pipe.transform(item, startPageId);
            expect(result).toBe(false);
        });

        it('called with raw page with language variants and a startPageId that matches a language variant should return true', () => {
            const item: Page<Raw> = getExamplePageData();
            const startPageId: number = 48;
            const result = pipe.transform(item, startPageId);
            expect(result).toBe(true);
        });

        it('called with raw page with language variants and a startPageId that matches no language variant should return false', () => {
            const item: Page<Raw> = getExamplePageData();
            const startPageId: number = 49;
            const result = pipe.transform(item, startPageId);
            expect(result).toBe(false);
        });

    });

});
