import { cloneDeep } from 'lodash-es';
import { getExampleEntityStore } from '../../testing';
import { File, FileOrImage } from '../file';
import { Folder } from '../folder';
import { Group } from '../group';
import { Image } from '../image';
import { Language } from '../language';
import { Message } from '../message';
import { Node } from '../node';
import { Page } from '../page';
import { Template, TemplateBO } from '../template';
import { IS_NORMALIZED, IndexById, Normalized, Raw } from '../type-util';
import { User } from '../user';
import { GcmsNormalizer } from './gcms-normalizer';
import { NormalizedEntityStore } from './gcms-normalizer-types';

describe('GcmsNormalizer', () => {

    const mockEntities: NormalizedEntityStore = getExampleEntityStore();

    function replaceUser<T>(entity: T, key: keyof T): void {
        const userId: number = entity[key] as any;
        entity[key] = denormalizeUser(mockEntities.user[userId] as any) as any;
    }

    function denormalizePage(page: Page<Normalized>): Page<Raw> {
        const denormalizedPage: Page<Raw> = cloneDeep(page) as any;
        replaceUser(denormalizedPage, 'creator');
        replaceUser(denormalizedPage, 'editor');
        replaceUser(denormalizedPage, 'publisher');
        delete denormalizedPage[IS_NORMALIZED];

        denormalizedPage.template = cloneDeep(mockEntities.template[page.template]) as any;
        replaceUser(denormalizedPage.template, 'creator');
        replaceUser(denormalizedPage.template, 'editor');

        if (page.folder) {
            denormalizedPage.folder = denormalizeFolder(mockEntities.folder[page.folder] as any);
        }

        const denormalizeSubPage = (subPageId: number) => {
            const subPage: Page<Raw> = cloneDeep(mockEntities.page[subPageId]) as any;
            replaceUser(subPage, 'creator');
            replaceUser(subPage, 'editor');
            replaceUser(subPage, 'publisher');
            if (subPage.folder) {
                subPage.folder = denormalizeFolder(mockEntities.folder[subPage.folderId] as any);
            }
            delete subPage.languageVariants;
            delete subPage.pageVariants;
            delete subPage.template;
            return subPage;
        };

        if (denormalizedPage.languageVariants) {
            Object.keys(denormalizedPage.languageVariants).forEach(langId => {
                denormalizedPage.languageVariants[langId] = denormalizeSubPage(denormalizedPage.languageVariants[langId] as any);
            });
        }
        if (denormalizedPage.pageVariants) {
            denormalizedPage.pageVariants = denormalizedPage.pageVariants.map(id => denormalizeSubPage(id as any));
        }

        return denormalizedPage;
    }

    function denormalizeFolder(folder: Folder<Normalized>): Folder<Raw> {
        const denormalizedFolder: Folder<Raw> = cloneDeep(folder) as any;
        replaceUser(denormalizedFolder, 'creator');
        replaceUser(denormalizedFolder, 'editor');
        delete denormalizedFolder[IS_NORMALIZED];

        if (denormalizedFolder.subfolders) {
            denormalizedFolder.subfolders = denormalizedFolder.subfolders.map(subfolderId => {
                const subfolder: Folder<Raw> = cloneDeep(mockEntities.folder[subfolderId as any]) as any;
                replaceUser(subfolder, 'creator');
                replaceUser(subfolder, 'editor');
                delete subfolder.subfolders;
                return subfolder;
            });
        }

        return denormalizedFolder;
    }

    function denormalizeFile(file: FileOrImage<Normalized>): FileOrImage<Raw> {
        const denormalizedFile: FileOrImage<Raw> = cloneDeep(file) as any;
        denormalizedFile.folder = denormalizeFolder(mockEntities.folder[denormalizedFile.folderId] as any);
        replaceUser(denormalizedFile, 'creator');
        replaceUser(denormalizedFile, 'editor');
        delete denormalizedFile[IS_NORMALIZED];
        return denormalizedFile;
    }

    function denormalizeUser(user: User<Normalized>): User<Raw> {
        const denormalizedUser: User<Raw> = cloneDeep(user) as any;
        if (denormalizedUser.groups) {
            denormalizedUser.groups = denormalizedUser.groups.map(groupId =>
                denormalizeGroup(mockEntities.group[groupId as any] as any),
            );
        }
        delete denormalizedUser[IS_NORMALIZED];
        return denormalizedUser;
    }

    function denormalizeGroup(group: Group<Normalized>): Group<Raw> {
        const denormalizedGroup: Group<Raw> = cloneDeep(group) as any;
        if (denormalizedGroup.children) {
            denormalizedGroup.children = denormalizedGroup.children.map(subGroupId => {
                const subGroup: Group<Raw> = cloneDeep(mockEntities.group[subGroupId as any]) as any;
                delete subGroup.children;
                return subGroup;
            });
        }
        delete denormalizedGroup[IS_NORMALIZED];
        return denormalizedGroup;
    }

    function assertUserIsDenormalized(user: User<Raw>): void {
        expect(typeof user).toEqual('object');
        if (user.groups) {
            user.groups.forEach(group => expect(typeof group).toEqual('object'));
        }
    }

    function assertUsersAreDenormalized(entity: { creator: User<Raw>, editor: User<Raw> }): void {
        assertUserIsDenormalized(entity.creator);
        assertUserIsDenormalized(entity.editor);
    }

    function assertFolderIsDenormalized(folder: Folder<Raw>): void {
        expect(typeof folder).toEqual('object');
        assertUsersAreDenormalized(folder);
    }

    function assertFileIsDenormalized(file: FileOrImage<Raw>): void {
        expect(typeof file).toEqual('object');
        assertUsersAreDenormalized(file);
        assertFolderIsDenormalized(file.folder);
    }

    let normalizer: GcmsNormalizer;

    beforeEach(() => {
        normalizer = new GcmsNormalizer();
    });

    describe('normalize()', () => {

        const PAGE_ID = 3;
        const PAGE_ID2 = 37;

        function assertAllEntitiesNormalized(entities: Partial<NormalizedEntityStore>): void {
            const branches = Object.keys(entities);
            branches.forEach(entityType => {
                const branch = entities[entityType];
                const ids = Object.keys(branch);
                ids.forEach(id => expect(branch[id][IS_NORMALIZED]).toBeTruthy());
            });
        }

        it('works for a single entity', () => {
            const normalizedPage: Page<Normalized> = mockEntities.page[PAGE_ID] as any;
            const expectedPages = cloneDeep(mockEntities.page);
            delete expectedPages[PAGE_ID2].template;
            // since page with id PAGE_ID has languageVariants that contain the page with id PAGE_ID2, the languageVariants have to be copied
            expectedPages[PAGE_ID2].languageVariants = normalizedPage.languageVariants;
            const expectedTemplates: IndexById<Template<Normalized>> = { };
            expectedTemplates[normalizedPage.templateId] = mockEntities.template[normalizedPage.templateId];
            const expectedFolders: IndexById<Folder<Normalized>> = { };
            expectedFolders[normalizedPage.folder] = mockEntities.folder[normalizedPage.folder];
            const rawPage = denormalizePage(normalizedPage);

            const result = normalizer.normalize('page', rawPage);
            expect(result.resultId).toEqual(PAGE_ID);
            expect(result.result).toEqual(normalizedPage);
            expect(result.result[IS_NORMALIZED]).toBeTruthy();
            expect(result.entities.page).toEqual(expectedPages);
            expect(result.entities.user).toEqual(mockEntities.user);
            expect(result.entities.group).toEqual(mockEntities.group);
            expect(result.entities.template).toEqual(expectedTemplates);
            expect(result.entities.folder).toEqual(expectedFolders);
            assertAllEntitiesNormalized(result.entities);
        });

        it('works for an array of entities', () => {
            const normalizedPages: Page<Normalized>[] = [ mockEntities.page[PAGE_ID] as any, mockEntities.page[PAGE_ID2] as any ];
            const expectedPages = cloneDeep(mockEntities.page);
            // since page with id PAGE_ID has languageVariants that contain the page with id PAGE_ID2, the languageVariants have to be copied
            expectedPages[PAGE_ID2].languageVariants = mockEntities.page[PAGE_ID].languageVariants;
            const expectedTemplates: IndexById<Template<Normalized>> = { };
            expectedTemplates[normalizedPages[0].templateId] = mockEntities.template[normalizedPages[0].templateId];
            const expectedFolders: IndexById<Folder<Normalized>> = { };
            expectedFolders[normalizedPages[0].folder] = mockEntities.folder[normalizedPages[0].folder];
            const rawPages = [ denormalizePage(normalizedPages[0]), denormalizePage(normalizedPages[1]) ];
            const result = normalizer.normalize('page', rawPages);
            expect(result.entities.page).toEqual(expectedPages);
            expect(result.entities.user).toEqual(mockEntities.user);
            expect(result.entities.group).toEqual(mockEntities.group);
            expect(result.entities.template).toEqual(expectedTemplates);
            expect(result.entities.folder).toEqual(expectedFolders);
            assertAllEntitiesNormalized(result.entities);
        });

        it('throws an error for an invalid entity type', () => {
            expect(() => normalizer.normalize('invalid' as any, {})).toThrow();
        });

    });

    describe('denormalize()', () => {

        const PAGE_ID = 3;
        let origEntities: any;

        beforeEach(() => {
            origEntities = cloneDeep(mockEntities);
        });

        afterEach(() => {
            // Make sure that the entities in the entity store have not been modified by the denormalization.
            expect(mockEntities).toEqual(origEntities);
        });

        it('works for a page', () => {
            const page: Page<Normalized> = mockEntities.page[PAGE_ID] as any;
            const expectedResult = denormalizePage(page);

            // Check that our expected page looks the way it is supposed to.
            assertUsersAreDenormalized(expectedResult);
            assertUserIsDenormalized(expectedResult.publisher);
            expect(typeof expectedResult.template).toEqual('object');
            assertUsersAreDenormalized(expectedResult.template);
            assertFolderIsDenormalized(expectedResult.folder);
            expect(typeof expectedResult.languageVariants[1]).toEqual('object');
            assertUsersAreDenormalized(expectedResult.languageVariants[1]);
            expect(expectedResult.languageVariants[1].languageVariants).toBeFalsy();
            expect(expectedResult.languageVariants[1].pageVariants).toBeFalsy();
            expect(expectedResult.languageVariants[1].template).toBeFalsy();
            expect(typeof expectedResult.languageVariants[2]).toEqual('object');
            expect(typeof expectedResult.pageVariants[0]).toEqual('object');
            assertUsersAreDenormalized(expectedResult.pageVariants[0]);
            expect(expectedResult[IS_NORMALIZED]).toBeUndefined();

            const actualResult = normalizer.denormalize('page', page, mockEntities);
            expect(actualResult).toEqual(expectedResult);
        });

        it('works for a page without variants and folder', () => {
            const page: Page<Normalized> = cloneDeep(mockEntities.page[PAGE_ID]) as any;
            page[IS_NORMALIZED] = true;
            const expectedResult = denormalizePage(page);
            delete page.languageVariants;
            delete page.pageVariants;
            delete page.folder;
            delete expectedResult.languageVariants;
            delete expectedResult.pageVariants;
            delete expectedResult.folder;

            const actualResult = normalizer.denormalize('page', page, mockEntities);
            expect(actualResult).toEqual(expectedResult);
        });

        it('works for a normalized page', () => {
            const page: Page<Normalized> = mockEntities.page[PAGE_ID] as any;
            const expectedResult = denormalizePage(page);
            const denormalizedPage = cloneDeep(expectedResult);

            const actualResult = normalizer.denormalize('page', denormalizedPage, mockEntities);
            expect(actualResult).toEqual(expectedResult);
        });

        it('works for a file', () => {
            const file: File<Normalized> = mockEntities.file[10] as any;
            const expectedResult = denormalizeFile(file) as File<Raw>;
            assertFileIsDenormalized(expectedResult);

            const actualResult = normalizer.denormalize('file', file, mockEntities);
            expect(actualResult).toEqual(expectedResult);
        });

        it('works for a folder', () => {
            const folder: Folder<Normalized> = mockEntities.folder[1] as any;
            const expectedResult = denormalizeFolder(folder);
            assertFolderIsDenormalized(expectedResult);

            const actualResult = normalizer.denormalize('folder', folder, mockEntities);
            expect(actualResult).toEqual(expectedResult);
        });

        it('works for a group', () => {
            const group: Group<Normalized> = mockEntities.group[1] as any;
            const expectedResult = denormalizeGroup(group);
            expect(typeof expectedResult.children[0]).toEqual('object');

            const actualResult = normalizer.denormalize('group', group, mockEntities);
            expect(actualResult).toEqual(expectedResult);
        });

        it('works for an image', () => {
            const image: Image<Normalized> = mockEntities.image[10] as any;
            const expectedResult = denormalizeFile(image) as Image<Raw>;
            assertFileIsDenormalized(expectedResult);

            const actualResult = normalizer.denormalize('image', image, mockEntities);
            expect(actualResult).toEqual(expectedResult);
        });

        it('works for a language', () => {
            const language: Language = mockEntities.language[1] as any;
            const expectedResult = cloneDeep(language);
            delete expectedResult[IS_NORMALIZED];

            const actualResult = normalizer.denormalize('language', language, mockEntities);
            expect(actualResult).toEqual(expectedResult);
        });

        it('works for a message', () => {
            const message: Message<Normalized> = mockEntities.message[1000] as any;
            const expectedResult: Message<Raw> = cloneDeep(message) as any;
            replaceUser(expectedResult, 'sender');
            delete expectedResult[IS_NORMALIZED];

            const actualResult = normalizer.denormalize('message', message, mockEntities);
            expect(actualResult).toEqual(expectedResult);
        });

        it('works for a node', () => {
            const node: Node<Normalized> = mockEntities.node[1] as any;
            const expectedResult: Node<Raw> = cloneDeep(node) as any;
            replaceUser(expectedResult, 'creator');
            replaceUser(expectedResult, 'editor');
            delete expectedResult[IS_NORMALIZED];

            const actualResult = normalizer.denormalize('node', node, mockEntities);
            expect(actualResult).toEqual(expectedResult);
        });

        it('works for a template', () => {
            const template: Template<Normalized> = mockEntities.template[1];
            const templateBO: TemplateBO<Normalized> = Object.assign({}, template, { id: `${template.id}` });
            const expectedResultBO: TemplateBO<Raw> = cloneDeep(templateBO) as any;

            replaceUser(expectedResultBO, 'creator');
            replaceUser(expectedResultBO, 'editor');
            delete expectedResultBO[IS_NORMALIZED];

            const actualResult = normalizer.denormalize('template', templateBO, mockEntities);
            expect(actualResult).toEqual(expectedResultBO);
        });

        it('works for a user', () => {
            const user: User<Normalized> = mockEntities.user[3] as any;
            const expectedResult = denormalizeUser(user);
            assertUserIsDenormalized(expectedResult);
            expect(expectedResult.groups).toBeTruthy();
            expect(expectedResult.groups.length).toBeGreaterThan(0);

            const actualResult = normalizer.denormalize('user', user, mockEntities);
            expect(actualResult).toEqual(expectedResult);
        });

        it('works for null', () => {
            let result: any;
            expect(() => result = normalizer.denormalize('page', null, mockEntities)).not.toThrow();
            expect(result).toBeNull();
        });

        it('works for undefined', () => {
            let result: any;
            expect(() => result = normalizer.denormalize('page', undefined, mockEntities)).not.toThrow();
            expect(result).toBeNull();
        });

    });

});
