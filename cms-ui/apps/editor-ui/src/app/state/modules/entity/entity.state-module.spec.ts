import { Page } from '@gentics/cms-models';
import { EntityState } from '../../../common/models';
import { addNormalizedEntities, updateEntities } from './entity.state-module';

describe('addNormalizedEntities', () => {

    it('adds non-existing entities', () => {
        const initial = {
            file: {},
            page: {},
        };

        const normalized = {
            result: [1, 2],
            entities: {
                page: {
                    1: {
                        id: 1,
                        name: 'Page 1',
                        type: 'page',
                    },
                    2: {
                        id: 2,
                        name: 'Page 2',
                        type: 'page',
                    },
                },
            },
        };

        const result = addNormalizedEntities(initial as EntityState, normalized);

        expect(result).not.toBe(initial as EntityState);
        expect(result.page[1]).toEqual({
            id: 1,
            name: 'Page 1',
            type: 'page',
        } as Page);
        expect(result.page[2]).toEqual({
            id: 2,
            name: 'Page 2',
            type: 'page',
        } as Page);
    });

    it('keeps unchanged entity branches', () => {
        const initial = {
            file: {},
            page: {},
        };

        const normalized = {
            result: [1],
            entities: {
                page: {
                    1: {
                        id: 1,
                        name: 'Page 1',
                        type: 'page',
                    },
                },
            },
        };

        const result = addNormalizedEntities(initial as EntityState, normalized);

        expect(result).not.toBe(initial as EntityState);
        expect(result.page).not.toBe(initial.page);
        expect(result.file).toBe(initial.file, '"files" changed but should not');
    });

    it('overwrites existing entities (and changes reference)', () => {
        const initial = {
            file: {},
            page: {
                1: {
                    id: 1,
                    name: 'Page 1 before change',
                    type: 'page',
                },
            },
        };

        const normalized = {
            result: [1],
            entities: {
                page: {
                    1: {
                        id: 1,
                        name: 'Page 1 after change',
                        type: 'page',
                    },
                },
            },
        };

        const result = addNormalizedEntities(initial as any, normalized);

        expect(initial.page[1].name).toBe('Page 1 before change');
        expect(result.page[1].name).toBe('Page 1 after change');
        expect(result.page[1]).not.toBe(initial.page[1] as Page, 'pages[1] is still the same reference');
        expect(result.page).not.toBe(initial.page as any, 'pages is still the same reference');
    });

    it('keeps existing entities with the same values (and keeps reference)', () => {
        const initial = {
            file: {},
            page: {
                1: {
                    id: 1,
                    name: 'Page 1, no changes',
                    type: 'page',
                },
            },
        };

        const normalized = {
            result: [1],
            entities: {
                page: {
                    1: {
                        id: 1,
                        name: 'Page 1, no changes',
                        type: 'page',
                    },
                },
            },
        };

        const result = addNormalizedEntities(initial as any, normalized);

        expect(result.page[1]).toBe(initial.page[1] as Page, 'pages[1] changed reference');
        expect(result.page).toBe(initial.page as any, 'pages changed reference');
        expect(result).toBe(initial as any, 'result is a different reference');
    });

    it('adds new properties to entities without changes in properties (and changes reference)', () => {
        const initial = {
            file: {},
            page: {
                1: {
                    id: 1,
                    name: 'Page 1, no changes',
                    type: 'page',
                    description: 'description not in new entity',
                },
            },
        };

        const normalized = {
            result: [1],
            entities: {
                page: {
                    1: {
                        id: 1,
                        name: 'Page 1, no changes',
                        type: 'page',
                        description: 'description not in new entity',
                        folderId: 25,
                    },
                },
            },
        };

        const result = addNormalizedEntities(initial as any, normalized);

        expect(result.page[1]).not.toBe(initial.page[1] as Page, 'pages[1] is still the same reference');
        expect(result.page[1].folderId).toBe(25);
        expect(result.page).not.toBe(initial.page as any, 'pages is still the same reference');
        expect(result).not.toBe(initial as any, 'result is still the same reference');
    });

    it ('adds new properties to entities with changes in properties (and changes reference)', () => {
        const initial = {
            file: {},
            page: {
                1: {
                    id: 1,
                    name: 'Page 1 before change',
                    type: 'page',
                    description: 'description not in new entity',
                },
            },
        };

        const normalized = {
            result: [1],
            entities: {
                page: {
                    1: {
                        id: 1,
                        name: 'Page 1 after change',
                        type: 'page',
                        description: 'description not in new entity',
                        folderId: 25,
                    },
                },
            },
        };

        const result = addNormalizedEntities(initial as any, normalized);

        expect(result.page[1]).not.toBe(initial.page[1] as Page, 'pages[1] is still the same reference');
        expect(result.page[1].folderId).toBe(25);
        expect(result.page).not.toBe(initial.page as any, 'pages is still the same reference');
        expect(result).not.toBe(initial as any, 'result is still the same reference');
    });

    it('deletes extra properties of existing entities if nothing changes (and changes reference)', () => {
        const initial = {
            file: {},
            page: {
                1: {
                    id: 1,
                    name: 'Page 1, no changes',
                    type: 'page',
                    description: 'description not in new entity',
                },
            },
        };

        const normalized = {
            result: [1],
            entities: {
                page: {
                    1: {
                        id: 1,
                        name: 'Page 1, no changes',
                        type: 'page',
                    },
                },
            },
        };

        const result = addNormalizedEntities(initial as any, normalized);

        expect(result.page[1]).not.toBe(initial.page[1] as Page, 'pages[1] is still the same reference');
        expect(result.page[1].description).toBeFalsy();
        expect(result.page).not.toBe(initial.page as any, 'pages is still the same reference');
        expect(result).not.toBe(initial as any, 'result is still the same reference');
    });

    it('deletes extra properties of existing entities if other properties change (and changes reference)', () => {
        const initial = {
            file: {},
            page: {
                1: {
                    id: 1,
                    name: 'Page 1 before change',
                    type: 'page',
                    description: 'description not in new entity',
                },
            },
        };

        const normalized = {
            result: [1],
            entities: {
                page: {
                    1: {
                        id: 1,
                        name: 'Page 1 after change',
                        type: 'page',
                    },
                },
            },
        };

        const result = addNormalizedEntities(initial as any, normalized);

        expect(result.page[1]).not.toBe(initial.page[1] as Page, 'pages[1] is still the same reference');
        expect(result.page[1].description).toBeFalsy();
        expect(result.page).not.toBe(initial.page as any, 'pages is still the same reference');
        expect(result).not.toBe(initial as any, 'result is still the same reference');
    });

});


describe('updateEntities', () => {

    it('updates properties of existing entities', () => {
        const initial = {
            file: {},
            page: {
                1: {
                    id: 1,
                    locked: false,
                    name: 'Page 1',
                    type: 'page',
                },
            },
        };

        const updates = {
            page: {
                1: {
                    locked: true,
                },
            },
        };

        const result = updateEntities(initial as any, updates);
        expect(result.page[1].locked).toBe(true);
    });

    it('does not add non-existing entities', () => {
        const initial = {
            file: {},
            page: {},
        };

        const updates = {
            page: {
                1: {
                    locked: true,
                },
            },
        };

        const result = updateEntities(initial as EntityState, updates);

        expect(result).toBe(initial as EntityState, 'result reference changed');
        expect(result.page[1]).toBeUndefined();
    });

    it('changes references of changed entity branches', () => {
        const initial = {
            file: {},
            page: {
                1: {
                    id: 1,
                    locked: false,
                    name: 'Page 1',
                    type: 'page',
                },
            },
        };

        const updates = {
            page: {
                1: {
                    locked: true,
                },
            },
        };

        const result = updateEntities(initial as any, updates);

        expect(result).not.toBe(initial as any, 'result is the same reference');
        expect(result.page).not.toBe(initial.page as any, 'pages is the same reference');
        expect(result.page[1]).not.toBe(initial.page[1] as Page, 'pages[1] is the same reference');
    });

    it('keeps unchanged entity branches', () => {
        const initial = {
            file: {},
            page: {
                1: {
                    id: 1,
                    locked: false,
                    name: 'Page 1',
                    type: 'page',
                },
            },
        };

        const updates = {
            page: {
                1: {
                    locked: true,
                },
            },
        };

        const result = updateEntities(initial as any, updates);

        expect(result).not.toBe(initial as any);
        expect(result.page).not.toBe(initial);
        expect(result.file).toBe(initial.file, '"files" changed but should not');
    });

    it('ignores empty entity branches in the input', () => {
        const initial = {
            file: {},
            page: {
                1: {
                    id: 1,
                    name: 'Page 1 before change',
                    type: 'page',
                },
            },
        };

        const updates = {
            file: { },
            page: { },
        };

        const result = updateEntities(initial as any, updates);

        expect(result === initial as any).toBe(true);
    });

    it('can overwrite non-empty arrays with empty arrays', () => {
        const initial = {
            page: {
                1: {
                    id: 1,
                    type: 'page',
                    versions: [
                        {
                            number: '1.5',
                            timestamp: 123456789,
                        },
                    ],
                },
            },
        };

        const updates = {
            page: {
                1: {
                    versions: [] as any[],
                },
            },
        };

        expect(initial.page[1].versions.length).toBe(1);

        const result = updateEntities(initial as any, updates);
        expect(result.page[1].versions.length).toBe(0);
    });

});
