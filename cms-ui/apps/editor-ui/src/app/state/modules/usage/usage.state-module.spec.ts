import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { File, Folder, Image, Page, Template, Usage } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { getExamplePageDataNormalized } from '@gentics/cms-models/testing/test-data.mock';
import { UsageState } from '../../../common/models';
import { ApplicationStateService } from '../../providers';
import { TestApplicationState } from '../../test-application-state.mock';
import { UpdateEntitiesAction } from '../entity/entity.actions';
import { STATE_MODULES } from '../state-modules';
import { ItemUsageFetchingErrorAction, ItemUsageFetchingSuccessAction, StartItemUsageFetchingAction } from './usage.actions';

describe('UsageStateModule', () => {

    let appState: TestApplicationState;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
        });
        appState = TestBed.get(ApplicationStateService);
    });

    it('sets the correct initial state', () => {
        expect(appState.now.usage).toEqual({
            files: [],
            folders: [],
            images: [],
            linkedPages: [],
            linkedFiles: [],
            linkedImages: [],
            pages: [],
            tags: [],
            templates: [],
            variants: [],
            fetching: false,
            itemType: undefined,
            itemId: undefined,
        } as UsageState);
    });

    it('fetchUsageStart works', fakeAsync(() => {
        appState.mockState({
            usage: {
                ...appState.now.usage,
                // leftovers from last usage modal
                images: [7777],
                pages: [1234, 9876],
            },
        });
        expect(appState.now.usage.fetching).toBe(false);
        appState.dispatch(new StartItemUsageFetchingAction('page', 1234));
        tick();

        expect(appState.now.usage.fetching).toBe(true);
        expect(appState.now.usage.itemId).toBe(1234);
        expect(appState.now.usage.itemType).toBe('page');
        expect(appState.now.usage.images).toEqual([]);
        expect(appState.now.usage.pages).toEqual([]);
    }));

    it('fetchUsageSuccess works', fakeAsync(() => {
        appState.mockState({
            entities: {
                file: {},
                folder: {},
                image: {},
                page: {},
                template: {},
            },
            usage: {
                fetching: true,
                itemId: 1234,
                itemType: 'page',
            },
        });

        appState.dispatch(new ItemUsageFetchingSuccessAction({
            files: [
                { id: 123, name: 'File 123' } as any as File,
            ],
            folders: [
                { id: 23, name: 'Folder 23' } as any as Folder,
            ],
            images: [
                { id: 44, name: 'Image 44' } as any as Image,
            ],
            pages: [
                { id: 1, name: 'Page 1' } as any as Page,
                { id: 2, name: 'Page 2' } as any as Page,
                { id: 3, name: 'Page 3' } as any as Page,
            ],
            tags: [
                { id: 56, name: 'Page 56 with tag' } as any as Page,
            ],
            templates: [
                { id: 33, name: 'Template 33' } as any as Template,
            ],
            variants: [
                { id: 42, name: 'Page variant 42' } as any as Page,
            ],
        }));
        tick();

        expect(appState.now.usage).toEqual({
            fetching: false,
            files: [123],
            folders: [23],
            images: [44],
            itemId: 1234,
            itemType: 'page',
            pages: [1, 2, 3],
            tags: [56],
            templates: [33],
            variants: [42],
            linkedPages: [],
            linkedFiles: [],
            linkedImages: [],
        });

        const expected = {
            file: {
                123: jasmine.objectContaining({ id: 123, name: 'File 123' }),
            },
            folder: {
                23: jasmine.objectContaining({ id: 23, name: 'Folder 23' }),
            },
            image: {
                44: jasmine.objectContaining({ id: 44, name: 'Image 44' }),
            },
            page: {
                1: jasmine.objectContaining({ id: 1, name: 'Page 1' }),
                2: jasmine.objectContaining({ id: 2, name: 'Page 2' }),
                3: jasmine.objectContaining({ id: 3, name: 'Page 3' }),
                42: jasmine.objectContaining({ id: 42, name: 'Page variant 42' }),
                56: jasmine.objectContaining({ id: 56, name: 'Page 56 with tag' }),
            },
            template: {
                33: jasmine.objectContaining({ id: 33, name: 'Template 33' }),
            },
        };

        expect(appState.now.entities).toEqual(jasmine.objectContaining(expected));

    }));

    it('fetchUsageError works', fakeAsync(() => {
        appState.mockState({
            usage: {
                fetching: true,
            },
        });
        appState.dispatch(new ItemUsageFetchingErrorAction());
        tick();

        expect(appState.now.usage.fetching).toBe(false);
    }));

    it('setItemUsage works', fakeAsync(() => {
        appState.mockState({
            entities: {
                page: {
                    1234: getExamplePageDataNormalized({ id: 1234 }),
                },
            },
        });
        const usageData: Usage = {
            files: 0,
            folders: 0,
            images: 0,
            pages: 2,
            templates: 1,
            total: 3,
        };

        appState.dispatch(new UpdateEntitiesAction({
            page: {
                1234: {
                    usage: usageData,
                },
            },
        }));
        tick();

        expect(appState.now.entities.page[1234].usage).toEqual(usageData);
    }));

});
