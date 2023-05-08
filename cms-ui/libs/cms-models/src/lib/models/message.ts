import { ResponseMessage } from './response';
import { DefaultModelType, ModelType, Normalizable, NormalizableEntity, Raw } from './type-util';
import { User } from './user';

/**
 * A message by a user or a job, e.g. a publish in progress.
 */
export interface Message<T extends ModelType = DefaultModelType> extends ResponseMessage, NormalizableEntity<T> {
    /** A unique message ID. */
    id: number;

    /** The text of the message, written by a user or sent by the backend. */
    message: string;

    /** The user who sent the message. */
    sender: Normalizable<T, User<Raw>, number>;

    /** Whether a message is read or not. Added by the UI, not returned by the API. */
    unread: boolean;
}

export interface MessageFromServer {
    id: number;
    message: string;
    sender: User<Raw>;
    timestamp: number;
    type: 'INFO';
}
