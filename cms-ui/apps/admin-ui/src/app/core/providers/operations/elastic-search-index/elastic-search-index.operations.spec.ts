import { ObservableStopper } from '@admin-ui/common';
import { InterfaceOf } from '@admin-ui/common/utils/util-types/util-types';
import { MOCK_ENTITIES_NORMALIZED, MOCK_ENTITIES_RAW } from '@admin-ui/common/testing/elastic-search-index.model';
import { AppStateService } from '@admin-ui/state';
import { OPTIONS_CONFIG } from '@admin-ui/state/state-store.config';
import { STATE_MODULES } from '@admin-ui/state/state.module';
import { TestBed } from '@angular/core/testing';
import {
    ElasticSearchIndex,
    ElasticSearchIndexListResponse,
    Normalized,
    Raw,
    RecursivePartial,
    ResponseCode,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { NgxsModule } from '@ngxs/store';
import { LoggerTestingModule } from 'ngx-logger/testing';
import { of as observableOf } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ElasticSearchIndexOperations, EntityManagerService, ErrorHandler, I18nNotificationService } from '../..';
import { MockErrorHandler } from '../../error-handler/error-handler.mock';

class MockApi implements RecursivePartial<InterfaceOf<GcmsApi>> {
    elasticSearchIndex = {
        getItems: jasmine.createSpy('getItems').and.stub(),
        rebuild: jasmine.createSpy('rebuild').and.returnValue(observableOf({})),
    };
}

class MockNotificationService {
    show = jasmine.createSpy('notification.show');
}

describe('ElasticSearchIndexOperations', () => {

    let api: MockApi;
    let entityOperations: ElasticSearchIndexOperations;
    let notification: MockNotificationService;
    let appState: AppStateService;
    let stopper: ObservableStopper;

    beforeEach(() => {

        notification = new MockNotificationService();
        stopper = new ObservableStopper();

        TestBed.configureTestingModule({
            imports: [
                NgxsModule.forRoot(STATE_MODULES, OPTIONS_CONFIG),
                LoggerTestingModule,
            ],
            providers: [
                AppStateService,
                EntityManagerService,
                ElasticSearchIndexOperations,
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: GcmsApi, useClass: MockApi },
                { provide: I18nNotificationService, useValue: notification },
            ],
        });

        api = TestBed.get(GcmsApi);
        entityOperations = TestBed.get(ElasticSearchIndexOperations);
        appState = TestBed.get(AppStateService);

    });

    afterEach(() => {
        stopper.stop();
    });

    it('getItems() works', () => {
        // prepare test data
        const mockResponse: ElasticSearchIndexListResponse = {
            responseInfo: { responseCode: ResponseCode.OK },
            hasMoreItems: false,
            numItems: MOCK_ENTITIES_RAW.length,
            items: MOCK_ENTITIES_RAW,
            messages: [],
        };
        api.elasticSearchIndex.getItems.and.returnValue(observableOf(mockResponse));

        // check if state does not yet has any user stored
        const storedItems$ = appState.select(state => state.entity.elasticSearchIndex);
        let storedEntities: ElasticSearchIndex<Normalized>[];
        storedItems$.pipe(
            takeUntil(stopper.stopper$),
        ).subscribe(entities => storedEntities = Object.keys(entities).map(key => entities[key]));
        expect(storedEntities).toEqual([]);

        const response$ = entityOperations.getAll();
        expect(api.elasticSearchIndex.getItems).toHaveBeenCalled();

        // check if correct response
        let loadedEntities: ElasticSearchIndex<Raw>[];
        response$.pipe(
            takeUntil(stopper.stopper$),
        ).subscribe(entities => loadedEntities = entities);
        expect(loadedEntities).toEqual(MOCK_ENTITIES_RAW);

        // check if mock entities have been put into store
        storedItems$.pipe(
            takeUntil(stopper.stopper$),
        ).subscribe(entities => storedEntities = Object.keys(entities).map(key => entities[key]));
        expect(storedEntities).toEqual(MOCK_ENTITIES_NORMALIZED);
    });

    it('rebuild() works', () => {
        // prepare test data
        const mockResponse: ElasticSearchIndexListResponse = {
            responseInfo: { responseCode: ResponseCode.OK },
            hasMoreItems: false,
            numItems: MOCK_ENTITIES_RAW.length,
            items: MOCK_ENTITIES_RAW,
            messages: [],
        };
        const indexName = 'index_one';
        const drop = true;
        api.elasticSearchIndex.getItems.and.returnValue(observableOf(mockResponse));

        // check if state does not yet has any user stored
        const storedItems$ = appState.select(state => state.entity.elasticSearchIndex);
        let storedEntities: ElasticSearchIndex<Normalized>[];
        storedItems$.pipe(
            takeUntil(stopper.stopper$),
        ).subscribe(entities => storedEntities = Object.keys(entities).map(key => entities[key]));
        expect(storedEntities).toEqual([]);

        const response$ = entityOperations.rebuild(indexName, drop);
        expect(api.elasticSearchIndex.rebuild).toHaveBeenCalledWith(indexName, drop);

        // check if correct response
        let loadedEntities: ElasticSearchIndex<Raw>[];
        response$.pipe(
            takeUntil(stopper.stopper$),
        ).subscribe(entities => loadedEntities = entities);
        expect(loadedEntities).toEqual(MOCK_ENTITIES_RAW);

        // check if mock entities have been put into store
        storedItems$.pipe(
            takeUntil(stopper.stopper$),
        ).subscribe(entities => storedEntities = Object.keys(entities).map(key => entities[key]));
        expect(storedEntities).toEqual(MOCK_ENTITIES_NORMALIZED);
    });

});
