import 'core-js/features/symbol';
import { GcmsNormalizer } from '@gentics/cms-models';
import { NormalizationWorkerRequest, NormalizationWorkerResponse } from './worker-task';

const normalizer = new GcmsNormalizer();

addEventListener('message', ({ data }: { data: NormalizationWorkerRequest<any, any> }) => {
    const response: NormalizationWorkerResponse = {
        id: data.id,
        result: null,
    };
    try {
        response.result = normalizer.normalize(data.entityType, data.rawEntities);
    } catch (error) {
        response.error = error;
    }
    postMessage(response);
});
