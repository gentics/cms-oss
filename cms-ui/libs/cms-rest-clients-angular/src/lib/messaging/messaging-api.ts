import { MessageListResponse, Response, SendMessageRequest } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';

/**
 * API methods related to the messaging resource.
 *
 * Docs for the endpoints used here can be found at:
 * http://www.gentics.com/Content.Node/cmp8/guides/restapi/resource_MessagingResource.html
 */
export class MessagingApi {

    constructor(private apiBase: ApiBase) {}

    /**
     * Get the list of messages for the current user.
     */
    getMessages(unreadOnly: boolean = false): Observable<MessageListResponse> {
        return this.apiBase.get('msg/list', { unread: unreadOnly });
    }

    /**
     * Mark messages as "read".
     */
    markAsRead(messageIDs: number[]): Observable<Response> {
        return this.apiBase.post('msg/read', { messages: messageIDs });
    }

    /**
     * Delete a message.
     */
    deleteMessage(messageId: number): Observable<Response> {
        return this.apiBase.delete(`msg/${messageId}`);
    }

    /**
     * Send a message to users and/or groups.
     */
    sendMessage(message: SendMessageRequest): Observable<Response> {
        return this.apiBase.post('msg/send', message);
    }
}
