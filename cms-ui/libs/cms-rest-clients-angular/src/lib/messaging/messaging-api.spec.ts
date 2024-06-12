import {MockApiBase} from '../base/api-base.mock';
import {MessagingApi} from './messaging-api';

describe('MessagingApi', () => {

    let messagingApi: MessagingApi;
    let apiBase: MockApiBase;
    beforeEach(() => {
        apiBase = new MockApiBase();
        messagingApi = new MessagingApi(apiBase as any);
    });


    it('getMessages sends the correct GET request', () => {
        messagingApi.getMessages();
        expect(apiBase.get).toHaveBeenCalledWith('msg/list', { unread: false });
        expect(apiBase.get).not.toHaveBeenCalledWith('msg/list', { unread: true });

        messagingApi.getMessages(true);
        expect(apiBase.get).toHaveBeenCalledWith('msg/list', { unread: true });
    });

    it('markAsRead sends the correct POST request', () => {
        messagingApi.markAsRead([111, 222, 333]);
        expect(apiBase.post).toHaveBeenCalledWith('msg/read', { messages: [111, 222, 333]});
    });

    it('sendMessage sends the correct POST request', () => {
        messagingApi.sendMessage({
            message: 'Test message',
            toUserId: [1234, 5678],
            toGroupId: [999],
            type: 'INFO',
        });
        expect(apiBase.post).toHaveBeenCalledWith('msg/send', {
            message: 'Test message',
            toUserId: [1234, 5678],
            toGroupId: [999],
            type: 'INFO',
        });
    });

});
