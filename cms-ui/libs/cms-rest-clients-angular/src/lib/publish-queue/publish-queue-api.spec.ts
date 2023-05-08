import {PublishQueueOptions} from '@gentics/cms-models';

import {MockApiBase} from '../base/api-base.mock';
import {PublishQueueApi} from './publish-queue-api';

describe('PublishQueueApi', () => {

    let publishQueueApi: PublishQueueApi;
    let apiBase: MockApiBase;
    beforeEach(() => {
        apiBase = new MockApiBase();
        publishQueueApi = new PublishQueueApi(apiBase as any);
    });


    it('assignToUsers sends a POST request to "page/assign"', () => {
        const pageIds = [11, 22, 33, 44];
        const userIds = [777, 888, 999];
        const message = 'some message';
        publishQueueApi.assignToUsers(pageIds, userIds, message);

        expect(apiBase.post).toHaveBeenCalledWith('page/assign', {
            message,
            pageIds,
            userIds
        });
    });

    it('getPublishQueue() sends a GET request to "page/pubqueue"', () => {
        const options: PublishQueueOptions = {
            maxItems: 10
        };
        publishQueueApi.getPublishQueue(options);
        expect(apiBase.get).toHaveBeenCalledWith('page/pubqueue', options);
    });

    it('approvePageStatus() sends a POST request to "page/pubqueue/approve"', () => {
        const ids: number[] = [23, 14];

        publishQueueApi.approvePageStatus(ids);
        expect(apiBase.post).toHaveBeenCalledWith('page/pubqueue/approve', { ids });
    });

});
