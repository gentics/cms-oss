import { GcmsNormalizer, GcmsTestData, IS_NORMALIZED } from '@gentics/cms-models';
import { cloneDeep } from 'lodash-es';
import { NormalizationWorkerRequest, NormalizationWorkerResponse } from './worker-task';

describe('NormalizationWorker', () => {

    let normalizationWorker: Worker;
    let onMessageSpy: jasmine.Spy;
    let normalizer: GcmsNormalizer;

    beforeEach(() => {
        onMessageSpy = jasmine.createSpy('onMessage');
        normalizationWorker = new Worker(new URL('./normalization.worker', import.meta.url), { type: 'module' });
        normalizationWorker.onmessage = onMessageSpy;
        normalizer = new GcmsNormalizer();
    });

    afterEach(() => {
        normalizationWorker.terminate();
    });

    it('normalizes the entities passed to it', done => {
        const rawEntities = [
            GcmsTestData.getExamplePageData({ id: 1 }),
            GcmsTestData.getExamplePageData({ id: 2 }),
            GcmsTestData.getExamplePageData({ id: 3 }),
        ];
        const request: NormalizationWorkerRequest<any, any> = {
            id: 4711,
            entityType: 'page',
            rawEntities,
        };

        const expectedResult = normalizer.normalize('page', cloneDeep(rawEntities));

        Object.values(expectedResult.entities.page).forEach(page => {
            delete page[IS_NORMALIZED];
        });
        Object.values(expectedResult.entities.user).forEach(user => {
            delete user[IS_NORMALIZED];
        });

        const expectedResponse: NormalizationWorkerResponse = {
            id: 4711,
            result: expectedResult,
        };

        normalizationWorker.postMessage(request);

        onMessageSpy.and.callFake((msg: MessageEvent) => {
            expect(msg.data).toEqual(expectedResponse);
            done();
        });
    });

});
